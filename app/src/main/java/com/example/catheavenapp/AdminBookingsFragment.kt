package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.FragmentAdminBookingsBinding
import com.example.catheavenapp.domain.booking.BookingLedger
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import com.example.catheavenapp.ui.admin.AdminBookingsViewModel
import com.example.catheavenapp.ui.admin.LedgerActionState
import com.example.catheavenapp.ui.admin.LedgerState
import com.example.catheavenapp.ui.booking.formatDuration
import com.example.catheavenapp.ui.booking.formatGuests
import com.example.catheavenapp.ui.booking.formatSlotTime
import com.example.catheavenapp.ui.booking.formatVisitDate
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.TimeZone

/** Admin Booking Ledger: every booking on a chosen day, with totals and cancellation. */
class AdminBookingsFragment : Fragment() {

    private var _binding: FragmentAdminBookingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminBookingsViewModel by viewModels()
    private lateinit var adapter: BookingAdapter

    /** The bookings currently on screen, so a tapped card can be matched to its booking. */
    private var shown: List<Booking> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BookingAdapter(emptyList()) { item ->
            shown.firstOrNull { it.id == item.id }?.let(::showBookingDetails)
        }
        binding.adminBookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.adminBookingsRecyclerView.adapter = adapter

        viewModel.date.observe(viewLifecycleOwner) { date ->
            val label = if (date == VisitDate.today()) "Today" else formatVisitDate(date)
            binding.btnFilterDate.text = "Showing: $label"
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                LedgerState.Loading -> {
                    binding.ledgerSummaryText.text = ""
                    showMessage("Loading bookings…")
                }
                is LedgerState.Failed -> {
                    binding.ledgerSummaryText.text = ""
                    showMessage(state.message)
                }
                is LedgerState.Loaded -> {
                    shown = state.ledger.bookings
                    adapter.updateData(shown.map { it.toItem() })
                    binding.ledgerSummaryText.text = BookingLedger.summary(state.ledger)
                    if (shown.isEmpty()) showMessage("No bookings on this day.")
                    else binding.emptyStateText.visibility = View.GONE
                }
            }
        }

        viewModel.actionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LedgerActionState.Done -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.onActionMessageShown()
                }
                is LedgerActionState.Failed -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.onActionMessageShown()
                }
                LedgerActionState.Idle, LedgerActionState.Working -> Unit
            }
        }

        binding.btnFilterDate.setOnClickListener { pickDay() }

        if (viewModel.state.value !is LedgerState.Loaded) viewModel.load()
    }

    private fun pickDay() {
        val utc = TimeZone.getTimeZone("UTC")
        val current = viewModel.date.value ?: VisitDate.today()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Show bookings for")
            .setSelection(current.toEpochMillis(utc))
            .build()
        // The picker returns UTC midnight; read the calendar day back in UTC
        picker.addOnPositiveButtonClickListener { picked ->
            viewModel.selectDate(VisitDate.fromEpochMillis(picked, utc))
        }
        picker.show(childFragmentManager, "ledgerDatePicker")
    }

    private fun showBookingDetails(booking: Booking) {
        val details = buildString {
            appendLine("Reference: ${booking.bookingReference}")
            appendLine("Time: ${formatSlotTime(booking.slotStart)} for ${formatDuration(booking.durationHours)}")
            appendLine("Guests: ${formatGuests(booking.numberOfGuests)}")
            appendLine("Due at the venue: R${booking.totalDue}")
            append("Status: ${booking.status.value.replaceFirstChar { it.uppercase() }}")
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(booking.supporterName.ifBlank { "Booking" })
            .setMessage(details)
            .setNegativeButton("Close", null)
        if (booking.status == BookingStatus.CONFIRMED) {
            dialog.setPositiveButton("Cancel booking") { _, _ -> confirmCancel(booking) }
        }
        dialog.show()
    }

    private fun confirmCancel(booking: Booking) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel this booking?")
            .setMessage(
                "${booking.supporterName}'s visit (${formatGuests(booking.numberOfGuests)}) will be cancelled " +
                    "and the seats made available again. This can't be undone."
            )
            .setNegativeButton("Keep booking", null)
            .setPositiveButton("Cancel booking") { _, _ -> viewModel.cancel(booking) }
            .show()
    }

    private fun showMessage(text: String) {
        shown = emptyList()
        adapter.updateData(emptyList())
        binding.emptyStateText.text = text
        binding.emptyStateText.visibility = View.VISIBLE
    }

    private fun Booking.toItem() = BookingItem(
        status = "${status.value.uppercase()}  ·  $bookingReference",
        date = supporterName.ifBlank { "(no name)" },
        price = "R$totalDue",
        timeDuration = "${formatSlotTime(slotStart)} | ${formatDuration(durationHours)}",
        guests = formatGuests(numberOfGuests),
        id = id
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
