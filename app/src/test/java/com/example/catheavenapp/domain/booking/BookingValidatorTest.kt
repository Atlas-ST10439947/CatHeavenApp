package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.BookingRequest
import com.example.catheavenapp.domain.model.PricingTier
import com.example.catheavenapp.domain.model.SlotType
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingValidatorTest {

    private val validator = BookingValidator()

    private val tiers = listOf(
        PricingTier("tier-1h", durationHours = 1.0, pricePerPerson = 120, isActive = true),
        PricingTier("tier-1.5h", durationHours = 1.5, pricePerPerson = 170, isActive = true),
        PricingTier("tier-2h", durationHours = 2.0, pricePerPerson = 220, isActive = true)
    )

    private fun slot(
        capacity: Int = 10,
        bookedCount: Int = 0,
        status: TimeslotStatus = TimeslotStatus.OPEN
    ) = Timeslot(
        id = "slot-1",
        dateMillis = 0L,
        startTime = "10:00",
        endTime = "11:00",
        slotType = SlotType.GENERAL_VISIT,
        capacity = capacity,
        bookedCount = bookedCount,
        status = status
    )

    private fun request(guests: Int = 2, hours: Double = 1.0, notes: String = "") =
        BookingRequest("slot-1", guests, hours, notes)

    @Test
    fun validRequest_returnsPriceAndTotal() {
        val result = validator.validate(request(guests = 2, hours = 1.0), slot(), tiers)
        assertEquals(ValidationResult.Valid(pricePerPerson = 120, totalDue = 240), result)
    }

    @Test
    fun halfHourTier_usesItsOwnPrice() {
        val result = validator.validate(request(guests = 2, hours = 1.5), slot(), tiers)
        assertEquals(ValidationResult.Valid(pricePerPerson = 170, totalDue = 340), result)
    }

    @Test
    fun longerVisit_usesMatchingTierPrice() {
        val result = validator.validate(request(guests = 3, hours = 2.0), slot(), tiers)
        assertEquals(ValidationResult.Valid(pricePerPerson = 220, totalDue = 660), result)
    }

    @Test
    fun zeroGuests_isRejected() {
        val result = validator.validate(request(guests = 0), slot(), tiers)
        assertEquals(ValidationResult.Invalid(BookingError.InvalidGuestCount(0, 8)), result)
    }

    @Test
    fun nineGuests_isRejected() {
        val result = validator.validate(request(guests = 9), slot(), tiers)
        assertEquals(ValidationResult.Invalid(BookingError.InvalidGuestCount(9, 8)), result)
    }

    @Test
    fun eightGuests_isAccepted() {
        val result = validator.validate(request(guests = 8), slot(), tiers)
        assertEquals(ValidationResult.Valid(pricePerPerson = 120, totalDue = 960), result)
    }

    @Test
    fun closedSlot_isRejected() {
        val result = validator.validate(request(), slot(status = TimeslotStatus.CLOSED), tiers)
        assertEquals(ValidationResult.Invalid(BookingError.SlotClosed), result)
    }

    @Test
    fun fullSlot_isRejected() {
        val result = validator.validate(request(), slot(status = TimeslotStatus.FULL), tiers)
        assertEquals(ValidationResult.Invalid(BookingError.SlotFull), result)
    }

    @Test
    fun openSlotWithNoSeatsLeft_isTreatedAsFull() {
        val result = validator.validate(request(), slot(capacity = 10, bookedCount = 10), tiers)
        assertEquals(ValidationResult.Invalid(BookingError.SlotFull), result)
    }

    @Test
    fun moreGuestsThanSeatsLeft_reportsRemainingSeats() {
        val result = validator.validate(request(guests = 4), slot(capacity = 10, bookedCount = 7), tiers)
        assertEquals(ValidationResult.Invalid(BookingError.NotEnoughSpace(4, 3)), result)
    }

    @Test
    fun guestsExactlyFillingRemainingSeats_isAccepted() {
        val result = validator.validate(request(guests = 3), slot(capacity = 10, bookedCount = 7), tiers)
        assertEquals(ValidationResult.Valid(pricePerPerson = 120, totalDue = 360), result)
    }

    @Test
    fun durationWithNoPricingTier_isRejected() {
        val result = validator.validate(request(hours = 3.0), slot(), tiers)
        assertEquals(ValidationResult.Invalid(BookingError.UnknownDuration(3.0)), result)
    }

    @Test
    fun inactivePricingTier_isRejected() {
        val tiersWith2hOff = tiers.map { if (it.durationHours == 2.0) it.copy(isActive = false) else it }
        val result = validator.validate(request(hours = 2.0), slot(), tiersWith2hOff)
        assertEquals(ValidationResult.Invalid(BookingError.UnknownDuration(2.0)), result)
    }

    @Test
    fun overlongNotes_areRejected() {
        val notes = "a".repeat(501)
        val result = validator.validate(request(notes = notes), slot(), tiers)
        assertEquals(ValidationResult.Invalid(BookingError.NotesTooLong(501, 500)), result)
    }
}
