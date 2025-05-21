package com.ethan.materialhub.data.news

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class NewsRepository @Inject constructor(private val apiKey: String) {
    private val newsApi: NewsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApi::class.java)
    }

    fun getTopHeadlines(
        country: String = "us",
        pageSize: Int = 20,
        page: Int = 1
    ): Flow<Result<NewsResponse>> = flow {
        try {
            val response = newsApi.getTopHeadlines(
                country = country,
                apiKey = apiKey,
                pageSize = pageSize,
                page = page
            )
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun searchNews(
        query: String,
        pageSize: Int = 20,
        page: Int = 1
    ): Flow<Result<NewsResponse>> = flow {
        try {
            val response = newsApi.searchNews(
                query = query,
                apiKey = apiKey,
                pageSize = pageSize,
                page = page
            )
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
} 