package com.example.catheavenapp.ui.booking

import com.example.catheavenapp.util.Diagnostics
import com.example.catheavenapp.util.FailureMessages
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.booking.BookingRejectedException
import com.example.catheavenapp.data.booking.BookingRepository
import com.example.catheavenapp.data.booking.FirestoreBookingRepository
import com.example.catheavenapp.data.booking.NotSignedInException
import com.example.catheavenapp.domain.booking.BookingDraft
import com.example.catheavenapp.domain.booking.DefaultPricing
import com.example.catheavenapp.domain.booking.PriceQuote
import com.example.catheavenapp.domain.booking.PricingCalculator
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.PricingTier
import com.example.catheavenapp.domain.model.Timeslot
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface TimeslotsState {
    object Loading : TimeslotsState
    data class Loaded(val date: VisitDate, val slots: List<Timeslot>) : TimeslotsState
    object Failed : TimeslotsState
}

sealed interface SubmitState {
    object Idle : SubmitState
    object Submitting : SubmitState
    object Succeeded : SubmitState
    data class Failed(val message: String) : SubmitState
}

/**
 * Holds the booking flow's state across the four booking screens.
 * The business rules live in the pure domain classes; Firestore access
 * lives in [BookingRepository].
 */
class BookingFlowViewModel(
    private val repository: BookingRepository = FirestoreBookingRepository()
) : ViewModel() {

    private val _draft = MutableLiveData(BookingDraft())
    val draft: LiveData<BookingDraft> = _draft

    // Task 1 defaults until the live tiers are loaded from Firestore.
    private val _pricingTiers = MutableLiveData(DefaultPricing.TIERS)
    val pricingTiers: LiveData<List<PricingTier>> = _pricingTiers

    private val _timeslots = MutableLiveData<TimeslotsState>(TimeslotsState.Loading)
    val timeslots: LiveData<TimeslotsState> = _timeslots
    private var timeslotsJob: Job? = null

    private val _submitState = MutableLiveData<SubmitState>(SubmitState.Idle)
    val submitState: LiveData<SubmitState> = _submitState

    private val _confirmedBooking = MutableLiveData<Booking?>(null)
    val confirmedBooking: LiveData<Booking?> = _confirmedBooking

    private val current: BookingDraft
        get() = _draft.value ?: BookingDraft()

    private fun update(change: (BookingDraft) -> BookingDraft) {
        _draft.value = change(current)
    }

    fun selectDate(date: VisitDate) = update { it.withDate(date) }

    fun selectDuration(hours: Double) = update { it.withDuration(hours) }

    fun selectTimeslot(slot: Timeslot) = update { it.withTimeslot(slot.id, slot.startTime) }

    fun addGuest() = update { it.addGuest() }

    fun removeGuest() = update { it.removeGuest() }

    fun updateContact(fullName: String, email: String, phone: String) =
        update { it.withContact(fullName, email, phone) }

    fun quoteFor(draft: BookingDraft): PriceQuote? =
        PricingCalculator.quote(_pricingTiers.value.orEmpty(), draft.durationHours, draft.guests)

    /** Loads the slots for the chosen date. Called each time the time screen opens, so counts are fresh. */
    fun loadTimeslots() {
        val date = current.date ?: return
        timeslotsJob?.cancel()
        _timeslots.value = TimeslotsState.Loading
        timeslotsJob = viewModelScope.launch {
            _timeslots.value = try {
                TimeslotsState.Loaded(date, repository.timeslotsOn(date))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Loading timeslots failed", e)
                TimeslotsState.Failed
            }
        }
    }

    /** Writes the booking in a Firestore transaction. Ignores repeat taps while one is in progress. */
    fun submitBooking() {
        if (_submitState.value == SubmitState.Submitting) return
        val draft = current
        _submitState.value = SubmitState.Submitting
        viewModelScope.launch {
            _submitState.value = try {
                _confirmedBooking.value = repository.createBooking(draft, quoteFor(draft))
                SubmitState.Succeeded
            } catch (e: CancellationException) {
                throw e
            } catch (e: BookingRejectedException) {
                SubmitState.Failed(e.rejection.message)
            } catch (e: NotSignedInException) {
                SubmitState.Failed(e.message ?: "Please sign in to book a visit.")
            } catch (e: Exception) {
                // e.g. no connection, or PERMISSION_DENIED if the security rules reject the write
                Diagnostics.warn(TAG, "Booking failed", e)
                SubmitState.Failed(FailureMessages.forSupporter(e, "save your booking"))
            }
        }
    }

    /** Called once the confirmation screen is showing, so the details screen doesn't navigate again. */
    fun onConfirmationShown() {
        if (_submitState.value == SubmitState.Succeeded) _submitState.value = SubmitState.Idle
    }

    /** Called once an error has been shown, so it isn't shown again when the screen is recreated. */
    fun onSubmitErrorShown() {
        if (_submitState.value is SubmitState.Failed) _submitState.value = SubmitState.Idle
    }

    /** Clears the flow once a booking is finished, so the next one starts fresh. */
    fun reset() {
        timeslotsJob?.cancel()
        _draft.value = BookingDraft()
        _timeslots.value = TimeslotsState.Loading
        _submitState.value = SubmitState.Idle
        _confirmedBooking.value = null
    }

    private companion object {
        const val TAG = "BookingFlow"
    }
}