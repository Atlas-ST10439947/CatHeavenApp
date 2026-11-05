package com.example.catheavenapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.catheavenapp.databinding.ItemAdminUserBinding
import com.example.catheavenapp.domain.model.User
import com.example.catheavenapp.domain.model.UserRole
import com.example.catheavenapp.ui.booking.formatDateMillis

/** [onToggleBlock] is called when the admin taps Block account or Unblock on a supporter. */
class UserAdapter(
    private var users: List<User>,
    private val onToggleBlock: (User) -> Unit
) :
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
            val context = root.context
            val isAdmin = user.role == UserRole.ADMIN
            userRoleText.text = when {
                isAdmin -> "ADMIN"
                user.blocked -> "SUPPORTER  ·  BLOCKED"
                else -> "SUPPORTER"
            }
            userRoleText.setTextColor(
                ContextCompat.getColor(context, if (user.blocked) R.color.errorRed else R.color.pink)
            )
            userNameText.text = user.fullName.ifBlank { "(no name)" }
            userEmailText.text = user.email
            userDetailText.text = listOfNotNull(
                user.phone.ifBlank { null },
                if (user.dateJoinedMillis > 0) "Joined ${formatDateMillis(user.dateJoinedMillis)}" else null
            ).joinToString("  ·  ")

            userBlockButton.visibility = if (isAdmin) View.GONE else View.VISIBLE
            userBlockButton.text = if (user.blocked) "Unblock" else "Block account"
            userBlockButton.setTextColor(
                ContextCompat.getColor(context, if (user.blocked) R.color.pink else R.color.errorRed)
            )
            val name = user.fullName.ifBlank { user.email }
            userBlockButton.contentDescription =
                if (user.blocked) "Unblock $name" else "Block $name's account"
            userBlockButton.setOnClickListener { onToggleBlock(user) }
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
