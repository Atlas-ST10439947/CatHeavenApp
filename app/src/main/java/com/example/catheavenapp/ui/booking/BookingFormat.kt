package com.example.catheavenapp.ui.booking

import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.booking.formatHours
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** e.g. "28 August 2026", matching the existing booking screens. */
fun formatVisitDate(date: VisitDate): String = formatDateMillis(date.toEpochMillis())

/** e.g. "28 August 2026", for dates stored as epoch millis (such as Booking.slotDateMillis). */
fun formatDateMillis(millis: Long): String =
    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(millis))

/** "1 Guest" / "3 Guests". */
fun formatGuests(guests: Int): String =
    if (guests == 1) "1 Guest" else "$guests Guests"

/** "1 Person" / "3 People", as used on the confirmation ticket. */
fun formatPeople(guests: Int): String =
    if (guests == 1) "1 Person" else "$guests People"

/** "1 Hour" / "1.5 Hours" / "2 Hours". */
fun formatDuration(hours: Double): String =
    "${formatHours(hours)} ${if (hours == 1.0) "Hour" else "Hours"}"

/** "14:00" -> "2:00 PM". Returns the input unchanged if it isn't "HH:mm". */
fun formatSlotTime(hhmm: String): String {
    val parts = hhmm.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return hhmm
    val suffix = if (hour < 12) "AM" else "PM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, suffix)
}