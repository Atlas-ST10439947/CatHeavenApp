package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.catheavenapp.databinding.FragmentResidentDetailBinding
import com.example.catheavenapp.domain.model.Cat
import com.example.catheavenapp.domain.model.CatDescriptions
import com.example.catheavenapp.ui.booking.formatDateMillis
import com.example.catheavenapp.ui.cats.ResidentDetailState
import com.example.catheavenapp.ui.cats.ResidentDetailViewModel

/** One cat's profile, loaded from Firestore by the ID passed from the residents list. */
class ResidentFragment : Fragment() {

    private var _binding: FragmentResidentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ResidentDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResidentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val catId = arguments?.getString(ARG_CAT_ID)
        if (catId == null) {
            showMessage("This cat couldn't be found.")
            return
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                ResidentDetailState.Loading -> showMessage("Loading…")
                ResidentDetailState.NotFound -> showMessage("This cat couldn't be found.")
                is ResidentDetailState.Failed -> showMessage(state.message)
                is ResidentDetailState.Loaded -> showCat(state.cat)
            }
        }

        if (viewModel.state.value !is ResidentDetailState.Loaded) viewModel.load(catId)
    }

    private fun showCat(cat: Cat) {
        val context = binding.root.context

        binding.catNameText.text = cat.name
        binding.catStatusText.text = CatDescriptions.statusLabel(cat.adoptionStatus)
        binding.catPhoto.contentDescription = "Photo of ${cat.name}"

        // Load the image for the detail view
        val resourceId = context.resources.getIdentifier(cat.photoUrl, "drawable", context.packageName)
        if (resourceId != 0) {
            binding.catPhoto.setImageResource(resourceId)
        } else {
            binding.catPhoto.setImageResource(R.drawable.pawprint)
        }

        binding.catFactsText.text = listOfNotNull(
            cat.breed.ifBlank { null },
            cat.gender.ifBlank { null }?.replaceFirstChar { it.uppercase() },
            CatDescriptions.age(cat.dateOfBirthMillis, System.currentTimeMillis())
        ).joinToString("  ·  ")

        binding.catPersonalityText.text = cat.personality
            .ifBlank { "Still getting to know this one!" }
            .replaceFirstChar { it.uppercase() }
        binding.catBiographyText.text = cat.biography.ifBlank { "No biography yet." }

        val arrived = cat.dateArrivedMillis
        binding.catArrivedText.visibility = if (arrived != null) View.VISIBLE else View.GONE
        if (arrived != null) {
            binding.catArrivedText.text = "Arrived at Cat Heaven on ${formatDateMillis(arrived)}"
        }

        binding.messageText.visibility = View.GONE
        binding.catInfoCard.visibility = View.VISIBLE
    }

    private fun showMessage(text: String) {
        binding.messageText.text = text
        binding.messageText.visibility = View.VISIBLE
        binding.catInfoCard.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_CAT_ID = "catId"
    }
}