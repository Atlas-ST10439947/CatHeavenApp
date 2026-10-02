package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
// import androidx.navigation.fragment.findNavController
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
            // findNavController().navigate(R.id.nav_booking)
        }

        binding.donateButton.setOnClickListener {
            // Open an external browser link to GivenGain/EFT info as per Cat Heaven's current process
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}