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
import com.example.catheavenapp.domain.model.UserSearch
import com.example.catheavenapp.ui.admin.AdminUsersState
import com.example.catheavenapp.ui.admin.AdminUsersViewModel

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

        adapter = UserAdapter(emptyList())
        binding.adminUsersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.adminUsersRecyclerView.adapter = adapter

        viewModel.state.observe(viewLifecycleOwner) { render() }

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
