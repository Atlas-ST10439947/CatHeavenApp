package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
// import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentBookingDateBinding
import java.text.SimpleDateFormat
import java.util.Locale

class BookingDateFragment : Fragment() {

    private var _binding: FragmentBookingDateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Capture Calendar clicks for UX feedback
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            val format = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            binding.selectedDateText.text = format.format(calendar.time)
        }

        binding.btnContinueDate.setOnClickListener {
            // TODO: Wiehan will hook up data passing here
            // findNavController().navigate(R.id.action_date_to_time)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}