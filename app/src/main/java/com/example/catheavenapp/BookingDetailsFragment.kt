package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentBookingDetailsBinding

class BookingDetailsFragment : Fragment() {

    private var _binding: FragmentBookingDetailsBinding? = null
    private val binding get() = _binding!!

    private var guestCount = 1
    private val pricePerPerson = 30 // Example base price for 1 Hour

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateGuestUI()

        // Restrict to max 8 guests per Cat Heaven rules
        binding.btnAddGuest.setOnClickListener {
            if (guestCount < 8) {
                guestCount++
                updateGuestUI()
            }
        }

        binding.btnMinusGuest.setOnClickListener {
            if (guestCount > 1) {
                guestCount--
                updateGuestUI()
            }
        }

        binding.btnConfirmBooking.setOnClickListener {
            // TODO: Wiehan will trigger the atomic Firestore write here before navigating
            findNavController().navigate(R.id.action_bookingDetails_to_bookingConfirm)
        }
    }

    private fun updateGuestUI() {
        val guestString = if (guestCount == 1) "1 Guest" else "$guestCount Guests"
        binding.guestCountText.text = guestString

        val total = guestCount * pricePerPerson
        binding.summaryDetails.text = "1 hour x $guestString"
        binding.summaryCalculation.text = "R$pricePerPerson x $guestCount"
        binding.summaryTotal.text = "R$total"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}