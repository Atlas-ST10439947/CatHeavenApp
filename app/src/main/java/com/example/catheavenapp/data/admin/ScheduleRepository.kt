package com.example.catheavenapp.data.admin

import com.example.catheavenapp.domain.booking.SlotWrite
import com.example.catheavenapp.domain.model.Timeslot
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

interface ScheduleRepository {
    /** Applies all writes for a day together: either every slot is saved, or none are. */
    suspend fun apply(writes: List<SlotWrite>)
}

class FirestoreScheduleRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ScheduleRepository {

    override suspend fun apply(writes: List<SlotWrite>) {
        val batch = db.batch()
        val timeslots = db.collection("timeslots")
        writes.forEach { write ->
            when (write) {
                is SlotWrite.Create ->
                    batch.set(timeslots.document(write.slot.id), write.slot.toFirestore())
                is SlotWrite.Update ->
                    batch.update(
                        timeslots.document(write.id),
                        mapOf("capacity" to write.capacity, "status" to write.status.value)
                    )
            }
        }
        batch.commit().await()
    }
}

private fun Timeslot.toFirestore(): Map<String, Any> = mapOf(
    "date" to Timestamp(Date(dateMillis)),
    "startTime" to startTime,
    "endTime" to endTime,
    "slotType" to slotType.value,
    "capacity" to capacity,
    "bookedCount" to bookedCount,
    "status" to status.value
)
