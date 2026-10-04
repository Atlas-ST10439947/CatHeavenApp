package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.FragmentAdminBookingsBinding

class AdminBookingsFragment : Fragment() {

    private var _binding: FragmentAdminBookingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reuse the unified BookingAdapter with dummy data for the demo
        val adapter = BookingAdapter(DummyData.upcomingBookings)
        binding.adminBookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.adminBookingsRecyclerView.adapter = adapter

        binding.btnFilterDate.setOnClickListener {
            // TODO: Launch a DatePickerDialog for admins to select a specific ledger day
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}