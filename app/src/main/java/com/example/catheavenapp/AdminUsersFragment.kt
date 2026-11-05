package com.example.catheavenapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.FragmentAdminUsersBinding
import com.example.catheavenapp.domain.model.User
import com.example.catheavenapp.domain.model.UserSearch
import com.example.catheavenapp.ui.admin.AdminUsersState
import com.example.catheavenapp.ui.admin.AdminUsersViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class AdminUsersFragment : Fragment() {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminUsersViewModel by viewModels()
    private lateinit var adapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserAdapter(emptyList()) { user -> confirmBlockChange(user) }
        binding.adminUsersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.adminUsersRecyclerView.adapter = adapter

        viewModel.state.observe(viewLifecycleOwner) { render() }
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.messageShown()
            }
        }

        // Real-time search filter by name or email
        binding.searchUserEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.load()
    }

    private fun render() {
        when (val state = viewModel.state.value) {
            null, AdminUsersState.Loading -> showMessage("Loading supporters…")
            is AdminUsersState.Failed -> showMessage(state.message)
            is AdminUsersState.Loaded -> {
                val query = binding.searchUserEdit.text?.toString().orEmpty()
                val matches = UserSearch.filter(state.users, query)
                adapter.updateData(matches)
                when {
                    matches.isNotEmpty() -> binding.emptyStateText.visibility = View.GONE
                    query.isBlank() -> showMessage("No supporters have registered yet.")
                    else -> showMessage("No supporters match \"$query\".")
                }
            }
        }
    }

    /** Asks before blocking or unblocking, so a mistaken tap doesn't lock someone out. */
    private fun confirmBlockChange(user: User) {
        val name = user.fullName.ifBlank { user.email }
        val builder = MaterialAlertDialogBuilder(requireContext())
        if (user.blocked) {
            builder.setTitle("Unblock $name?")
                .setMessage("They'll be able to log in and book visits again.")
                .setPositiveButton("Unblock") { _, _ -> viewModel.setBlocked(user, false) }
        } else {
            builder.setTitle("Block $name?")
                .setMessage(
                    "They won't be able to log in or book visits. When they try to log in, " +
                        "they'll be asked to contact Cat Heaven.\n\nTheir profile and existing " +
                        "bookings stay as they are, so cancel any upcoming bookings in the " +
                        "Booking Ledger if needed. You can unblock them at any time."
                )
                .setPositiveButton("Block") { _, _ -> viewModel.setBlocked(user, true) }
        }
        builder.setNegativeButton("Cancel", null).show()
    }

    private fun showMessage(text: String) {
        adapter.updateData(emptyList())
        binding.emptyStateText.text = text
        binding.emptyStateText.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
