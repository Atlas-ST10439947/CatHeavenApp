package com.example.catheavenapp.domain.booking

import java.util.Calendar
import java.util.TimeZone

/**
 * The café's normal opening pattern, used to open several weeks of bookings in one go
 * so supporters always find times to book or reschedule to.
 * Kept free of Firebase so it can be unit tested.
 */
object DefaultSchedule {

    /** Guests per hourly slot when days are opened in bulk. Admins can still change a single day. */
    const val DEFAULT_CAPACITY = 30

    const val DEFAULT_WEEKS = 4

    /** Cat Heaven opens Tuesday to Saturday (see the opening hours on the Home screen). */
    private val OPEN_DAYS = setOf(
        Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    private val UTC = TimeZone.getTimeZone("UTC")

    fun isOpeningDay(date: VisitDate): Boolean = calendarFor(date).get(Calendar.DAY_OF_WEEK) in OPEN_DAYS

    /** Opening days from [from] (included) for the next [days] calendar days, in order. */
    fun openingDays(from: VisitDate, days: Int): List<VisitDate> = (0 until days).mapNotNull { offset ->
        val calendar = calendarFor(from).apply { add(Calendar.DAY_OF_MONTH, offset) }
        val date = VisitDate(
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
        )
        date.takeIf { calendar.get(Calendar.DAY_OF_WEEK) in OPEN_DAYS }
    }

    // UTC avoids daylight-saving surprises when stepping a day at a time
    private fun calendarFor(date: VisitDate): Calendar = Calendar.getInstance(UTC).apply {
        clear()
        set(date.year, date.month - 1, date.day)
    }
}
