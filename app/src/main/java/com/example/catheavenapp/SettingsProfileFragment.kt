package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.catheavenapp.databinding.FragmentSettingsProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class SettingsProfileFragment : Fragment() {

    private var _binding: FragmentSettingsProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)

        // Email belongs to the login account, so show it but don't allow edits
        binding.editEmail.isEnabled = false

        // Load the saved profile from Firestore
        userDoc.get()
            .addOnSuccessListener { doc ->
                _binding?.let { b ->
                    b.editName.setText(doc.getString("fullName") ?: "")
                    b.editEmail.setText(doc.getString("email") ?: currentUser.email ?: "")
                    b.editPhone.setText(doc.getString("phone") ?: "")
                    b.editLocation.setText(doc.getString("location") ?: "")
                }
            }
            .addOnFailureListener {
                _binding?.let { Snackbar.make(it.root, "Could not load your profile", Snackbar.LENGTH_SHORT).show() }
            }

        // Save changes back to Firestore
        binding.btnUpdateProfile.setOnClickListener {
            val updates = mapOf(
                "fullName" to binding.editName.text.toString().trim(),
                "phone" to binding.editPhone.text.toString().trim(),
                "location" to binding.editLocation.text.toString().trim()
            )
            userDoc.update(updates)
                .addOnSuccessListener {
                    _binding?.let { Snackbar.make(it.root, "Profile updated", Snackbar.LENGTH_SHORT).show() }
                }
                .addOnFailureListener { e ->
                    _binding?.let { Snackbar.make(it.root, e.localizedMessage ?: "Update failed", Snackbar.LENGTH_LONG).show() }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}