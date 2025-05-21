package com.ethan.materialhub.data.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.ethan.materialhub.data.calendar.model.CalendarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun getEvents(startTime: Long, endTime: Long): Flow<List<CalendarEvent>> = flow {
        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTEND} <= ?"
        val selectionArgs = arrayOf(startTime.toString(), endTime.toString())

        // Note: Querying the Calendar Provider requires READ_CALENDAR permission.
        // Permission handling is assumed to be done in the UI layer.
        try {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                    val description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
                    val startMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                    val endMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                    val location = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION))
                    val calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))

                    events.add(
                        CalendarEvent(
                            id = id,
                            title = title,
                            description = description,
                            startTime = Instant.ofEpochMilli(startMillis),
                            endTime = Instant.ofEpochMilli(endMillis),
                            location = location,
                            calendarId = calendarId
                        )
                    )
                }
            }
             emit(events)
        } catch (e: SecurityException) {
            // Handle missing permissions - rethrow or emit an error state
            throw e // Let ViewModel handle the error state
        } catch (e: Exception) {
            // Handle other potential query errors
             throw e // Let ViewModel handle the error state
        }


    }.flowOn(Dispatchers.IO)

    suspend fun addEvent(event: CalendarEvent): Long = withContext(Dispatchers.IO) {
         // Note: Inserting into Calendar Provider requires WRITE_CALENDAR permission.
        // Permission handling is assumed to be done in the UI layer.
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.DTSTART, event.startTime.toEpochMilli())
                put(CalendarContract.Events.DTEND, event.endTime.toEpochMilli())
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id) // Using java.util.TimeZone for compatibility
            }

            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ContentUris.parseId(uri ?: throw IllegalStateException("Failed to insert event"))
        } catch (e: SecurityException) {
             // Handle missing permissions
             throw e // Let ViewModel handle the error state
        } catch (e: Exception) {
             // Handle other potential insertion errors
             throw e // Let ViewModel handle the error state
        }
    }

    suspend fun updateEvent(event: CalendarEvent) = withContext(Dispatchers.IO) {
         // Note: Updating Calendar Provider requires WRITE_CALENDAR permission.
        // Permission handling is assumed to be done in the UI layer.
         try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.DTSTART, event.startTime.toEpochMilli())
                put(CalendarContract.Events.DTEND, event.endTime.toEpochMilli())
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
            }

            val selection = "${CalendarContract.Events._ID} = ?"
            val selectionArgs = arrayOf(event.id.toString())

            contentResolver.update(
                CalendarContract.Events.CONTENT_URI,
                values,
                selection,
                selectionArgs
            )
        } catch (e: SecurityException) {
             // Handle missing permissions
             throw e // Let ViewModel handle the error state
        } catch (e: Exception) {
             // Handle other potential update errors
             throw e // Let ViewModel handle the error state
        }
    }

    suspend fun deleteEvent(eventId: Long) = withContext(Dispatchers.IO) {
         // Note: Deleting from Calendar Provider requires WRITE_CALENDAR permission.
        // Permission handling is assumed to be done in the UI layer.
         try {
            val selection = "${CalendarContract.Events._ID} = ?"
            val selectionArgs = arrayOf(eventId.toString())

            contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                selection,
                selectionArgs
            )
        } catch (e: SecurityException) {
             // Handle missing permissions
             throw e // Let ViewModel handle the error state
        } catch (e: Exception) {
             // Handle other potential deletion errors
             throw e // Let ViewModel handle the error state
        }
    }

    suspend fun getDefaultCalendarId(): Long? = withContext(Dispatchers.IO) {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.IS_PRIMARY} = 1"
        val uri = CalendarContract.Calendars.CONTENT_URI

        // Query for primary calendar
        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return@withContext cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                }
            }
        } catch (e: SecurityException) {
             // Handle missing READ_CALENDAR permission
             throw e // Let ViewModel handle the error state
        } catch (e: Exception) {
             // Handle other query errors
             throw e // Let ViewModel handle the error state
        }


        // If no primary calendar found, get the first available calendar
         try {
            context.contentResolver.query(
                uri,
                projection,
                null, // No selection
                null,
                "${CalendarContract.Calendars._ID} ASC" // Order by ID to get the first one
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return@withContext cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                }
            }
        } catch (e: SecurityException) {
             // Handle missing READ_CALENDAR permission
             throw e // Let ViewModel handle the error state
        } catch (e: Exception) {
             // Handle other query errors
             throw e // Let ViewModel handle the error state
        }

        return@withContext null // No calendars found
    }
} 