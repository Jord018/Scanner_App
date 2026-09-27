package com.LingTH.fridge.Notification

import java.time.LocalTime
import java.time.Duration



class AlertTimeManager(
    private val alertsPerDay: Int,
    private val startHour: Int = 9,
    private val endHour: Int = 21
) {
    val timeSlots: List<LocalTime> = run {
        val start = LocalTime.of(startHour, 0)
        val end = LocalTime.of(endHour, 0)
        val interval = Duration.between(start, end).dividedBy(alertsPerDay.toLong())
        List(alertsPerDay) { i -> start.plus(interval.multipliedBy(i.toLong())) }
    }

    fun debugTimeSlots(): List<String> =
        timeSlots.map { it.toString() }

}




