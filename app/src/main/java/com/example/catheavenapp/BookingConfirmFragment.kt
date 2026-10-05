package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.catheavenapp.databinding.FragmentBookingConfirmBinding
import com.example.catheavenapp.ui.booking.BookingFlowViewModel
import com.example.catheavenapp.ui.booking.formatDateMillis
import com.example.catheavenapp.ui.booking.formatDuration
import com.example.catheavenapp.ui.booking.formatPeople
import com.example.catheavenapp.ui.booking.formatSlotTime

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

        val booking = bookingViewModel.confirmedBooking.value
        if (booking == null) {
            // Nothing to show (e.g. the app was restarted on this screen)
            goHome()
            return
        }
        bookingViewModel.onConfirmationShown()

        // The ticket shows the booking exactly as it was saved in Firestore
        binding.successEmailText.text = "Booking reference: ${booking.bookingReference}"
        binding.ticketName.text = booking.supporterName
        binding.ticketDate.text = formatDateMillis(booking.slotDateMillis)
        binding.ticketTime.text = formatSlotTime(booking.slotStart)
        binding.ticketDuration.text = formatDuration(booking.durationHours)
        binding.ticketGuests.text = formatPeople(booking.numberOfGuests)
        binding.ticketTotal.text = "R${booking.totalDue}"

        binding.btnReturnHome.setOnClickListener { goHome() }

        // Back must not return to the details screen, where Confirm could book the same visit again
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { goHome() }
    }

    private fun goHome() {
        // The booking is finished, so the next one starts with a clean slate
        bookingViewModel.reset()
        findNavController().navigate(
            R.id.action_bookingConfirm_to_nav_home,
            null,
            navOptions { popUpTo(R.id.nav_home) { inclusive = true } }   // drop the booking screens from the back stack
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}