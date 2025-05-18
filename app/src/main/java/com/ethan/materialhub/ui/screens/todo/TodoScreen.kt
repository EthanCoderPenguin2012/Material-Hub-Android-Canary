@file:OptIn(ExperimentalMaterial3Api::class)
package com.ethan.materialhub.ui.screens.todo

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ethan.materialhub.data.AppDatabase
import com.ethan.materialhub.data.todo.Priority
import com.ethan.materialhub.data.todo.TodoEntity
import com.ethan.materialhub.data.todo.TodoRepository
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.DeleteSweep

enum class TodoFilter {
    ALL, ACTIVE, COMPLETED
}

enum class TodoSort {
    CREATED_AT, DUE_DATE, PRIORITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(
            TodoRepository(AppDatabase.getDatabase(LocalContext.current).todoDao())
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(TodoFilter.ALL) }
    var currentSort by remember { mutableStateOf(TodoSort.CREATED_AT) }
    
    val filteredAndSortedTodos = remember(uiState, currentFilter, currentSort) {
        when (uiState) {
            is TodoUiState.Success -> {
                val todos = (uiState as TodoUiState.Success).todos
                val filtered = when (currentFilter) {
                    TodoFilter.ALL -> todos
                    TodoFilter.ACTIVE -> todos.filter { !it.isCompleted }
                    TodoFilter.COMPLETED -> todos.filter { it.isCompleted }
                }
                when (currentSort) {
                    TodoSort.CREATED_AT -> filtered.sortedByDescending { it.createdAt }
                    TodoSort.DUE_DATE -> filtered.sortedWith(
                        compareBy<TodoEntity> { it.dueDate == null }
                            .thenBy { it.dueDate }
                    )
                    TodoSort.PRIORITY -> filtered.sortedByDescending { it.priority }
                }
            }
            else -> emptyList()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Filter menu
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        TodoFilter.values().forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter.name.lowercase().capitalize()) },
                                onClick = {
                                    currentFilter = filter
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (currentFilter == filter) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Sort menu
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        TodoSort.values().forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.name.lowercase().replace("_", " ").capitalize()) },
                                onClick = {
                                    currentSort = sort
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (currentSort == sort) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Clear completed
                    IconButton(onClick = { viewModel.deleteCompletedTodos() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Completed")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is TodoUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TodoUiState.Success -> {
                    if (filteredAndSortedTodos.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (currentFilter) {
                                    TodoFilter.ALL -> "No tasks yet. Add one!"
                                    TodoFilter.ACTIVE -> "No active tasks"
                                    TodoFilter.COMPLETED -> "No completed tasks"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredAndSortedTodos) { todo ->
                                TodoItem(
                                    todo = todo,
                                    onToggleComplete = { viewModel.toggleTodoCompletion(todo) },
                                    onDelete = { viewModel.deleteTodo(todo) }
                                )
                            }
                        }
                    }
                }
                is TodoUiState.Error -> {
                    Text(
                        text = (uiState as TodoUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, priority, dueDate ->
                viewModel.addTodo(title, description, priority, dueDate)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItem(
    todo: TodoEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = todo.isCompleted,
                    onCheckedChange = { onToggleComplete() }
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!todo.description.isNullOrBlank()) {
                        Text(
                            text = todo.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PriorityChip(priority = todo.priority)
                        todo.dueDate?.let { date ->
                            Text(
                                text = dateFormat.format(date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PriorityChip(priority: Priority) {
    val (color, text) = when (priority) {
        Priority.LOW -> MaterialTheme.colorScheme.tertiary to "Low"
        Priority.MEDIUM -> MaterialTheme.colorScheme.secondary to "Medium"
        Priority.HIGH -> MaterialTheme.colorScheme.error to "High"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?, priority: Priority, dueDate: Date?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var dueDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.values().forEach { priorityOption ->
                        FilterChip(
                            selected = priority == priorityOption,
                            onClick = { priority = priorityOption },
                            label = {
                                Text(
                                    when (priorityOption) {
                                        Priority.LOW -> "Low"
                                        Priority.MEDIUM -> "Medium"
                                        Priority.HIGH -> "High"
                                    }
                                )
                            }
                        )
                    }
                }
                
                // Due date selection
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (dueDate != null) {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dueDate!!)
                        } else {
                            "Set Due Date (Optional)"
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            title,
                            description.takeIf { it.isNotBlank() },
                            priority,
                            dueDate
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
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            dueDate = Date(it)
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

fun String.capitalizeFirstChar(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 