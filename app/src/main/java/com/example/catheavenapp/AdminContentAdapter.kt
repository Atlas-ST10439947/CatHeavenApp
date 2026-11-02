package com.example.catheavenapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catheavenapp.databinding.ItemAdminContentBinding
import com.example.catheavenapp.domain.model.CatDescriptions
import com.example.catheavenapp.ui.admin.ContentRow

/** Rows on the admin Content screen, each with Edit and Delete actions. */
class AdminContentAdapter(
    private val onEdit: (ContentRow) -> Unit,
    private val onDelete: (ContentRow) -> Unit
) : RecyclerView.Adapter<AdminContentAdapter.RowViewHolder>() {

    private var rows: List<ContentRow> = emptyList()

    inner class RowViewHolder(val binding: ItemAdminContentBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnEdit.setOnClickListener { rowAt(bindingAdapterPosition)?.let(onEdit) }
            binding.btnDelete.setOnClickListener { rowAt(bindingAdapterPosition)?.let(onDelete) }
        }
    }

    private fun rowAt(position: Int): ContentRow? =
        if (position == RecyclerView.NO_POSITION) null else rows.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder =
        RowViewHolder(ItemAdminContentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val (title, subtitle) = when (val row = rows[position]) {
            is ContentRow.CatRow -> row.cat.name to listOfNotNull(
                CatDescriptions.statusLabel(row.cat.adoptionStatus),
                row.cat.breed.ifBlank { null }
            ).joinToString("  ·  ")
            is ContentRow.MenuRow -> row.item.name to listOfNotNull(
                row.item.category.label,
                row.item.price?.let { "R$it" },
                if (row.item.isAvailable) null else "Hidden"
            ).joinToString("  ·  ")
        }
        with(holder.binding) {
            contentTitle.text = title
            contentSubtitle.text = subtitle
            // Screen readers announce which item each button acts on
            btnEdit.contentDescription = "Edit $title"
            btnDelete.contentDescription = "Delete $title"
        }
    }

    override fun getItemCount(): Int = rows.size

    fun updateData(newRows: List<ContentRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
