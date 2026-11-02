package com.example.catheavenapp.ui.booking

import com.example.catheavenapp.util.Diagnostics
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.booking.BookingRepository
import com.example.catheavenapp.data.booking.FirestoreBookingRepository
import com.example.catheavenapp.data.booking.NotSignedInException
import com.example.catheavenapp.domain.booking.BookingHistory
import com.example.catheavenapp.domain.booking.VisitDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface MyBookingsState {
    object Loading : MyBookingsState
    data class Loaded(val history: BookingHistory) : MyBookingsState
    data class Failed(val message: String) : MyBookingsState
}

/** Loads the signed-in supporter's bookings for the My Bookings screen. */
class MyBookingsViewModel(
    private val repository: BookingRepository = FirestoreBookingRepository()
) : ViewModel() {

    private val _state = MutableLiveData<MyBookingsState>(MyBookingsState.Loading)
    val state: LiveData<MyBookingsState> = _state
    private var loadJob: Job? = null

    /** Called each time the screen opens, so a booking made a moment ago shows up. */
    fun load() {
        loadJob?.cancel()
        // Keep showing the current list while refreshing, instead of flashing "Loading"
        if (_state.value !is MyBookingsState.Loaded) _state.value = MyBookingsState.Loading
        loadJob = viewModelScope.launch {
            _state.value = try {
                MyBookingsState.Loaded(BookingHistory.from(repository.myBookings(), VisitDate.today()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotSignedInException) {
                MyBookingsState.Failed("Please sign in to see your bookings.")
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Loading bookings failed", e)
                MyBookingsState.Failed("We couldn't load your bookings. Check your connection and try again.")
            }
        }
    }

    private companion object {
        const val TAG = "MyBookings"
    }
}
