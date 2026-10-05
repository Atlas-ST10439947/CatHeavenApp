package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.catheavenapp.databinding.FragmentAdminScheduleBinding
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.TimeslotStatus
import com.example.catheavenapp.ui.admin.AdminScheduleViewModel
import com.example.catheavenapp.ui.admin.ScheduleDayState
import com.example.catheavenapp.ui.admin.ScheduleSaveState
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminScheduleFragment : Fragment() {

    private var _binding: FragmentAdminScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminScheduleViewModel by viewModels()

    // The next 30 days, starting today. Noon is used so the day never shifts at midnight.
    private val days: List<VisitDate> = (0 until DAYS_AHEAD).map { offset ->
        VisitDate.fromEpochMillis(VisitDate.today().toEpochMillis() + offset * ONE_DAY + HALF_DAY)
    }
    private var selectedDate: VisitDate = days.first()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Select Day" now lists real dates, because timeslots belong to a specific day
        val labels = days.map { dayLabel(it) }
        binding.dayDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )
        binding.dayDropdown.setText(dayLabel(selectedDate), false)
        binding.dayDropdown.setOnClickListener { binding.dayDropdown.showDropDown() }
        binding.dayDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedDate = days[position]
            viewModel.loadDay(selectedDate)
        }

        viewModel.dayState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ScheduleDayState.Loaded -> if (state.slots.isNotEmpty()) {
                    // Show the day's current settings
                    binding.inputCapacity.setText(state.slots.maxOf { it.capacity }.toString())
                    binding.switchOpenStatus.isChecked = state.slots.any { it.status != TimeslotStatus.CLOSED }
                }
                is ScheduleDayState.Failed ->
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                ScheduleDayState.Loading -> Unit
            }
            updateSaveButton()
        }

        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ScheduleSaveState.Saved -> {
                    Snackbar.make(
                        binding.root,
                        "Saved ${state.slotCount} time slots for ${dayLabel(selectedDate)}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.onSaveMessageShown()
                }
                is ScheduleSaveState.Failed -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.onSaveMessageShown()
                }
                ScheduleSaveState.Idle, ScheduleSaveState.Saving -> Unit
            }
            updateSaveButton()
        }

        binding.btnSaveSchedule.setOnClickListener {
            val capacity = binding.inputCapacity.text?.toString()?.trim()?.toIntOrNull()
            if (capacity == null) {
                binding.inputCapacity.error = "Capacity is required"
                return@setOnClickListener
            }
            viewModel.save(selectedDate, capacity, binding.switchOpenStatus.isChecked)
        }

        viewModel.loadDay(selectedDate)
    }

    private fun updateSaveButton() {
        val day = viewModel.dayState.value
        val saving = viewModel.saveState.value == ScheduleSaveState.Saving
        binding.btnSaveSchedule.isEnabled = day is ScheduleDayState.Loaded && !saving
        binding.btnSaveSchedule.text = when {
            saving -> "Saving…"
            day is ScheduleDayState.Loaded && day.slots.isEmpty() -> "Create Time Slots"
            else -> "Save Schedule"
        }
    }

    private fun dayLabel(date: VisitDate): String =
        SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault()).format(Date(date.toEpochMillis()))

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DAYS_AHEAD = 30
        const val ONE_DAY = 24L * 60 * 60 * 1000
        const val HALF_DAY = 12L * 60 * 60 * 1000
    }
}
