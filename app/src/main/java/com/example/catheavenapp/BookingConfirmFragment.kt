package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentBookingConfirmBinding
import com.example.catheavenapp.ui.booking.BookingFlowViewModel

class BookingConfirmFragment : Fragment() {

    private var _binding: FragmentBookingConfirmBinding? = null
    private val binding get() = _binding!!

    private val bookingViewModel: BookingFlowViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Static confirmation view for the Front-End demo.
        // Next step (feature/booking-transaction): show the saved booking's
        // reference, date and time here once it comes back from Firestore.

        binding.btnReturnHome.setOnClickListener {
            // The booking is finished, so the next one starts with a clean slate
            bookingViewModel.reset()
            findNavController().navigate(R.id.action_bookingConfirm_to_nav_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}