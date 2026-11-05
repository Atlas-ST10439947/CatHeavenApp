package com.example.catheavenapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentHomeBinding
import com.example.catheavenapp.ui.cats.ResidentsState
import com.example.catheavenapp.ui.cats.ResidentsViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Same list as Meet the Residents, so both screens show the same number of cats
    private val residents: ResidentsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Route the main CTA straight into the booking flow
        binding.bookButton.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_bookingDateFragment)
        }

        // The Rescue Cats card opens the Meet the Residents tab
        binding.rescueCatsCard.setOnClickListener {
            (activity as? MainActivity)?.openTab(R.id.nav_cats)
        }

        // Show the real number of resident cats. If it can't load, the text from the
        // layout stays, so the screen never shows an error here.
        residents.state.observe(viewLifecycleOwner) { state ->
            val count = (state as? ResidentsState.Loaded)?.cats?.size ?: return@observe
            if (count == 0) return@observe
            val cats = if (count == 1) "1 rescued cat" else "$count rescued cats"
            binding.introText.text =
                "Visit our $cats in their peaceful sanctuary. Book a slot to enjoy coffee, cuddles, and a little bit of magic."
            binding.rescueCatsCountText.text = if (count == 1) "1 Furry Friend" else "$count Furry Friends"
            binding.rescueCatsCard.contentDescription =
                "Rescue cats, ${binding.rescueCatsCountText.text.toString().lowercase()}. Opens Meet the Residents."
        }
        if (residents.state.value !is ResidentsState.Loaded) residents.load()

        // Launch external browser for GivenGain donations
        binding.donateButton.setOnClickListener {
            val url = "https://www.givengain.com/cause/rescue-cat-cafe-21339"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}