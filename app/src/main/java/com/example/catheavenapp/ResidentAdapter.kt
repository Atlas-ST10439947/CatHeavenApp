package com.example.catheavenapp

import com.example.catheavenapp.ui.admin.CatPhotos
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
        val context = holder.itemView.context

        with(holder.binding) {
            textView6.text = cat.name
            imageView.contentDescription = "Photo of ${cat.name}"

            // Load the image from res/drawable based on the Firestore string
            val resourceId = context.resources.getIdentifier(CatPhotos.drawableKey(cat.photoUrl, cat.name), "drawable", context.packageName)

            if (resourceId != 0) {
                imageView.setImageResource(resourceId)
            } else {
                // Fallback if the filename in Firestore doesn't match a drawable
                imageView.setImageResource(R.drawable.pawprint)
            }
        }
    }

    override fun getItemCount(): Int = cats.size

    fun updateData(newCats: List<Cat>) {
        cats = newCats
        notifyDataSetChanged()
    }
}