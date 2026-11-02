package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentBookingTimeBinding
import com.example.catheavenapp.databinding.ItemTimeSlotBinding
import com.example.catheavenapp.domain.booking.PricingCalculator
import com.example.catheavenapp.domain.booking.formatHours
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import com.example.catheavenapp.ui.booking.BookingFlowViewModel
import com.example.catheavenapp.ui.booking.TimeslotsState
import com.example.catheavenapp.ui.booking.formatSlotTime
import com.example.catheavenapp.ui.booking.formatVisitDate
import com.google.android.material.button.MaterialButton
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

        setUpDurationButtons()

        bookingViewModel.draft.observe(viewLifecycleOwner) { draft ->
            draft.date?.let { binding.headerDateText.text = formatVisitDate(it) }
        }

        bookingViewModel.timeslots.observe(viewLifecycleOwner) { renderSlots(it) }
        bookingViewModel.loadTimeslots()   // fresh counts every time the screen opens

        binding.btnContinueTime.setOnClickListener {
            if (bookingViewModel.draft.value?.hasTimeslot == true) {
                // Keep the same action ID your previous version of this file used
                findNavController().navigate(R.id.action_bookingTime_to_bookingDetails)
            } else {
                Snackbar.make(binding.root, "Please choose a time slot", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUpDurationButtons() {
        val durationButtons = mapOf(
            binding.btn1Hour to 1.0,
            binding.btn15Hour to 1.5,
            binding.btn2Hour to 2.0
        )
// Show the live price on each button, and disable lengths with no active tier
        bookingViewModel.pricingTiers.observe(viewLifecycleOwner) { tiers ->
            durationButtons.forEach { (button, hours) ->
                button.isSingleLine = false   // allow the price on a second line
                button.maxLines = 2
                val tier = PricingCalculator.tierFor(tiers, hours)
                button.text = if (tier != null) {
                    "${formatHours(hours)} Hr\nR${tier.pricePerPerson}pp"
                } else {
                    "${formatHours(hours)} Hr\nUnavailable"
                }
                button.isEnabled = tier != null
            }
        }

        // Restore the duration chosen earlier in this booking
        val saved = bookingViewModel.draft.value?.durationHours ?: 1.0
        durationButtons.entries.firstOrNull { it.value == saved }
            ?.let { binding.durationToggleGroup.check(it.key.id) }

        binding.durationToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            durationButtons.entries.firstOrNull { it.key.id == checkedId }
                ?.let { bookingViewModel.selectDuration(it.value) }
        }
    }

    private fun renderSlots(state: TimeslotsState) {
        val grid = binding.timeSlotGrid
        grid.removeAllViews()

        when (state) {
            TimeslotsState.Loading -> grid.addView(messageView("Loading available times…"))

            TimeslotsState.Failed -> grid.addView(
                messageView("We couldn't load the times. Check your connection and try again.")
            )

            is TimeslotsState.Loaded -> {
                // Ignore a result for a different day (the date changed while it loaded)
                if (state.date != bookingViewModel.draft.value?.date) return

                // Closed slots (e.g. blocked for an event) aren't shown at all
                val slots = state.slots.filter { it.status != TimeslotStatus.CLOSED }
                if (slots.isEmpty()) {
                    grid.addView(messageView("There are no visits on this day. Please pick another date."))
                    return
                }

                val selectedId = bookingViewModel.draft.value?.timeslotId
                slots.forEach { slot -> grid.addView(slotButton(slot, selectedId)) }
            }
        }
    }

    private fun slotButton(slot: Timeslot, selectedId: String?): MaterialButton {
        val button = ItemTimeSlotBinding.inflate(layoutInflater, binding.timeSlotGrid, false).root
        val time = formatSlotTime(slot.startTime)

        button.text = if (slot.isBookable) "$time\n${slot.remainingCapacity} left" else "$time\nFull"
        button.isEnabled = slot.isBookable
        button.isChecked = slot.isBookable && slot.id == selectedId
        button.tag = slot.id

        button.setOnClickListener {
            bookingViewModel.selectTimeslot(slot)
            // Exactly one slot stays checked, even if the same one is tapped twice
            val grid = binding.timeSlotGrid
            for (i in 0 until grid.childCount) {
                (grid.getChildAt(i) as? MaterialButton)?.let { it.isChecked = it.tag == slot.id }
            }
        }
        return button
    }

    /** A message that spans both columns of the grid. */
    private fun messageView(text: String): TextView {
        val padding = (16 * resources.displayMetrics.density).toInt()
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
            setPadding(padding, padding, padding, padding)
            layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(0, 2, 1f)
            ).apply { width = 0 }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}