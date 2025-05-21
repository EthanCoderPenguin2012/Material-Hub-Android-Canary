package com.ethan.materialhub.di

import android.content.Context
import com.ethan.materialhub.data.AppDatabase
import com.ethan.materialhub.BuildConfig
import com.ethan.materialhub.data.news.NewsRepository
import com.ethan.materialhub.data.calendar.CalendarRepository
import com.ethan.materialhub.data.weather.WeatherRepository
import com.ethan.materialhub.data.weather.api.WeatherApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import javax.inject.Inject
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    @Named("WeatherApiKey")
    fun provideWeatherApiKey(): String {
        // TODO: Replace "YOUR_OPENWEATHERMAP_API_KEY" in app/build.gradle or wherever BuildConfig is configured
        // with your actual WeatherAPI key.
        // Ensure this key is added to your build.gradle file.
        // Example in app/build.gradle.kts (assuming buildConfigFields):
        // buildConfigFields { put("String", "WEATHER_API_KEY", "\"YOUR_WEATHER_API_KEY\"") }
        return BuildConfig.WEATHER_API_KEY
    }

    @Provides
    @Singleton
    @Named("NewsApiKey")
    fun provideNewsApiKey(): String {
        // TODO: Replace "YOUR_NEWS_API_KEY" in app/build.gradle or wherever BuildConfig is configured
        // with your actual News API key.
        // Ensure this key is added to your build.gradle file.
        // Example in app/build.gradle.kts (assuming buildConfigFields):
        // buildConfigFields { put("String", "NEWS_API_KEY", "\"YOUR_NEWS_API_KEY\"") }
        // **IMPORTANT**: Verify the actual BuildConfig field name for the News API key, it might be different.
        // Assuming NEWS_API_KEY for now.
        // If BuildConfig.NEWS_API_KEY is not found, you might need to check your build.gradle configuration.

        // Removed temporary workaround.
        return BuildConfig.NEWS_API_KEY
    }

    @Provides
    @Singleton
    fun provideWeatherApi(): WeatherApi {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/") // Verify the base URL for your chosen weather API
            // Consider adding an OkHttpClient interceptor to automatically add the API key
            // This is a more robust approach than passing it in every ViewModel call
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNewsRepository(@Named("NewsApiKey") apiKey: String): NewsRepository {
        return NewsRepository(apiKey)
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(weatherApi: WeatherApi, @Named("WeatherApiKey") apiKey: String): WeatherRepository {
        return WeatherRepository(weatherApi, apiKey)
    }

    @Provides
    @Singleton
    fun provideCalendarRepository(@ApplicationContext context: Context): CalendarRepository {
        return CalendarRepository(context)
    }
} 