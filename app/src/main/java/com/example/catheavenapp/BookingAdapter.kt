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
    val guests: String,
    val id: String = ""   // the booking document, so a tap can act on it
)

/** Booking cards for My Bookings and the admin Booking Ledger. [onClick] is optional. */
class BookingAdapter(
    private var bookings: List<BookingItem>,
    private val onClick: ((BookingItem) -> Unit)? = null
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(val binding: FragmentUpcomingBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            if (onClick != null) {
                binding.root.isClickable = true
                binding.root.isFocusable = true
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) onClick?.invoke(bookings[position])
                }
            }
        }
    }

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
            // Read the whole card as one sentence for screen readers
            root.contentDescription = "${item.status}, ${item.date}, ${item.timeDuration}, ${item.guests}, ${item.price}"
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateData(newBookings: List<BookingItem>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}