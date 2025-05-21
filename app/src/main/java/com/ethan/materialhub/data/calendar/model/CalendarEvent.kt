package com.ethan.materialhub.data.calendar.model

import java.time.Instant

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val startTime: Instant,
    val endTime: Instant,
    val location: String? = null,
    val calendarId: Long
) 