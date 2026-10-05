package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.FragmentUpcomingBookingListBinding
import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import com.example.catheavenapp.ui.booking.MyBookingsState
import com.example.catheavenapp.ui.booking.MyBookingsViewModel
import com.example.catheavenapp.ui.booking.formatDateMillis
import com.example.catheavenapp.ui.booking.formatDuration
import com.example.catheavenapp.ui.booking.formatGuests
import com.example.catheavenapp.ui.booking.formatSlotTime

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

        adapter = BookingAdapter(emptyList())
        binding.bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.bookingsRecyclerView.adapter = adapter

        viewModel.state.observe(viewLifecycleOwner) { render(it) }

        // Switching tabs just re-renders from the bookings already loaded
        binding.bookingToggleGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) viewModel.state.value?.let { render(it) }
        }

        viewModel.load()   // fresh every time the screen opens
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
}