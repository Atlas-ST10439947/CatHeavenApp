package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNavProfile.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_profile)
        }

        binding.btnNavNotifs.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_notifs)
        }

        binding.btnLogout.setOnClickListener {
            // TODO: Courtney will trigger Firebase.auth.signOut() here and route back to Login
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}