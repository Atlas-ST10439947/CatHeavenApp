package com.example.catheavenapp.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.catheavenapp.domain.booking.BookingDraft
import com.example.catheavenapp.domain.booking.DefaultPricing
import com.example.catheavenapp.domain.booking.PriceQuote
import com.example.catheavenapp.domain.booking.PricingCalculator
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.PricingTier

/**
 * Shared state for the booking flow (date -> time -> details -> confirm).
 *
 * Scoped to MainActivity, so every booking screen reads and writes the same
 * draft and the user's choices survive moving between screens and rotation.
 * The business rules live in the pure domain classes; this class only holds state.
 */
class BookingFlowViewModel : ViewModel() {

    private val _draft = MutableLiveData(BookingDraft())
    val draft: LiveData<BookingDraft> = _draft

    // Task 1 defaults until the live tiers are loaded from Firestore.
    private val _pricingTiers = MutableLiveData(DefaultPricing.TIERS)
    val pricingTiers: LiveData<List<PricingTier>> = _pricingTiers

    private val current: BookingDraft
        get() = _draft.value ?: BookingDraft()

    private fun update(change: (BookingDraft) -> BookingDraft) {
        _draft.value = change(current)
    }

    fun selectDate(date: VisitDate) = update { it.withDate(date) }

    fun selectDuration(hours: Double) = update { it.withDuration(hours) }

    fun selectStartTime(time: String) = update { it.withStartTime(time) }

    fun addGuest() = update { it.addGuest() }

    fun removeGuest() = update { it.removeGuest() }

    fun updateContact(fullName: String, email: String, phone: String) =
        update { it.withContact(fullName, email, phone) }

    fun quoteFor(draft: BookingDraft): PriceQuote? =
        PricingCalculator.quote(_pricingTiers.value.orEmpty(), draft.durationHours, draft.guests)

    /** Clears the flow once a booking is finished, so the next one starts fresh. */
    fun reset() {
        _draft.value = BookingDraft()
    }
}
