package com.example.catheavenapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentAdminBinding

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_adminFragment_to_adminScheduleFragment)
        }

        binding.cardBookings.setOnClickListener {
            findNavController().navigate(R.id.action_adminFragment_to_adminBookingsFragment)
        }

        binding.cardUsers.setOnClickListener {
            findNavController().navigate(R.id.action_adminFragment_to_adminUsersFragment)
        }

        binding.cardCatsMenu.setOnClickListener {
            findNavController().navigate(R.id.action_adminFragment_to_adminContentFragment)
        }

        binding.btnAdminLogout.setOnClickListener {
            // Courtney will trigger Firebase Auth logout here
            val intent = Intent(requireContext(), LaunchActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}