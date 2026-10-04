package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.catheavenapp.databinding.FragmentSettingsNotifsBinding

class SettingsNotifsFragment : Fragment() {

    private var _binding: FragmentSettingsNotifsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsNotifsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchEmail.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update user's email preference in Firestore
        }

        binding.switchInApp.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update user's in-app notification preference in Firestore
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}