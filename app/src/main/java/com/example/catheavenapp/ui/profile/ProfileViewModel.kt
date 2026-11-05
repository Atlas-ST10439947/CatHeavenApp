package com.example.catheavenapp.ui.profile

import com.example.catheavenapp.util.Diagnostics
import com.example.catheavenapp.util.FailureMessages
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.booking.NotSignedInException
import com.example.catheavenapp.data.profile.FirestoreProfileRepository
import com.example.catheavenapp.data.profile.ProfileRepository
import com.example.catheavenapp.domain.profile.Profile
import com.example.catheavenapp.domain.profile.ProfileCheck
import com.example.catheavenapp.domain.profile.ProfileValidator
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface ProfileState {
    object Loading : ProfileState
    data class Loaded(val profile: Profile) : ProfileState
    data class Failed(val message: String) : ProfileState
}

sealed interface ProfileSaveState {
    object Idle : ProfileSaveState
    object Saving : ProfileSaveState
    data class Saved(val message: String) : ProfileSaveState
    data class Failed(val message: String) : ProfileSaveState
}

/** Backs the Profile and Notifications settings screens. */
class ProfileViewModel(
    private val repository: ProfileRepository = FirestoreProfileRepository()
) : ViewModel() {

    private val _state = MutableLiveData<ProfileState>(ProfileState.Loading)
    val state: LiveData<ProfileState> = _state

    private val _saveState = MutableLiveData<ProfileSaveState>(ProfileSaveState.Idle)
    val saveState: LiveData<ProfileSaveState> = _saveState

    fun load() {
        viewModelScope.launch {
            _state.value = try {
                ProfileState.Loaded(repository.myProfile())
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotSignedInException) {
                ProfileState.Failed("Please sign in again to edit your profile.")
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Loading profile failed", e)
                ProfileState.Failed("We couldn't load your profile. Check your connection and try again.")
            }
        }
    }

    /**
     * Validates and saves the details. Returns the validation result so the
     * screen can put an error on the right field; only saves when it is Valid.
     */
    fun saveDetails(fullName: String, phone: String, location: String): ProfileCheck {
        val check = ProfileValidator.check(fullName, phone, location)
        if (check !is ProfileCheck.Valid || _saveState.value == ProfileSaveState.Saving) return check

        _saveState.value = ProfileSaveState.Saving
        viewModelScope.launch {
            _saveState.value = try {
                repository.updateDetails(check.details)
                updateLoaded {
                    it.copy(
                        fullName = check.details.fullName,
                        phone = check.details.phone,
                        location = check.details.location
                    )
                }
                ProfileSaveState.Saved("Profile updated")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Saving profile failed", e)
                ProfileSaveState.Failed(FailureMessages.forSupporter(e, "save your profile"))
            }
        }
        return check
    }

    fun onSaveMessageShown() {
        if (_saveState.value is ProfileSaveState.Saved || _saveState.value is ProfileSaveState.Failed) {
            _saveState.value = ProfileSaveState.Idle
        }
    }

    private fun updateLoaded(change: (Profile) -> Profile) {
        val loaded = _state.value as? ProfileState.Loaded ?: return
        _state.value = ProfileState.Loaded(change(loaded.profile))
    }

    private companion object {
        const val TAG = "Profile"
    }
}
