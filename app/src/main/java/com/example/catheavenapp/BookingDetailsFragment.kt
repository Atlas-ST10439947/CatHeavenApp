package com.example.catheavenapp

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentBookingDetailsBinding
import com.example.catheavenapp.domain.booking.BookingDraft
import com.example.catheavenapp.domain.booking.BookingValidator
import com.example.catheavenapp.domain.booking.formatHours
import com.example.catheavenapp.ui.booking.BookingFlowViewModel
import com.example.catheavenapp.ui.booking.SubmitState
import com.example.catheavenapp.ui.booking.formatGuests
import com.google.android.material.snackbar.Snackbar

class BookingDetailsFragment : Fragment() {

    private var _binding: FragmentBookingDetailsBinding? = null
    private val binding get() = _binding!!

    private val bookingViewModel: BookingFlowViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Refill contact details if the user comes back to this screen
        bookingViewModel.draft.value?.let { draft ->
            if (draft.fullName.isNotEmpty()) binding.inputName.setText(draft.fullName)
            if (draft.email.isNotEmpty()) binding.inputEmail.setText(draft.email)
            if (draft.phone.isNotEmpty()) binding.inputPhone.setText(draft.phone)
        }

        binding.btnAddGuest.setOnClickListener { bookingViewModel.addGuest() }
        binding.btnMinusGuest.setOnClickListener { bookingViewModel.removeGuest() }

        bookingViewModel.draft.observe(viewLifecycleOwner) { render(it) }

        binding.btnConfirmBooking.setOnClickListener {
            if (!contactDetailsAreValid()) return@setOnClickListener

            bookingViewModel.updateContact(
                fullName = binding.inputName.text?.toString().orEmpty(),
                email = binding.inputEmail.text?.toString().orEmpty(),
                phone = binding.inputPhone.text?.toString().orEmpty()
            )

            // Writes the booking through the Firestore transaction.
            // The observer below navigates once it has been saved.
            bookingViewModel.submitBooking()
        }

        bookingViewModel.submitState.observe(viewLifecycleOwner) { state ->
            val button = binding.btnConfirmBooking
            when (state) {
                SubmitState.Submitting -> {
                    button.isEnabled = false   // stops a double tap from booking twice
                    button.text = "Booking…"
                }
                SubmitState.Succeeded ->
                    findNavController().navigate(R.id.action_bookingDetails_to_bookingConfirm)
                is SubmitState.Failed -> {
                    button.isEnabled = true
                    button.text = "Confirm Booking"
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    bookingViewModel.onSubmitErrorShown()
                }
                SubmitState.Idle -> {
                    button.isEnabled = true
                    button.text = "Confirm Booking"
                }
            }
        }
    }

    private fun render(draft: BookingDraft) {
        val guests = formatGuests(draft.guests)
        binding.guestCountText.text = guests

        // Disable the +/- buttons at the limits instead of silently ignoring taps
        binding.btnAddGuest.isEnabled = draft.guests < BookingValidator.MAX_GUESTS
        binding.btnMinusGuest.isEnabled = draft.guests > 1

        val hours = formatHours(draft.durationHours)
        val unit = if (draft.durationHours == 1.0) "hour" else "hours"
        binding.summaryDetails.text = "$hours $unit x $guests"

        val quote = bookingViewModel.quoteFor(draft)
        if (quote != null) {
            binding.summaryCalculation.text = "R${quote.pricePerPerson} x ${draft.guests}"
            binding.summaryTotal.text = "R${quote.total}"
        } else {
            binding.summaryCalculation.text = "-"
            binding.summaryTotal.text = "Unavailable"
        }
    }

    private fun contactDetailsAreValid(): Boolean {
        val name = binding.inputName.text?.toString()?.trim().orEmpty()
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.inputPhone.text?.toString()?.trim().orEmpty()
        var valid = true

        if (name.isEmpty()) {
            binding.inputName.error = "Please enter your name"
            valid = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.error = "Please enter a valid email address"
            valid = false
        }
        if (phone.length < 10) {
            binding.inputPhone.error = "Please enter a valid phone number"
            valid = false
        }
        return valid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}