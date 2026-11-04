package com.example.catheavenapp.ui.booking

import com.example.catheavenapp.util.Diagnostics
import com.example.catheavenapp.util.FailureMessages
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.booking.BookingChangeException
import com.example.catheavenapp.data.booking.BookingRepository
import com.example.catheavenapp.data.booking.FirestoreBookingRepository
import com.example.catheavenapp.data.booking.NotSignedInException
import com.example.catheavenapp.domain.booking.BookingHistory
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.Timeslot
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface MyBookingsState {
    object Loading : MyBookingsState
    data class Loaded(val history: BookingHistory) : MyBookingsState
    data class Failed(val message: String) : MyBookingsState
}

/** Result of a cancel or reschedule, shown once as a message. */
sealed interface BookingActionState {
    object Idle : BookingActionState
    object Working : BookingActionState
    data class Done(val message: String) : BookingActionState
    data class Failed(val message: String) : BookingActionState
}

/** Loads the signed-in supporter's bookings for the My Bookings screen, and changes them. */
class MyBookingsViewModel(
    private val repository: BookingRepository = FirestoreBookingRepository()
) : ViewModel() {

    private val _state = MutableLiveData<MyBookingsState>(MyBookingsState.Loading)
    val state: LiveData<MyBookingsState> = _state

    private val _actionState = MutableLiveData<BookingActionState>(BookingActionState.Idle)
    val actionState: LiveData<BookingActionState> = _actionState

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

    /** Slots on [date] that can take this booking's guests, for the reschedule picker. */
    suspend fun slotsThatFit(date: VisitDate, booking: Booking): List<Timeslot> =
        repository.timeslotsOn(date).filter {
            it.id != booking.timeslotId && it.isBookable && it.remainingCapacity >= booking.numberOfGuests
        }

    fun cancel(booking: Booking) = act(
        "Your ${formatDateMillis(booking.slotDateMillis)} visit has been cancelled", "cancel your booking"
    ) {
        repository.cancelMyBooking(booking.id)
    }

    fun reschedule(booking: Booking, newSlot: Timeslot) = act(
        "Moved to ${formatDateMillis(newSlot.dateMillis)} at ${formatSlotTime(newSlot.startTime)}",
        "move your booking"
    ) {
        repository.rescheduleMyBooking(booking.id, newSlot.id)
    }

    fun onActionMessageShown() {
        if (_actionState.value is BookingActionState.Done || _actionState.value is BookingActionState.Failed) {
            _actionState.value = BookingActionState.Idle
        }
    }

    private fun act(successMessage: String, what: String, action: suspend () -> Unit) {
        if (_actionState.value == BookingActionState.Working) return
        _actionState.value = BookingActionState.Working
        viewModelScope.launch {
            _actionState.value = try {
                action()
                BookingActionState.Done(successMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BookingChangeException) {
                BookingActionState.Failed(e.rejection.message)
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Changing booking failed", e)
                BookingActionState.Failed(FailureMessages.forSupporter(e, what))
            }
            load()
        }
    }

    private companion object {
        const val TAG = "MyBookings"
    }
}
