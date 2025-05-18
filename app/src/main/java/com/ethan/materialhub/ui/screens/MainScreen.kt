package com.ethan.materialhub.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import com.ethan.materialhub.ui.screens.weather.WeatherScreen
import com.ethan.materialhub.ui.screens.news.NewsScreen
import com.ethan.materialhub.ui.weather.WeatherViewModel
import com.ethan.materialhub.ui.weather.WeatherUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: WeatherViewModel = hiltViewModel()) {
    var editMode by remember { mutableStateOf(false) }
    var selectedMetrics by remember { mutableStateOf(listOf("Wind Speed", "Precipitation", "Humidity")) }

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
            coroutineScope.launch { fetchLocationAndWeather(fusedLocationClient, viewModel) }
        }
    }
    val weatherState by viewModel.weatherState.collectAsState()

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fetchLocationAndWeather(fusedLocationClient, viewModel)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (weatherState) {
                is WeatherUiState.Loading -> {
                    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        repeat(3) {
                            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
                is WeatherUiState.Error -> {
                    Text((weatherState as WeatherUiState.Error).message, color = MaterialTheme.colorScheme.error)
                }
                is WeatherUiState.Success -> {
                    val weather = (weatherState as WeatherUiState.Success).weather
                    WeatherCircleWidgetReal(weather)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        selectedMetrics.forEach { metric ->
                            WeatherMetricCircleReal(metric, weather)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            NewsScreen()
        }
        // Edit button (top right)
        IconButton(
            onClick = { editMode = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        if (editMode) {
            EditMetricsDialog(
                selectedMetrics = selectedMetrics,
                onDismiss = { editMode = false },
                onSave = { newMetrics ->
                    selectedMetrics = newMetrics
                    editMode = false
                }
            )
        }
    }
}

@Composable
fun WeatherCircleWidgetReal(weather: com.ethan.materialhub.data.weather.model.WeatherResponse) {
    val temp = weather.current.temp_c
    val desc = weather.current.condition.text
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp,
        modifier = Modifier.size(180.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${temp}°C", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(desc, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun WeatherMetricCircleReal(metric: String, weather: com.ethan.materialhub.data.weather.model.WeatherResponse) {
    val value = when (metric) {
        "Wind Speed" -> "${weather.current.wind_kph} kph"
        "Precipitation" -> "${weather.current.precip_mm} mm"
        "Humidity" -> "${weather.current.humidity}%"
        "UV Index" -> "${weather.current.uv}"
        else -> "-"
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.size(80.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(metric, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

suspend fun fetchLocationAndWeather(fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient, viewModel: WeatherViewModel) {
    try {
        val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        location?.let {
            viewModel.fetchWeather(it.latitude, it.longitude)
        } ?: run {
            viewModel.fetchWeather(40.7128, -74.0060) // Fallback to New York City
        }
    } catch (e: SecurityException) {
        viewModel.fetchWeather(40.7128, -74.0060)
    } catch (e: Exception) {
        viewModel.fetchWeather(40.7128, -74.0060)
    }
}

@Composable
fun EditMetricsDialog(selectedMetrics: List<String>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val allMetrics = listOf("Wind Speed", "Precipitation", "Humidity", "UV Index")
    val selected = remember { mutableStateListOf(*selectedMetrics.toTypedArray()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Weather Metrics") },
        text = {
            Column {
                allMetrics.forEach { metric ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected.contains(metric),
                            onCheckedChange = {
                                if (it) selected.add(metric) else selected.remove(metric)
                            }
                        )
                        Text(metric)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selected.take(3)) }) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
} 