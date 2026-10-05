package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentBookingDateBinding
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.ui.booking.BookingFlowViewModel
import com.example.catheavenapp.ui.booking.formatVisitDate

class BookingDateFragment : Fragment() {

    private var _binding: FragmentBookingDateBinding? = null
    private val binding get() = _binding!!

    private val bookingViewModel: BookingFlowViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Past days can't be booked
        val today = VisitDate.today()
        binding.calendarView.minDate = today.toEpochMillis()

        // Resume a booking in progress, or start from today
        val savedDate = bookingViewModel.draft.value?.date
        if (savedDate == null || savedDate < today) {
            bookingViewModel.selectDate(today)
        } else {
            binding.calendarView.date = savedDate.toEpochMillis()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // CalendarView months are 0-11; VisitDate uses 1-12
            bookingViewModel.selectDate(VisitDate(year, month + 1, dayOfMonth))
        }

        bookingViewModel.draft.observe(viewLifecycleOwner) { draft ->
            draft.date?.let { binding.selectedDateText.text = formatVisitDate(it) }
        }

        binding.btnContinueDate.setOnClickListener {
            findNavController().navigate(R.id.action_bookingDate_to_bookingTime)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}