package com.ethan.materialhub.data.calendar.model

import java.util.*

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val startTime: Date,
    val endTime: Date,
    val location: String? = null,
    val calendarId: Long
) 