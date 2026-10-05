package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentBookingTimeBinding
import com.example.catheavenapp.domain.booking.PricingCalculator
import com.example.catheavenapp.domain.booking.formatHours
import com.example.catheavenapp.ui.booking.BookingFlowViewModel
import com.example.catheavenapp.ui.booking.formatVisitDate
import com.google.android.material.snackbar.Snackbar

class BookingTimeFragment : Fragment() {

    private var _binding: FragmentBookingTimeBinding? = null
    private val binding get() = _binding!!

    private val bookingViewModel: BookingFlowViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Each duration button and the visit length it stands for
        val durationButtons = listOf(
            binding.btn1Hour to 1.0,
            binding.btn15Hour to 1.5,
            binding.btn2Hour to 2.0
        )

        // Each time button and its start time in the 24-hour format used by Firestore
        val timeButtons = listOf(
            binding.btnTime10 to "10:00",
            binding.btnTime11 to "11:00",
            binding.btnTime12 to "12:00",
            binding.btnTime1 to "13:00",
            binding.btnTime2 to "14:00",
            binding.btnTime3 to "15:00"
        )

        binding.durationToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            durationButtons.firstOrNull { it.first.id == checkedId }
                ?.let { bookingViewModel.selectDuration(it.second) }
        }

        timeButtons.forEach { (button, time) ->
            button.setOnClickListener { bookingViewModel.selectStartTime(time) }
        }

        // Prices on the duration buttons come from the pricing tiers, not hard-coded text
        bookingViewModel.pricingTiers.observe(viewLifecycleOwner) { tiers ->
            durationButtons.forEach { (button, hours) ->
                val tier = PricingCalculator.tierFor(tiers, hours)
                button.isEnabled = tier != null
                button.text = if (tier != null) {
                    "${formatHours(hours)} Hr\nR${tier.pricePerPerson}pp"
                } else {
                    "${formatHours(hours)} Hr\nUnavailable"
                }
            }
        }

        bookingViewModel.draft.observe(viewLifecycleOwner) { draft ->
            binding.headerDateText.text = draft.date?.let { formatVisitDate(it) } ?: ""

            durationButtons.firstOrNull { it.second == draft.durationHours }
                ?.let { binding.durationToggleGroup.check(it.first.id) }

            // Only the chosen time stays highlighted
            timeButtons.forEach { (button, time) -> button.isChecked = time == draft.startTime }
        }

        binding.btnContinueTime.setOnClickListener {
            if (bookingViewModel.draft.value?.hasTime == true) {
                findNavController().navigate(R.id.action_bookingTime_to_bookingDetails)
            } else {
                Snackbar.make(binding.root, "Please choose a time slot", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}