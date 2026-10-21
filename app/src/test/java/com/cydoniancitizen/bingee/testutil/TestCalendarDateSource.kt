package com.cydoniancitizen.bingee.testutil

import com.cydoniancitizen.bingee.domain.calendar.CalendarDateContext
import com.cydoniancitizen.bingee.domain.calendar.CalendarDateSource
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class TestCalendarDateSource(initialDate: LocalDate) : CalendarDateSource {
    private val dates = MutableStateFlow(CalendarDateContext(initialDate, ZoneId.systemDefault()))

    override fun currentDate(): LocalDate = dates.value.today

    override fun observeDate(): Flow<LocalDate> = dates.map { it.today }.distinctUntilChanged()

    override fun currentContext(): CalendarDateContext = dates.value

    override fun observeContext(): Flow<CalendarDateContext> = dates

    fun advanceTo(date: LocalDate) {
        dates.value = dates.value.copy(today = date)
    }

    fun changeZone(zoneId: ZoneId) {
        dates.value = dates.value.copy(zoneId = zoneId)
    }
}
