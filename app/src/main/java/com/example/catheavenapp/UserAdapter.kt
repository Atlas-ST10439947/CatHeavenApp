package com.example.catheavenapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catheavenapp.databinding.ItemAdminUserBinding
import com.example.catheavenapp.domain.model.User
import com.example.catheavenapp.domain.model.UserRole
import com.example.catheavenapp.ui.booking.formatDateMillis

class UserAdapter(private var users: List<User>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemAdminUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        with(holder.binding) {
            userRoleText.text = if (user.role == UserRole.ADMIN) "ADMIN" else "SUPPORTER"
            userNameText.text = user.fullName.ifBlank { "(no name)" }
            userEmailText.text = user.email
            userDetailText.text = listOfNotNull(
                user.phone.ifBlank { null },
                if (user.dateJoinedMillis > 0) "Joined ${formatDateMillis(user.dateJoinedMillis)}" else null
            ).joinToString("  ·  ")
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
