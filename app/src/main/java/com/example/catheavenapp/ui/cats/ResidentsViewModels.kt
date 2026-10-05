package com.example.catheavenapp.ui.cats

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.cat.CatRepository
import com.example.catheavenapp.data.cat.FirestoreCatRepository
import com.example.catheavenapp.domain.model.Cat
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface ResidentsState {
    object Loading : ResidentsState
    data class Loaded(val cats: List<Cat>) : ResidentsState
    data class Failed(val message: String) : ResidentsState
}

/** The "Meet The Residents" list. */
class ResidentsViewModel(
    private val repository: CatRepository = FirestoreCatRepository()
) : ViewModel() {

    private val _state = MutableLiveData<ResidentsState>(ResidentsState.Loading)
    val state: LiveData<ResidentsState> = _state

    fun load() {
        viewModelScope.launch {
            _state.value = try {
                ResidentsState.Loaded(repository.allCats())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Loading cats failed", e)
                ResidentsState.Failed("We couldn't load the residents. Check your connection.")
            }
        }
    }
}

sealed interface ResidentDetailState {
    object Loading : ResidentDetailState
    data class Loaded(val cat: Cat) : ResidentDetailState
    object NotFound : ResidentDetailState
    data class Failed(val message: String) : ResidentDetailState
}

/** One cat's profile. */
class ResidentDetailViewModel(
    private val repository: CatRepository = FirestoreCatRepository()
) : ViewModel() {

    private val _state = MutableLiveData<ResidentDetailState>(ResidentDetailState.Loading)
    val state: LiveData<ResidentDetailState> = _state

    fun load(catId: String) {
        viewModelScope.launch {
            _state.value = try {
                repository.cat(catId)?.let { ResidentDetailState.Loaded(it) } ?: ResidentDetailState.NotFound
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Loading cat failed", e)
                ResidentDetailState.Failed("We couldn't load this cat. Check your connection.")
            }
        }
    }
}

private const val TAG = "Residents"
