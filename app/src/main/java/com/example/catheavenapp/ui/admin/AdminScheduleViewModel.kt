package com.example.catheavenapp.ui.admin

import com.example.catheavenapp.util.Diagnostics
import com.example.catheavenapp.util.FailureMessages
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catheavenapp.data.admin.FirestoreScheduleRepository
import com.example.catheavenapp.data.admin.ScheduleRepository
import com.example.catheavenapp.data.booking.BookingRepository
import com.example.catheavenapp.data.booking.FirestoreBookingRepository
import com.example.catheavenapp.domain.booking.DayPlan
import com.example.catheavenapp.domain.booking.DaySchedule
import com.example.catheavenapp.domain.booking.DefaultSchedule
import com.example.catheavenapp.domain.booking.SlotWrite
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.Timeslot
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface ScheduleDayState {
    object Loading : ScheduleDayState
    data class Loaded(val date: VisitDate, val slots: List<Timeslot>) : ScheduleDayState
    data class Failed(val message: String) : ScheduleDayState
}

sealed interface ScheduleSaveState {
    object Idle : ScheduleSaveState
    object Saving : ScheduleSaveState
    data class Saved(val slotCount: Int, val capacity: Int) : ScheduleSaveState
    /** Bulk "open the next few weeks": how many days and slots were created. */
    data class OpenedDefaults(val days: Int, val slots: Int, val weeks: Int) : ScheduleSaveState
    data class Failed(val message: String) : ScheduleSaveState
}

class AdminScheduleViewModel(
    private val bookings: BookingRepository = FirestoreBookingRepository(),
    private val schedule: ScheduleRepository = FirestoreScheduleRepository()
) : ViewModel() {

    private val _dayState = MutableLiveData<ScheduleDayState>(ScheduleDayState.Loading)
    val dayState: LiveData<ScheduleDayState> = _dayState

    private val _saveState = MutableLiveData<ScheduleSaveState>(ScheduleSaveState.Idle)
    val saveState: LiveData<ScheduleSaveState> = _saveState

    private var loadJob: Job? = null

    /** Loads the slots that already exist for [date], so the form shows the current settings. */
    fun loadDay(date: VisitDate) {
        loadJob?.cancel()
        _dayState.value = ScheduleDayState.Loading
        loadJob = viewModelScope.launch {
            _dayState.value = try {
                ScheduleDayState.Loaded(date, bookings.timeslotsOn(date))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Loading day failed", e)
                ScheduleDayState.Failed("We couldn't load this day. Check your connection and try again.")
            }
        }
    }

    fun save(date: VisitDate, capacity: Int, open: Boolean) {
        if (_saveState.value == ScheduleSaveState.Saving) return
        val day = _dayState.value as? ScheduleDayState.Loaded
        if (day == null || day.date != date) {
            _saveState.value = ScheduleSaveState.Failed("This day is still loading. Try again in a moment.")
            return
        }

        when (val plan = DaySchedule.plan(date, day.slots, capacity, open)) {
            is DayPlan.Invalid -> _saveState.value = ScheduleSaveState.Failed(plan.message)
            is DayPlan.Ready -> {
                _saveState.value = ScheduleSaveState.Saving
                viewModelScope.launch {
                    _saveState.value = try {
                        schedule.apply(plan.writes)
                        ScheduleSaveState.Saved(plan.writes.size, capacity)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // PERMISSION_DENIED here means this account isn't recognised as an admin
                        Diagnostics.warn(TAG, "Saving schedule failed", e)
                        ScheduleSaveState.Failed(FailureMessages.forAdmin(e, "save the schedule"))
                    }
                    loadDay(date)   // show what is actually saved now
                }
            }
        }
    }

    /**
     * Opens every Tuesday-Saturday in the coming weeks that has no times yet, with the
     * standard hourly slots. Days an admin has already set up (or closed) are left alone,
     * and existing bookings are never touched.
     */
    fun openDefaultWeeks(
        capacity: Int = DefaultSchedule.DEFAULT_CAPACITY,
        weeks: Int = DefaultSchedule.DEFAULT_WEEKS
    ) {
        if (_saveState.value == ScheduleSaveState.Saving) return
        _saveState.value = ScheduleSaveState.Saving
        viewModelScope.launch {
            _saveState.value = try {
                val today = VisitDate.today()
                val days = DefaultSchedule.openingDays(today, weeks * 7 + 1).filter { it > today }
                val writes = mutableListOf<SlotWrite>()
                var openedDays = 0
                for (day in days) {
                    if (bookings.timeslotsOn(day).isNotEmpty()) continue
                    val plan = DaySchedule.plan(day, emptyList(), capacity, true) as? DayPlan.Ready ?: continue
                    writes += plan.writes
                    openedDays++
                }
                if (writes.isNotEmpty()) schedule.apply(writes)   // one batch: all days or none
                ScheduleSaveState.OpenedDefaults(openedDays, writes.size, weeks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.warn(TAG, "Opening default weeks failed", e)
                ScheduleSaveState.Failed(FailureMessages.forAdmin(e, "open the coming weeks"))
            }
            (_dayState.value as? ScheduleDayState.Loaded)?.date?.let { loadDay(it) }
        }
    }

    fun onSaveMessageShown() {
        _saveState.value = ScheduleSaveState.Idle
    }

    private companion object {
        const val TAG = "AdminSchedule"
    }
}
