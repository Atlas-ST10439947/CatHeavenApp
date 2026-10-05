package com.example.catheavenapp.ui.booking

import com.example.catheavenapp.domain.booking.VisitDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** e.g. "28 August 2026", matching the existing booking screens. */
fun formatVisitDate(date: VisitDate): String =
    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(date.toEpochMillis()))

/** "1 Guest" / "3 Guests". */
fun formatGuests(guests: Int): String =
    if (guests == 1) "1 Guest" else "$guests Guests"
