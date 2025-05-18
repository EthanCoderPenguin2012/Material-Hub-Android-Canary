@file:OptIn(ExperimentalMaterial3Api::class)
package com.ethan.materialhub.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ethan.materialhub.data.weather.model.WeatherResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            coroutineScope.launch { fetchLocation(fusedLocationClient, viewModel) }
        }
    }

    val weatherState by viewModel.weatherState.collectAsState()

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fetchLocation(fusedLocationClient, viewModel)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (weatherState) {
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is WeatherUiState.Success -> {
                WeatherContent((weatherState as WeatherUiState.Success).weather)
            }
            is WeatherUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (weatherState as WeatherUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherContent(weather: WeatherResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = weather.location.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "${weather.current.temp_c}°C",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = weather.current.condition.text,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                WeatherDetailRow("Humidity", "${weather.current.humidity}%")
                WeatherDetailRow("Wind Speed", "${weather.current.wind_kph} kph")
                WeatherDetailRow("Precipitation", "${weather.current.precip_mm} mm")
                WeatherDetailRow("UV Index", "${weather.current.uv}")
            }
        }
    }
}

@Composable
private fun WeatherDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun String.capitalizeFirstChar(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private suspend fun fetchLocation(
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: WeatherViewModel
) {
    try {
        if (!fusedLocationClient.isLocationAvailable()) {
            viewModel.fetchWeather(40.7128, -74.0060) // Fallback to New York City
            return
        }

        val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        location?.let {
            viewModel.fetchWeather(it.latitude, it.longitude)
        } ?: run {
            viewModel.fetchWeather(40.7128, -74.0060) // Fallback to New York City
        }
    } catch (e: SecurityException) {
        // Handle permission denied
        viewModel.fetchWeather(40.7128, -74.0060) // Fallback to New York City
    } catch (e: Exception) {
        // Handle other location errors
        viewModel.fetchWeather(40.7128, -74.0060) // Fallback to New York City
    }
}

private fun FusedLocationProviderClient.isLocationAvailable(): Boolean {
    return try {
        getLastLocation().isComplete
    } catch (e: SecurityException) {
        false
    }
} 