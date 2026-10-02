package com.example.catheavenapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catheavenapp.databinding.FragmentUpcomingBookingBinding

data class BookingItem(
    val status: String,
    val date: String,
    val price: String,
    val timeDuration: String,
    val guests: String
)

object DummyData {
    val upcomingBookings = listOf(
        BookingItem("UPCOMING", "28 August 2026", "R30", "11:00 AM | 1 Hour", "1 Guest")
    )
    val pastBookings = listOf(
        BookingItem("PAST", "15 July 2026", "R60", "2:00 PM | 2 Hours", "2 Guests")
    )
}

class BookingAdapter(private var bookings: List<BookingItem>) :
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(val binding: FragmentUpcomingBookingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = FragmentUpcomingBookingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val item = bookings[position]
        with(holder.binding) {
            statusText.text = item.status
            dateText.text = item.date
            priceText.text = item.price
            timeText.text = item.timeDuration
            guestText.text = item.guests
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateData(newBookings: List<BookingItem>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}