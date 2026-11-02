package com.example.catheavenapp.ui.admin

import com.example.catheavenapp.util.Diagnostics
import com.example.catheavenapp.util.FailureMessages
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.admin.FirestoreLedgerRepository
import com.example.catheavenapp.data.admin.LedgerRepository
import com.example.catheavenapp.domain.booking.BookingLedger
import com.example.catheavenapp.domain.booking.DayLedger
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.Booking
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface LedgerState {
    object Loading : LedgerState
    data class Loaded(val date: VisitDate, val ledger: DayLedger) : LedgerState
    data class Failed(val message: String) : LedgerState
}

sealed interface LedgerActionState {
    object Idle : LedgerActionState
    object Working : LedgerActionState
    data class Done(val message: String) : LedgerActionState
    data class Failed(val message: String) : LedgerActionState
}

/** Backs the admin Booking Ledger: one day's bookings, with cancellation. */
class AdminBookingsViewModel(
    private val repository: LedgerRepository = FirestoreLedgerRepository()
) : ViewModel() {

    private val _date = MutableLiveData(VisitDate.today())
    val date: LiveData<VisitDate> = _date

    private val _state = MutableLiveData<LedgerState>(LedgerState.Loading)
    val state: LiveData<LedgerState> = _state

    private val _actionState = MutableLiveData<LedgerActionState>(LedgerActionState.Idle)
    val actionState: LiveData<LedgerActionState> = _actionState

    private var loadJob: Job? = null

    fun selectDate(date: VisitDate) {
        _date.value = date
        load()
    }

    fun load() {
        val date = _date.value ?: VisitDate.today()
        loadJob?.cancel()
        _state.value = LedgerState.Loading
        loadJob = viewModelScope.launch {
            _state.value = try {
                LedgerState.Loaded(date, BookingLedger.forDay(repository.bookingsOn(date)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Loading ledger failed", e)
                LedgerState.Failed("We couldn't load bookings. Check your connection and that this account is an admin.")
            }
        }
    }

    fun cancel(booking: Booking) {
        if (_actionState.value == LedgerActionState.Working) return
        _actionState.value = LedgerActionState.Working
        viewModelScope.launch {
            _actionState.value = try {
                repository.cancel(booking.id)
                LedgerActionState.Done("Booking ${booking.bookingReference} cancelled and its seats released")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Cancelling booking failed", e)
                LedgerActionState.Failed(FailureMessages.forAdmin(e, "cancel that booking"))
            }
            load()
        }
    }

    fun onActionMessageShown() {
        if (_actionState.value is LedgerActionState.Done || _actionState.value is LedgerActionState.Failed) {
            _actionState.value = LedgerActionState.Idle
        }
    }

    private companion object {
        const val TAG = "AdminLedger"
    }
}
