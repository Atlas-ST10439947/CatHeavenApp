package com.example.catheavenapp.ui.admin

import com.example.catheavenapp.util.Diagnostics
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.admin.FirestoreUserRepository
import com.example.catheavenapp.data.admin.UserRepository
import com.example.catheavenapp.domain.model.User
import com.example.catheavenapp.domain.model.UserRole
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface AdminUsersState {
    object Loading : AdminUsersState
    data class Loaded(val users: List<User>) : AdminUsersState
    data class Failed(val message: String) : AdminUsersState
}

class AdminUsersViewModel(
    private val repository: UserRepository = FirestoreUserRepository()
) : ViewModel() {

    private val _state = MutableLiveData<AdminUsersState>(AdminUsersState.Loading)
    val state: LiveData<AdminUsersState> = _state

    /** A one-off message for a Snackbar; the screen clears it once shown. */
    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message

    fun messageShown() {
        _message.value = null
    }

    /** Blocks or unblocks [user], then updates that card in the list. */
    fun setBlocked(user: User, blocked: Boolean) {
        if (user.role == UserRole.ADMIN) return   // the rules refuse this too
        viewModelScope.launch {
            try {
                repository.setBlocked(user.uid, blocked)
                val current = _state.value
                if (current is AdminUsersState.Loaded) {
                    _state.value = AdminUsersState.Loaded(
                        current.users.map { if (it.uid == user.uid) it.copy(blocked = blocked) else it }
                    )
                }
                val name = user.fullName.ifBlank { user.email }
                _message.value = if (blocked) "$name is blocked" else "$name is unblocked"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn("AdminUsers", "Changing blocked status failed", e)
                _message.value = "We couldn't update this account. Check your connection and try again."
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = try {
                AdminUsersState.Loaded(repository.allUsers())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn("AdminUsers", "Loading users failed", e)
                AdminUsersState.Failed(
                    "We couldn't load users. Check your connection and that this account is an admin."
                )
            }
        }
    }
}
