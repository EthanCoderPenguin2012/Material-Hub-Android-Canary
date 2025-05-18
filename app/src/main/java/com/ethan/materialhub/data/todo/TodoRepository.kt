package com.ethan.materialhub.data.todo

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()
    val activeTodos: Flow<List<TodoEntity>> = todoDao.getActiveTodos()
    val completedTodos: Flow<List<TodoEntity>> = todoDao.getCompletedTodos()

    suspend fun insertTodo(todo: TodoEntity): Long = todoDao.insertTodo(todo)

    suspend fun updateTodo(todo: TodoEntity) = todoDao.updateTodo(todo)

    suspend fun deleteTodo(todo: TodoEntity) = todoDao.deleteTodo(todo)

    suspend fun deleteCompletedTodos() = todoDao.deleteCompletedTodos()
} 