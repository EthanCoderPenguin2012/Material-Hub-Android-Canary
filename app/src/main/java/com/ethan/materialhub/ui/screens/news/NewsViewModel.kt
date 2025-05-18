package com.ethan.materialhub.ui.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ethan.materialhub.data.news.Article
import com.ethan.materialhub.data.news.NewsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NewsViewModel(private val repository: NewsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentPage = 1
    private var isLoadingMore = false
    private var hasMorePages = true

    init {
        loadTopHeadlines()
    }

    fun loadTopHeadlines() {
        viewModelScope.launch {
            _uiState.value = NewsUiState.Loading
            repository.getTopHeadlines(page = currentPage)
                .catch { e -> _uiState.value = NewsUiState.Error(e.message ?: "Unknown error") }
                .collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _uiState.value = NewsUiState.Success(
                                articles = response.articles,
                                hasMorePages = response.articles.size >= 20
                            )
                        },
                        onFailure = { e ->
                            _uiState.value = NewsUiState.Error(e.message ?: "Unknown error")
                        }
                    )
                }
        }
    }

    fun searchNews(query: String) {
        if (query.isBlank()) {
            loadTopHeadlines()
            return
        }

        viewModelScope.launch {
            _searchQuery.value = query
            _uiState.value = NewsUiState.Loading
            currentPage = 1
            hasMorePages = true
            
            repository.searchNews(query = query, page = currentPage)
                .catch { e -> _uiState.value = NewsUiState.Error(e.message ?: "Unknown error") }
                .collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _uiState.value = NewsUiState.Success(
                                articles = response.articles,
                                hasMorePages = response.articles.size >= 20
                            )
                        },
                        onFailure = { e ->
                            _uiState.value = NewsUiState.Error(e.message ?: "Unknown error")
                        }
                    )
                }
        }
    }

    fun loadMoreNews() {
        if (isLoadingMore || !hasMorePages) return
        
        isLoadingMore = true
        currentPage++
        
        viewModelScope.launch {
            val currentArticles = (uiState.value as? NewsUiState.Success)?.articles ?: emptyList()
            
            val flow = if (_searchQuery.value.isBlank()) {
                repository.getTopHeadlines(page = currentPage)
            } else {
                repository.searchNews(query = _searchQuery.value, page = currentPage)
            }
            
            flow.collect { result ->
                result.fold(
                    onSuccess = { response ->
                        val newArticles = currentArticles + response.articles
                        _uiState.value = NewsUiState.Success(
                            articles = newArticles,
                            hasMorePages = response.articles.size >= 20
                        )
                        hasMorePages = response.articles.size >= 20
                    },
                    onFailure = { e ->
                        _uiState.value = NewsUiState.Error(e.message ?: "Unknown error")
                    }
                )
                isLoadingMore = false
            }
        }
    }
}

sealed class NewsUiState {
    data object Loading : NewsUiState()
    data class Success(
        val articles: List<Article>,
        val hasMorePages: Boolean
    ) : NewsUiState()
    data class Error(val message: String) : NewsUiState()
}

class NewsViewModelFactory(private val repository: NewsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 