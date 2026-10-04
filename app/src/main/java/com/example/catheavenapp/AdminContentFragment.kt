package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.FragmentAdminContentBinding

class AdminContentFragment : Fragment() {

    private var _binding: FragmentAdminContentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.adminContentRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Listen for Toggle Button clicks to swap lists
        binding.contentToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnManageCats -> {
                        // TODO: Bind the ResidentAdapter here
                    }
                    R.id.btnManageMenu -> {
                        // TODO: Bind the MenuAdapter here
                    }
                }
            }
        }

        binding.fabAddContent.setOnClickListener {
            // TODO: Open a dialog or new fragment to add a new Cat or Menu item to Firestore
            Toast.makeText(requireContext(), "Add new item clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}