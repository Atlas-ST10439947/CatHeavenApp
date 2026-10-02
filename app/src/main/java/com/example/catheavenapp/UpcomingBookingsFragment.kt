package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.FragmentUpcomingListBinding

class UpcomingBookingsFragment : Fragment() {

    private var _binding: FragmentUpcomingListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpcomingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BookingAdapter(DummyData.upcomingBookings)
        binding.bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.bookingsRecyclerView.adapter = adapter

        binding.bookingToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnUpcoming -> adapter.updateData(DummyData.upcomingBookings)
                    R.id.btnPast -> adapter.updateData(DummyData.pastBookings)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}