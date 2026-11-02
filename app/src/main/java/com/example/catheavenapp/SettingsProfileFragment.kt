package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.catheavenapp.databinding.FragmentSettingsProfileBinding
import com.example.catheavenapp.domain.profile.ProfileCheck
import com.example.catheavenapp.domain.profile.ProfileField
import com.example.catheavenapp.ui.profile.ProfileSaveState
import com.example.catheavenapp.ui.profile.ProfileState
import com.example.catheavenapp.ui.profile.ProfileViewModel
import com.google.android.material.snackbar.Snackbar

/** Lets the supporter edit their name, phone and location on their users document. */
class SettingsProfileFragment : Fragment() {

    private var _binding: FragmentSettingsProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                ProfileState.Loading -> setFormEnabled(false)
                is ProfileState.Failed -> {
                    setFormEnabled(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is ProfileState.Loaded -> {
                    // Only fill the fields once, so a refresh never wipes what the user is typing
                    if (savedInstanceState == null && binding.editName.tag == null) {
                        binding.editName.setText(state.profile.fullName)
                        binding.editPhone.setText(state.profile.phone)
                        binding.editLocation.setText(state.profile.location)
                        binding.editName.tag = LOADED
                    }
                    binding.editEmail.setText(state.profile.email)
                    setFormEnabled(true)
                }
            }
        }

        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            val saving = state == ProfileSaveState.Saving
            binding.btnUpdateProfile.isEnabled = !saving && viewModel.state.value is ProfileState.Loaded
            binding.btnUpdateProfile.text = if (saving) "Saving…" else "Update Profile"
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

        binding.btnUpdateProfile.setOnClickListener {
            clearErrors()
            val check = viewModel.saveDetails(
                fullName = binding.editName.text?.toString().orEmpty(),
                phone = binding.editPhone.text?.toString().orEmpty(),
                location = binding.editLocation.text?.toString().orEmpty()
            )
            if (check is ProfileCheck.Invalid) {
                val layout = when (check.field) {
                    ProfileField.NAME -> binding.nameInputLayout
                    ProfileField.PHONE -> binding.phoneInputLayout
                    ProfileField.LOCATION -> binding.locationInputLayout
                }
                layout.error = check.message
                layout.requestFocus()
            }
        }

        if (viewModel.state.value !is ProfileState.Loaded) viewModel.load()
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.editName.isEnabled = enabled
        binding.editPhone.isEnabled = enabled
        binding.editLocation.isEnabled = enabled
        binding.btnUpdateProfile.isEnabled = enabled && viewModel.saveState.value != ProfileSaveState.Saving
    }

    private fun clearErrors() {
        binding.nameInputLayout.error = null
        binding.phoneInputLayout.error = null
        binding.locationInputLayout.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val LOADED = "loaded"
    }
}
