package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentBookingTimeBinding

class BookingTimeFragment : Fragment() {

    private var _binding: FragmentBookingTimeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Group the time slots into a list
        val timeButtons = listOf(
            binding.btnTime10, binding.btnTime11, binding.btnTime12,
            binding.btnTime1, binding.btnTime2, binding.btnTime3
        )

        // Make them mutually exclusive (clicking one un-checks the rest)
        timeButtons.forEach { button ->
            button.setOnClickListener {
                timeButtons.forEach { it.isChecked = false } // Turn off all buttons
                button.isChecked = true // Turn on the tapped button
            }
        }

        binding.btnContinueTime.setOnClickListener {
            // Passes the flow to the Guest Details screen
            findNavController().navigate(R.id.action_bookingTime_to_bookingDetails)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}