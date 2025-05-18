package com.ethan.materialhub.ui.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ethan.materialhub.data.todo.Priority
import com.ethan.materialhub.data.todo.TodoEntity
import com.ethan.materialhub.data.todo.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

sealed class TodoUiState {
    object Loading : TodoUiState()
    data class Success(val todos: List<TodoEntity>) : TodoUiState()
    data class Error(val message: String) : TodoUiState()
}

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<TodoUiState>(TodoUiState.Loading)
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch {
            repository.getAllTodos()
                .catch { e ->
                    _uiState.value = TodoUiState.Error(e.message ?: "Failed to load todos")
                }
                .collect { todos ->
                    _uiState.value = TodoUiState.Success(todos)
                }
        }
    }

    fun addTodo(title: String, description: String?, priority: Priority, dueDate: Date?) {
        if (title.isBlank()) return

        viewModelScope.launch {
            try {
                val todo = TodoEntity(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    createdAt = Date(),
                    isCompleted = false
                )
                repository.insertTodo(todo)
            } catch (e: Exception) {
                _uiState.value = TodoUiState.Error(e.message ?: "Failed to add todo")
            }
        }
    }

    fun toggleTodoCompletion(todo: TodoEntity) {
        viewModelScope.launch {
            try {
                repository.updateTodo(todo.copy(isCompleted = !todo.isCompleted))
            } catch (e: Exception) {
                _uiState.value = TodoUiState.Error(e.message ?: "Failed to update todo")
            }
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            try {
                repository.deleteTodo(todo)
            } catch (e: Exception) {
                _uiState.value = TodoUiState.Error(e.message ?: "Failed to delete todo")
            }
        }
    }

    fun deleteCompletedTodos() {
        viewModelScope.launch {
            try {
                when (val currentState = _uiState.value) {
                    is TodoUiState.Success -> {
                        val completedTodos = currentState.todos.filter { it.isCompleted }
                        completedTodos.forEach { todo ->
                            repository.deleteTodo(todo)
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = TodoUiState.Error(e.message ?: "Failed to delete completed todos")
            }
        }
    }

    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            try {
                repository.updateTodo(todo)
            } catch (e: Exception) {
                _uiState.value = TodoUiState.Error(e.message ?: "Failed to update todo")
            }
        }
    }
}

class TodoViewModelFactory(private val repository: TodoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 