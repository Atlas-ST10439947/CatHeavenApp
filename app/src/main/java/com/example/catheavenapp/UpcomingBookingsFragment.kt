package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.FragmentUpcomingBookingListBinding
import com.example.catheavenapp.domain.booking.BookingChanges
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.ui.booking.BookingActionState
import com.example.catheavenapp.ui.booking.MyBookingsState
import com.example.catheavenapp.ui.booking.MyBookingsViewModel
import com.example.catheavenapp.ui.booking.formatDateMillis
import com.example.catheavenapp.ui.booking.formatDuration
import com.example.catheavenapp.ui.booking.formatGuests
import com.example.catheavenapp.ui.booking.formatVisitDate
import com.example.catheavenapp.ui.booking.formatSlotTime
import com.example.catheavenapp.util.Diagnostics
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.TimeZone
import kotlin.coroutines.cancellation.CancellationException

class UpcomingBookingsFragment : Fragment() {

    private var _binding: FragmentUpcomingBookingListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyBookingsViewModel by viewModels()
    private lateinit var adapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpcomingBookingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tapping an upcoming booking offers to reschedule or cancel it
        adapter = BookingAdapter(emptyList()) { item -> findBooking(item.id)?.let(::showBookingOptions) }
        binding.bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.bookingsRecyclerView.adapter = adapter

        viewModel.state.observe(viewLifecycleOwner) { render(it) }

        // Switching tabs just re-renders from the bookings already loaded
        binding.bookingToggleGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) viewModel.state.value?.let { render(it) }
        }

        viewModel.actionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookingActionState.Done -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.onActionMessageShown()
                }
                is BookingActionState.Failed -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.onActionMessageShown()
                }
                BookingActionState.Idle, BookingActionState.Working -> Unit
            }
        }

        viewModel.load()   // fresh every time the screen opens
    }

    private fun findBooking(id: String): Booking? {
        val history = (viewModel.state.value as? MyBookingsState.Loaded)?.history ?: return null
        return (history.upcoming + history.past).firstOrNull { it.id == id }
    }

    private fun showBookingOptions(booking: Booking) {
        val summary = "${formatSlotTime(booking.slotStart)} for ${formatDuration(booking.durationHours)}, " +
            "${formatGuests(booking.numberOfGuests)}, R${booking.totalDue} due at the venue.\n" +
            "Reference: ${booking.bookingReference}"
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(formatDateMillis(booking.slotDateMillis))
            .setNegativeButton("Close", null)

        if (BookingChanges.canChange(booking, VisitDate.today())) {
            dialog.setMessage(summary)
                .setPositiveButton("Reschedule") { _, _ -> pickNewDate(booking) }
                .setNeutralButton("Cancel booking") { _, _ -> confirmCancel(booking) }
        } else if (booking.status == BookingStatus.CONFIRMED) {
            dialog.setMessage("$summary\n\nChanges can be made up to the day before your visit. Please contact Cat Heaven.")
        } else {
            dialog.setMessage(summary)
        }
        dialog.show()
    }

    private fun confirmCancel(booking: Booking) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel this visit?")
            .setMessage("Your places on ${formatDateMillis(booking.slotDateMillis)} will be released for other supporters.")
            .setNegativeButton("Keep booking", null)
            .setPositiveButton("Cancel visit") { _, _ -> viewModel.cancel(booking) }
            .show()
    }

    /** Step 1 of rescheduling: choose a day from tomorrow onwards. */
    private fun pickNewDate(booking: Booking) {
        val utc = TimeZone.getTimeZone("UTC")
        val tomorrowUtc = MaterialDatePicker.todayInUtcMilliseconds() + ONE_DAY
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Choose a new day")
            .setSelection(tomorrowUtc)
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointForward.from(tomorrowUtc)).build()
            )
            .build()
        picker.addOnPositiveButtonClickListener { picked ->
            pickNewSlot(booking, VisitDate.fromEpochMillis(picked, utc))
        }
        picker.show(childFragmentManager, "rescheduleDatePicker")
    }

    /** Step 2: choose one of that day's slots with room for the whole group. */
    private fun pickNewSlot(booking: Booking, date: VisitDate) {
        viewLifecycleOwner.lifecycleScope.launch {
            val slots: List<Timeslot> = try {
                viewModel.slotsThatFit(date, booking)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn("MyBookings", "Loading slots for reschedule failed", e)
                _binding?.let {
                    Snackbar.make(it.root, "We couldn't load that day. Check your connection.", Snackbar.LENGTH_LONG).show()
                }
                return@launch
            }
            if (!isAdded) return@launch
            if (slots.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(formatVisitDate(date))
                    .setMessage("There's no time with room for ${formatGuests(booking.numberOfGuests).lowercase()} that day. Please try another day.")
                    .setPositiveButton("Choose another day") { _, _ -> pickNewDate(booking) }
                    .setNegativeButton("Close", null)
                    .show()
                return@launch
            }
            val labels = slots.map { slot ->
                val left = slot.remainingCapacity
                "${formatSlotTime(slot.startTime)}  ·  ${if (left == 1) "1 place" else "$left places"} left"
            }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Times on ${formatVisitDate(date)}")
                .setItems(labels) { _, which -> viewModel.reschedule(booking, slots[which]) }
                .setNegativeButton("Back") { _, _ -> pickNewDate(booking) }
                .show()
        }
    }

    private fun render(state: MyBookingsState) {
        val showingPast = binding.bookingToggleGroup.checkedButtonId == R.id.btnPast

        when (state) {
            MyBookingsState.Loading -> showMessage("Loading your bookings…")

            is MyBookingsState.Failed -> showMessage(state.message)

            is MyBookingsState.Loaded -> {
                val bookings = if (showingPast) state.history.past else state.history.upcoming
                adapter.updateData(bookings.map { it.toItem(isUpcoming = !showingPast) })
                when {
                    bookings.isNotEmpty() -> binding.emptyStateText.visibility = View.GONE
                    showingPast -> showMessage("No past bookings yet.")
                    else -> showMessage("No upcoming bookings yet. Book a visit and it will show here.")
                }
            }
        }
    }

    private fun showMessage(text: String) {
        adapter.updateData(emptyList())
        binding.emptyStateText.text = text
        binding.emptyStateText.visibility = View.VISIBLE
    }

    /** Converts a saved booking into the card format Atlas's adapter already displays. */
    private fun Booking.toItem(isUpcoming: Boolean) = BookingItem(
        id = id,
        status = when {
            status == BookingStatus.CANCELLED -> "CANCELLED"
            isUpcoming -> "UPCOMING"
            else -> "PAST"
        },
        date = formatDateMillis(slotDateMillis),
        price = "R$totalDue",
        timeDuration = "${formatSlotTime(slotStart)} | ${formatDuration(durationHours)}",
        guests = formatGuests(numberOfGuests)
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ONE_DAY = 24L * 60 * 60 * 1000
    }
}