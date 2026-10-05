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
import com.google.firebase.firestore.FirebaseFirestore

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
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid == null) {
                        openHome(MainActivity::class.java)
                        return@addOnSuccessListener
                    }
                    // Admins go to the admin dashboard, everyone else to the supporter app.
                    // This only decides which screens to show: the security rules still
                    // check admin rights on every admin read and write.
                    FirebaseFirestore.getInstance().collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val isAdmin = doc.getString("role") == "admin"
                            openHome(if (isAdmin) AdminActivity::class.java else MainActivity::class.java)
                        }
                        .addOnFailureListener { openHome(MainActivity::class.java) }
                }

                .addOnFailureListener { exception ->
                    if (_binding == null) return@addOnFailureListener
                    binding.emailInputLayout.error = exception.localizedMessage ?: "Login failed"
                }
        }

        binding.registerNavButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun openHome(target: Class<*>) {
        if (!isAdded) return   // the user left this screen while signing in
        startActivity(Intent(requireContext(), target))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}