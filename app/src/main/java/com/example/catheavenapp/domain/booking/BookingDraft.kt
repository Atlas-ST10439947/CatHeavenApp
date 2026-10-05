package com.example.catheavenapp.domain.booking

import java.util.Calendar
import java.util.TimeZone

/**
 * A calendar day chosen in the booking flow, independent of time zone.
 * [month] is 1-12 (unlike java.util.Calendar, which uses 0-11).
 */
data class VisitDate(val year: Int, val month: Int, val day: Int) : Comparable<VisitDate> {

    init {
        require(month in 1..12) { "month must be 1-12" }
        require(day in 1..31) { "day must be 1-31" }
    }

    /** Midnight at the start of this day, in [timeZone]. */
    fun toEpochMillis(timeZone: TimeZone = TimeZone.getDefault()): Long {
        val calendar = Calendar.getInstance(timeZone)
        calendar.clear()
        calendar.set(year, month - 1, day)
        return calendar.timeInMillis
    }

    override fun compareTo(other: VisitDate): Int =
        compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    companion object {
        fun fromEpochMillis(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): VisitDate {
            val calendar = Calendar.getInstance(timeZone)
            calendar.timeInMillis = millis
            return VisitDate(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun today(timeZone: TimeZone = TimeZone.getDefault()): VisitDate =
            fromEpochMillis(System.currentTimeMillis(), timeZone)
    }
}

/**
 * Everything the supporter has chosen so far in the booking flow.
 * Immutable: every change returns a new draft, which keeps the flow
 * predictable and easy to test.
 */
data class BookingDraft(
    val date: VisitDate? = null,
    val durationHours: Double = 1.0,
    val startTime: String? = null,   // 24-hour "HH:mm", matching timeslots.startTime
    val guests: Int = 1,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val timeslotId: String? = null   // the timeslots/{id} document being booked
) {
    val hasDate: Boolean get() = date != null
    val hasTime: Boolean get() = date != null && startTime != null

    /** True once a real timeslot document has been chosen, so the booking can be written. */
    val hasTimeslot: Boolean get() = hasTime && timeslotId != null

    /** Changing the day clears the chosen slot, because availability differs per day. */
    fun withDate(newDate: VisitDate): BookingDraft =
        if (newDate == date) this else copy(date = newDate, startTime = null, timeslotId = null)

    fun withDuration(hours: Double): BookingDraft = copy(durationHours = hours)

    fun withStartTime(time: String): BookingDraft = copy(startTime = time)

    /** Selects a specific timeslot document and remembers its start time for display. */
    fun withTimeslot(id: String, startTime: String): BookingDraft =
        copy(timeslotId = id, startTime = startTime)

    fun addGuest(max: Int = BookingValidator.MAX_GUESTS): BookingDraft =
        if (guests < max) copy(guests = guests + 1) else this

    fun removeGuest(): BookingDraft =
        if (guests > 1) copy(guests = guests - 1) else this

    fun withContact(fullName: String, email: String, phone: String): BookingDraft =
        copy(fullName = fullName.trim(), email = email.trim(), phone = phone.trim())
}