package com.example.catheavenapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerSubmitButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailRegEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            val password = binding.passwordRegEditText.text.toString().trim()
            val confirmPassword = binding.passwordConfirmEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                binding.emailRegEditText.error = "Name, email, and password are required"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.passwordConfirmEditText.error = "Passwords do not match"
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    val userProfile = hashMapOf(
                        "fullName" to name,
                        "email" to email,
                        "phone" to phone,
                        "role" to "supporter",
                        "dateJoined" to Timestamp.now()
                    )

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(userProfile)
                        .addOnSuccessListener {
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener { exception ->
                            binding.emailRegEditText.error = "Account created, but profile setup failed: ${exception.localizedMessage}"
                        }
                }
                .addOnFailureListener { exception ->
                    binding.emailRegEditText.error = exception.localizedMessage ?: "Registration failed"
                }
        }

        binding.loginNavButton.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}