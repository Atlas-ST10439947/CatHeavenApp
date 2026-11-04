package com.example.catheavenapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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