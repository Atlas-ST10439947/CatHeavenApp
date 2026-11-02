package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.catheavenapp.databinding.FragmentSettingsNotifsBinding
import com.example.catheavenapp.domain.profile.NotificationPrefs
import com.example.catheavenapp.ui.profile.ProfileSaveState
import com.example.catheavenapp.ui.profile.ProfileState
import com.example.catheavenapp.ui.profile.ProfileViewModel
import com.google.android.material.snackbar.Snackbar

/** Saves the supporter's notification choices to notificationPrefs on their users document. */
class SettingsNotifsFragment : Fragment() {

    private var _binding: FragmentSettingsNotifsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    /** True while the switches are being set from saved data, so that doesn't count as a change. */
    private var applyingSavedState = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsNotifsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                ProfileState.Loading -> setSwitchesEnabled(false)
                is ProfileState.Failed -> {
                    setSwitchesEnabled(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is ProfileState.Loaded -> {
                    applyingSavedState = true
                    binding.switchEmail.isChecked = state.profile.notifications.email
                    binding.switchInApp.isChecked = state.profile.notifications.inApp
                    applyingSavedState = false
                    setSwitchesEnabled(true)
                }
            }
        }

        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileSaveState.Saved -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    viewModel.onSaveMessageShown()
                }
                is ProfileSaveState.Failed -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.onSaveMessageShown()
                }
                ProfileSaveState.Idle, ProfileSaveState.Saving -> Unit
            }
        }

        binding.switchEmail.setOnCheckedChangeListener { _, _ -> onSwitchChanged() }
        binding.switchInApp.setOnCheckedChangeListener { _, _ -> onSwitchChanged() }

        if (viewModel.state.value !is ProfileState.Loaded) viewModel.load()
    }

    private fun onSwitchChanged() {
        if (applyingSavedState) return
        viewModel.saveNotifications(
            NotificationPrefs(email = binding.switchEmail.isChecked, inApp = binding.switchInApp.isChecked)
        )
    }

    private fun setSwitchesEnabled(enabled: Boolean) {
        binding.switchEmail.isEnabled = enabled
        binding.switchInApp.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
