package com.example.catheavenapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.FrameLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseNetworkException
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
                            if (doc.getBoolean("blocked") == true) {
                                // An admin blocked this account: sign straight back out
                                FirebaseAuth.getInstance().signOut()
                                showBlockedMessage()
                                return@addOnSuccessListener
                            }
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

        binding.forgotPasswordTextView.setOnClickListener { showPasswordReset() }

        binding.registerNavButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    /**
     * Sends Firebase's password reset email. The message is the same whether or not an
     * account exists, so the screen can't be used to find out who has an account.
     */
    private fun showPasswordReset() {
        val context = requireContext()
        val field = TextInputLayout(context).apply { hint = "Email address" }
        val input = TextInputEditText(field.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.emailEditText.text?.toString()?.trim())
        }
        field.addView(input)
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(context).apply {
            setPadding(padding, padding / 3, padding, 0)
            addView(field)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Reset your password")
            .setMessage("We'll email you a link to choose a new password.")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send link", null)   // set below, so an invalid email keeps the dialog open
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val email = input.text?.toString()?.trim().orEmpty()
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    field.error = "Please enter a valid email address"
                    return@setOnClickListener
                }
                dialog.dismiss()
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    val views = _binding ?: return@addOnCompleteListener
                    val message = if (!task.isSuccessful && task.exception is FirebaseNetworkException) {
                        "We couldn't send the email. Check your connection and try again."
                    } else {
                        "If there's an account for $email, a reset link is on its way. Check your inbox and spam folder."
                    }
                    Snackbar.make(views.root, message, Snackbar.LENGTH_LONG)
                        .apply {
                            // Show the whole message, not just the first two lines
                            view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                                ?.maxLines = 5
                        }
                        .show()
                }
            }
        }
        dialog.show()
    }

    private fun showBlockedMessage() {
        if (!isAdded) return
        _binding?.passwordEditText?.text?.clear()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Your account has been blocked")
            .setMessage("Please contact us at $CONTACT_EMAIL to find out more.")
            .setPositiveButton("OK", null)
            .show()
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

    private companion object {
        /** Cat Heaven's contact email, as shown on the Home screen. */
        const val CONTACT_EMAIL = "meow@catheaven.co.za"
    }
}