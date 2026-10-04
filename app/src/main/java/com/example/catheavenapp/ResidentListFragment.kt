package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
// import androidx.navigation.fragment.findNavController
import com.example.catheavenapp.databinding.FragmentResidentListBinding

class ResidentListFragment : Fragment() {

    private var _binding: FragmentResidentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResidentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView with 2 columns
        binding.residentsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Pass dummy data and a click listener for navigation
        val adapter = ResidentAdapter(DummyCatData.cats) { selectedCat ->
            // TODO: Wiehan will pass the cat ID bundle here
            // findNavController().navigate(R.id.action_catList_to_catItem)
        }
        binding.residentsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}