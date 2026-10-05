package com.example.catheavenapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                binding.emailInputLayout.error = "Email and password are required"
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    authResult.user?.getIdToken(true)?.addOnSuccessListener { tokenResult ->
                        val isAdmin = tokenResult.claims["admin"] == true
                        val destination = if (isAdmin) AdminActivity::class.java else MainActivity::class.java
                        startActivity(Intent(requireContext(), destination))
                        requireActivity().finish()
                    }?.addOnFailureListener {
                        _binding?.emailInputLayout?.error = "Could not verify account role"
                    }
                }

                .addOnFailureListener { exception ->
                    binding.emailInputLayout.error = exception.localizedMessage ?: "Login failed"
                }
        }

        binding.registerNavButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}