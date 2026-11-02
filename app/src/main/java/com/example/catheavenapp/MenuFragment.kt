package com.example.catheavenapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.catheavenapp.data.menu.FirestoreMenuRepository
import com.example.catheavenapp.data.menu.MenuRepository
import com.example.catheavenapp.databinding.FragmentMenuBinding
import com.example.catheavenapp.domain.content.MenuSections
import com.example.catheavenapp.domain.model.MenuCategory
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * The café menu and gift shop. Items come from the Firestore menuItems collection,
 * which admins manage from the admin Content screen. Until a section has items in
 * Firestore (or if loading fails offline), the section keeps its default list.
 */
class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val repository: MenuRepository = FirestoreMenuRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sections = MenuSections.from(repository.allItems())
                val views = _binding ?: return@launch   // the screen may have closed while loading
                sections[MenuCategory.FOOD_AND_DRINK]?.takeIf { it.isNotEmpty() }?.let { items ->
                    views.foodListText.text = items.joinToString("\n", transform = MenuSections::line)
                }
                sections[MenuCategory.GIFT_SHOP]?.takeIf { it.isNotEmpty() }?.let { items ->
                    views.giftListText.text = items.joinToString("\n", transform = MenuSections::line)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Offline or unavailable: the default menu text stays on screen
                Log.w("Menu", "Loading menu failed", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
