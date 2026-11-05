package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentSettingsBinding
import com.example.catheavenapp.domain.settings.ThemeMode
import com.example.catheavenapp.util.ThemePreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent


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

        showAppearance()
        binding.btnAppearance.setOnClickListener { chooseAppearance() }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LaunchActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun showAppearance() {
        val mode = ThemePreference.current(requireContext())
        binding.btnAppearance.text = "Appearance: ${mode.label}"
    }

    /** System default, Light or Dark. The app switches as soon as one is picked. */
    private fun chooseAppearance() {
        val modes = ThemeMode.entries
        val selected = modes.indexOf(ThemePreference.current(requireContext()))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Appearance")
            .setSingleChoiceItems(modes.map { it.label }.toTypedArray(), selected) { dialog, which ->
                dialog.dismiss()
                if (which != selected) {
                    ThemePreference.save(requireContext(), modes[which])
                    // If the colours don't change (e.g. System default -> Light on a light
                    // phone), the screen isn't recreated, so update the label here too
                    if (_binding != null) showAppearance()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}