package com.example.catheavenapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catheavenapp.databinding.FragmentResidentBinding
import com.example.catheavenapp.domain.model.Cat

class ResidentAdapter(
    private var cats: List<Cat>,
    private val onCatClick: (Cat) -> Unit
) : RecyclerView.Adapter<ResidentAdapter.ResidentViewHolder>() {

    inner class ResidentViewHolder(val binding: FragmentResidentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Opens the cat's profile when the card is tapped
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onCatClick(cats[position])
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
        with(holder.binding) {
            textView6.text = cat.name
            imageView.contentDescription = "Photo of ${cat.name}"
            // Photos stay the placeholder image: Cloud Storage needs the paid Blaze plan
        }
    }

    override fun getItemCount(): Int = cats.size

    fun updateData(newCats: List<Cat>) {
        cats = newCats
        notifyDataSetChanged()
    }
}
