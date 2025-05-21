@file:OptIn(ExperimentalMaterial3Api::class)
package com.ethan.materialhub.ui.calendar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ethan.materialhub.data.calendar.model.CalendarEvent
import java.util.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val events by viewModel.events.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }

    val context = LocalContext.current
    var hasCalendarPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCalendarPermissions = permissions[Manifest.permission.READ_CALENDAR] == true &&
                               permissions[Manifest.permission.WRITE_CALENDAR] == true
        if (hasCalendarPermissions) {
            // Refresh events if permissions are granted after denial
            viewModel.setSelectedDate(viewModel.selectedDate.value)
        } else {
            // Handle case where permissions are still denied - maybe show a persistent message
            viewModel.setUiStateError("Calendar permissions are required to view and manage events.")
        }
    }

    // Request permissions when the screen is first displayed if not already granted
    LaunchedEffect(Unit) {
        if (!hasCalendarPermissions) {
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
        } else {
             // If permissions are already granted, trigger initial data load
            viewModel.setSelectedDate(viewModel.selectedDate.value)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    IconButton(onClick = {
                        if (hasCalendarPermissions) {
                            showAddEventDialog = true
                        } else {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                            // TODO: Show a message to the user explaining why permissions are needed (e.g., a Snackbar)
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date selector
            DateSelector(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    if (hasCalendarPermissions) {
                        viewModel.setSelectedDate(date)
                    } else {
                        calendarPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                        // TODO: Show a message to the user explaining why permissions are needed
                    }
                }
            )

            // Display content based on UI state and permissions
            when {
                 !hasCalendarPermissions -> {
                     Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Calendar permissions are required to view and manage events.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                 }
                uiState is CalendarUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState is CalendarUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState as CalendarUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                uiState is CalendarUiState.Success -> {
                     if (events.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No events for this day",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(events) { event ->
                                EventCard(
                                    event = event,
                                    onEdit = { eventToEdit = event },
                                    onDelete = { viewModel.deleteEvent(event.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onEventAdded = { event ->
                if (hasCalendarPermissions) {
                    viewModel.addEvent(event)
                } else {
                     calendarPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR
                        )
                    )
                    // TODO: Show a message
                }
                showAddEventDialog = false
            }
        )
    }

    eventToEdit?.let { event ->
        EditEventDialog(
            event = event,
            onDismiss = { eventToEdit = null },
            onEventUpdated = { updatedEvent ->
                if (hasCalendarPermissions) {
                    viewModel.updateEvent(updatedEvent)
                } else {
                     calendarPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR
                        )
                    )
                    // TODO: Show a message
                }
                eventToEdit = null
            }
        )
    }
}

@Composable
private fun DateSelector(
    selectedDate: Instant,
    onDateSelected: (Instant) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMMM d, yyyy") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
             onDateSelected(selectedDate.minus(1, ChronoUnit.DAYS))
        }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
        }

        Text(
            text = dateFormat.format(selectedDate.atZone(ZoneId.systemDefault())),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.clickable { showDatePicker = true }
        )

        IconButton(onClick = {
             onDateSelected(selectedDate.plus(1, ChronoUnit.DAYS))
        }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(Instant.ofEpochMilli(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Event")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${timeFormat.format(event.startTime.atZone(ZoneId.systemDefault()))} - ${timeFormat.format(event.endTime.atZone(ZoneId.systemDefault()))}",
                style = MaterialTheme.typography.bodyMedium
            )

            event.description?.let { description ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            event.location?.let { location ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onEventAdded: (CalendarEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(Instant.now().truncatedTo(ChronoUnit.MINUTES)) }
    var endTime by remember { mutableStateOf(Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.HOURS)) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start: ${timeFormat.format(startTime.atZone(ZoneId.systemDefault()))}")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("End: ${timeFormat.format(endTime.atZone(ZoneId.systemDefault()))}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && endTime.isAfter(startTime)) {
                        onEventAdded(
                            CalendarEvent(
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                startTime = startTime,
                                endTime = endTime,
                                location = location.takeIf { it.isNotBlank() },
                                calendarId = 1 // TODO: Get default calendar ID or allow user selection
                            )
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { selectedLocalTime ->
                val today = LocalDate.now()
                val selectedZonedTime = selectedLocalTime.atDate(today).atZone(ZoneId.systemDefault())
                startTime = selectedZonedTime.toInstant()
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { selectedLocalTime ->
                val today = LocalDate.now()
                val selectedZonedTime = selectedLocalTime.atDate(today).atZone(ZoneId.systemDefault())
                endTime = selectedZonedTime.toInstant()
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun EditEventDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onEventUpdated: (CalendarEvent) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start: ${timeFormat.format(startTime.atZone(ZoneId.systemDefault()))}")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("End: ${timeFormat.format(endTime.atZone(ZoneId.systemDefault()))}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && endTime.isAfter(startTime)) {
                        onEventUpdated(
                            event.copy(
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                startTime = startTime,
                                endTime = endTime,
                                location = location.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { selectedLocalTime ->
                val eventDate = event.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                val selectedZonedTime = selectedLocalTime.atDate(eventDate).atZone(ZoneId.systemDefault())
                startTime = selectedZonedTime.toInstant()
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { selectedLocalTime ->
                val eventDate = event.endTime.atZone(ZoneId.systemDefault()).toLocalDate()
                val selectedZonedTime = selectedLocalTime.atDate(eventDate).atZone(ZoneId.systemDefault())
                endTime = selectedZonedTime.toInstant()
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun TimePickerDialog(
    initialTime: Instant = Instant.now(),
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    val initialLocalTime = initialTime.atZone(ZoneId.systemDefault()).toLocalTime()
    val timePickerState = rememberTimePickerState(
        initialHour = initialLocalTime.hour,
        initialMinute = initialLocalTime.minute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 