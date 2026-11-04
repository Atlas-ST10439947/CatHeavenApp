package com.example.catheavenapp.ui.admin

import com.example.catheavenapp.util.Diagnostics
import com.example.catheavenapp.util.FailureMessages
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.cat.CatRepository
import com.example.catheavenapp.data.cat.FirestoreCatRepository
import com.example.catheavenapp.data.menu.FirestoreMenuRepository
import com.example.catheavenapp.data.menu.MenuRepository
import com.example.catheavenapp.domain.content.ContentCheck
import com.example.catheavenapp.domain.content.ContentValidator
import com.example.catheavenapp.domain.content.DefaultMenu
import com.example.catheavenapp.domain.model.Cat
import com.example.catheavenapp.domain.model.MenuItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class ContentTab { CATS, MENU }

/** One row on the admin Content screen: either a cat or a menu item. */
sealed interface ContentRow {
    data class CatRow(val cat: Cat) : ContentRow
    data class MenuRow(val item: MenuItem) : ContentRow
}

sealed interface ContentListState {
    object Loading : ContentListState
    data class Loaded(val tab: ContentTab, val rows: List<ContentRow>) : ContentListState
    data class Failed(val message: String) : ContentListState
}

sealed interface ContentSaveState {
    object Idle : ContentSaveState
    object Saving : ContentSaveState
    data class Done(val message: String) : ContentSaveState
    data class Failed(val message: String) : ContentSaveState
}

/** Lets admins add, edit and remove resident cats and café/gift shop items. */
class AdminContentViewModel(
    private val cats: CatRepository = FirestoreCatRepository(),
    private val menu: MenuRepository = FirestoreMenuRepository(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _tab = MutableLiveData(ContentTab.CATS)
    val tab: LiveData<ContentTab> = _tab

    private val _list = MutableLiveData<ContentListState>(ContentListState.Loading)
    val list: LiveData<ContentListState> = _list

    private val _saveState = MutableLiveData<ContentSaveState>(ContentSaveState.Idle)
    val saveState: LiveData<ContentSaveState> = _saveState

    private var loadJob: Job? = null

    fun selectTab(tab: ContentTab) {
        if (_tab.value == tab) return
        _tab.value = tab
        load()
    }

    fun load() {
        val tab = _tab.value ?: ContentTab.CATS
        loadJob?.cancel()
        _list.value = ContentListState.Loading
        loadJob = viewModelScope.launch {
            _list.value = try {
                val rows = when (tab) {
                    ContentTab.CATS -> cats.allCats().map { ContentRow.CatRow(it) }
                    // Grouped by section (Beverages & Food, then Gift Shop), then by name
                    ContentTab.MENU -> menu.allItems()
                        .sortedWith(compareBy({ it.category.ordinal }, { it.name.lowercase() }))
                        .map { ContentRow.MenuRow(it) }
                }
                ContentListState.Loaded(tab, rows)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Loading content failed", e)
                ContentListState.Failed("We couldn't load this list. Check your connection and try again.")
            }
        }
    }

    /** Validates first; only saves when the cat is valid. The screen shows any field error. */
    fun saveCat(cat: Cat): ContentCheck {
        val check = ContentValidator.checkCat(cat, clock())
        if (check == ContentCheck.Valid) {
            write(if (cat.id.isBlank()) "${cat.name} added" else "${cat.name} updated") { cats.save(cat) }
        }
        return check
    }

    fun saveMenuItem(item: MenuItem): ContentCheck {
        val check = ContentValidator.checkMenuItem(item)
        if (check == ContentCheck.Valid) {
            write(if (item.id.isBlank()) "${item.name} added" else "${item.name} updated") { menu.save(item) }
        }
        return check
    }

    fun deleteCat(cat: Cat) = write("${cat.name} removed") { cats.delete(cat.id) }

    /** Adds the standard gift shop range. Fixed IDs mean tapping twice can't create duplicates. */
    fun addDefaultGiftShop() = write("Added the standard gift shop items") {
        DefaultMenu.giftShopItems().forEach { menu.save(it) }
    }

    fun deleteMenuItem(item: MenuItem) = write("${item.name} removed") { menu.delete(item.id) }

    fun onSaveMessageShown() {
        if (_saveState.value is ContentSaveState.Done || _saveState.value is ContentSaveState.Failed) {
            _saveState.value = ContentSaveState.Idle
        }
    }

    private fun write(successMessage: String, action: suspend () -> Unit) {
        if (_saveState.value == ContentSaveState.Saving) return
        _saveState.value = ContentSaveState.Saving
        viewModelScope.launch {
            _saveState.value = try {
                action()
                ContentSaveState.Done(successMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // PERMISSION_DENIED means the account isn't an admin, or the rules rejected the data
                Diagnostics.warn(TAG, "Saving content failed", e)
                ContentSaveState.Failed(FailureMessages.forAdmin(e, "save that change"))
            }
            load()   // show what is actually stored now
        }
    }

    private companion object {
        const val TAG = "AdminContent"
    }
}
