package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.FragmentPastBookingListBinding

class PastBookingsFragment : Fragment() {

    private var _binding: FragmentPastBookingListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPastBookingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the RecyclerView using the unified BookingAdapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = BookingAdapter(DummyData.pastBookings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}