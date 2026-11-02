package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.catheavenapp.databinding.FragmentResidentListBinding
import com.example.catheavenapp.ui.cats.ResidentsState
import com.example.catheavenapp.ui.cats.ResidentsViewModel

class ResidentListFragment : Fragment() {

    private var _binding: FragmentResidentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ResidentsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResidentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2 columns on phones, more in landscape and on tablets (res/values*/integers.xml)
        val columns = resources.getInteger(R.integer.resident_columns)
        binding.residentsRecyclerView.layoutManager = GridLayoutManager(requireContext(), columns)

        // Tapping a cat opens its profile, passing the cat's ID
        val adapter = ResidentAdapter(emptyList()) { selectedCat ->
            findNavController().navigate(
                R.id.action_catList_to_catItem,
                bundleOf(ResidentFragment.ARG_CAT_ID to selectedCat.id)
            )
        }
        binding.residentsRecyclerView.adapter = adapter

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                ResidentsState.Loading -> binding.residentCountText.text = "Loading residents…"
                is ResidentsState.Failed -> {
                    adapter.updateData(emptyList())
                    binding.residentCountText.text = state.message
                }
                is ResidentsState.Loaded -> {
                    adapter.updateData(state.cats)
                    binding.residentCountText.text = when (state.cats.size) {
                        0 -> "No residents to show yet."
                        1 -> "1 rescued cat calling Cat Heaven home."
                        else -> "${state.cats.size} rescued cats calling Cat Heaven home."
                    }
                }
            }
        }

        if (viewModel.state.value !is ResidentsState.Loaded) viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
