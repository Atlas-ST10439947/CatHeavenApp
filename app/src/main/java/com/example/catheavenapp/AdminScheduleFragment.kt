package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.catheavenapp.databinding.FragmentAdminScheduleBinding

class AdminScheduleFragment : Fragment() {

    private var _binding: FragmentAdminScheduleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveSchedule.setOnClickListener {
            val selectedDay = binding.dayDropdown.text.toString()
            val capacity = binding.inputCapacity.text.toString()
            val isOpen = binding.switchOpenStatus.isChecked

            if (capacity.isNotEmpty()) {
                // TODO: Wiehan will trigger the API call to update the Firestore timeslots collection here
                Toast.makeText(requireContext(), "$selectedDay schedule updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                binding.inputCapacity.error = "Capacity is required"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}