package com.example.catheavenapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catheavenapp.databinding.FragmentResidentBinding

// Temporary Data Model for your Demo Video
data class CatItem(
    val name: String,
    val breed: String,
    val status: String
)

object DummyCatData {
    val cats = listOf(
        CatItem("Luna", "Domestic Shorthair", "Resident"),
        CatItem("Oliver", "Tabby", "Pending Adoption"),
        CatItem("Milo", "Ginger Tabby", "Resident"),
        CatItem("Bella", "Calico", "Available")
    )
}

class ResidentAdapter(
    private var cats: List<CatItem>,
    private val onCatClick: (CatItem) -> Unit
) : RecyclerView.Adapter<ResidentAdapter.ResidentViewHolder>() {

    inner class ResidentViewHolder(val binding: FragmentResidentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Triggers the navigation action when the card is tapped
            binding.root.setOnClickListener {
                onCatClick(cats[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResidentViewHolder {
        val binding = FragmentResidentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ResidentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResidentViewHolder, position: Int) {
        val cat = cats[position]

        // Assuming your item layout (fragment_resident.xml) has basic TextViews.
        // Update these binding IDs to match your actual XML design.
        /*
        with(holder.binding) {
            catNameText.text = cat.name
            catBreedText.text = cat.breed
            catStatusText.text = cat.status
        }
        */
    }

    override fun getItemCount(): Int = cats.size
}