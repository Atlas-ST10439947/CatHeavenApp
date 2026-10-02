package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.catheavenapp.databinding.FragmentResidentBinding

class ResidentFragment : Fragment() {

    private var _binding: FragmentResidentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResidentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Static view for the Front-End demo.
        // Wiehan will populate the cat's biography, age, and photo dynamically from Firebase here.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}