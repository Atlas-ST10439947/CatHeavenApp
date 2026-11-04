package com.example.catheavenapp.ui.admin

import com.example.catheavenapp.util.Diagnostics
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.admin.FirestoreUserRepository
import com.example.catheavenapp.data.admin.UserRepository
import com.example.catheavenapp.domain.model.User
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
