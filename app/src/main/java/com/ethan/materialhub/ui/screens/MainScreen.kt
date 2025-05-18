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
import androidx.compose.material.icons.filled.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.ui.draw.blur
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ethan.materialhub.ui.weather.ForecastUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewModel: WeatherViewModel = hiltViewModel()) {
    var editMode by remember { mutableStateOf(false) }
    var themePickerOpen by remember { mutableStateOf(false) }
    var selectedMetrics by remember { mutableStateOf(listOf("Wind Speed", "Precipitation", "Humidity")) }
    var userGradient by remember { mutableStateOf(listOf(Color(0xFFB3C6FF), Color(0xFFE3F2FD))) }
    var userShape by remember { mutableStateOf<RoundedCornerShape>(RoundedCornerShape(32.dp)) }

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
    val forecastState by viewModel.forecastState.collectAsState()

    // Animated gradient based on weather
    val animatedGradient = remember(weatherState) {
        when (weatherState) {
            is WeatherUiState.Success -> {
                val desc = (weatherState as WeatherUiState.Success).weather.current.condition.text.lowercase()
                when {
                    "sun" in desc -> listOf(Color(0xFFFFE082), Color(0xFFFFF8E1))
                    "rain" in desc -> listOf(Color(0xFF90CAF9), Color(0xFFB3E5FC))
                    "cloud" in desc -> listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1))
                    "snow" in desc -> listOf(Color(0xFFE1F5FE), Color(0xFFFFFFFF))
                    else -> userGradient
                }
            }
            else -> userGradient
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Weather Section with AnimatedContent and animated gradient
            AnimatedContent(targetState = weatherState, label = "weather-animated-content") { state ->
                val gradientBrush = Brush.verticalGradient(animatedGradient)
                ElevatedCard(
                    shape = userShape,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = 0.98f }
                        .background(gradientBrush, shape = userShape)
                        .blur(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (state) {
                            is WeatherUiState.Loading -> {
                                Text("Loading Weather...", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    repeat(3) {
                                        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }
                            }
                            is WeatherUiState.Error -> {
                                Text((state as WeatherUiState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                            }
                            is WeatherUiState.Success -> {
                                val weather = (state as WeatherUiState.Success).weather
                                Text(weather.location.name, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                                WeatherCircleWidgetReal(weather)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    selectedMetrics.forEach { metric ->
                                        WeatherMetricCircleReal(metric, weather)
                                    }
                                }
                                // Glanceable chips for quick info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Feels like ${weather.current.temp_c}°C") },
                                        leadingIcon = { Icon(Icons.Filled.Thermostat, contentDescription = null) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("UV ${weather.current.uv}") },
                                        leadingIcon = { Icon(Icons.Filled.WbSunny, contentDescription = null) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Forecast Section with AnimatedContent and animated list items
            AnimatedContent(targetState = forecastState, label = "forecast-animated-content") { state ->
                when (state) {
                    is ForecastUiState.Success -> {
                        val forecastDays = state.forecast.forecast.forecastday
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(forecastDays, key = { it.date }) { day ->
                                    AnimatedVisibility(visible = true, enter = androidx.compose.animation.fadeIn(tween(600)), exit = androidx.compose.animation.fadeOut(tween(600))) {
                                        val icon = getMaterialWeatherIcon(day.day.condition.text)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(day.date, style = MaterialTheme.typography.bodySmall)
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = day.day.condition.text,
                                                modifier = Modifier.size(40.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text("${day.day.mintemp_c.toInt()}°/${day.day.maxtemp_c.toInt()}°", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> ForecastRow(state)
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            // News Section with animated list items
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().height(400.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("News", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    // Animate news items (assuming NewsScreen uses LazyColumn)
                    NewsScreen()
                }
            }
        }
        // Animated, morphing FAB (top right)
        Box(modifier = Modifier.fillMaxSize()) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val fabShape by animateDpAsState(targetValue = if (isPressed) 24.dp else 56.dp, animationSpec = spring())
            val fabColor by animateColorAsState(targetValue = if (isPressed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, animationSpec = spring())
            FloatingActionButton(
                onClick = { themePickerOpen = true },
                containerColor = fabColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = if (isPressed) RoundedCornerShape(24.dp) else CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = fabShape),
                interactionSource = interactionSource,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = "Theme")
            }
        }
        if (themePickerOpen) {
            ThemePickerDialog(
                currentGradient = userGradient,
                currentShape = userShape,
                onDismiss = { themePickerOpen = false },
                onSave = { gradient, shape ->
                    userGradient = gradient
                    userShape = shape
                    themePickerOpen = false
                }
            )
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

fun getMaterialWeatherIcon(condition: String): ImageVector {
    return when (condition.lowercase()) {
        "sunny", "clear" -> Icons.Filled.WbSunny
        "partly cloudy", "cloudy", "overcast" -> Icons.Filled.Cloud
        "rain", "patchy rain possible", "light rain", "showers" -> Icons.Filled.Umbrella
        "thunderstorm", "thundery outbreaks possible" -> Icons.Filled.FlashOn
        "snow", "sleet", "ice", "blizzard" -> Icons.Filled.AcUnit
        "fog", "mist", "freezing fog" -> Icons.Filled.Cloud
        else -> Icons.Filled.CloudQueue
    }
}

@Composable
fun WeatherCircleWidgetReal(weather: com.ethan.materialhub.data.weather.model.WeatherResponse) {
    val temp = weather.current.temp_c
    val desc = weather.current.condition.text
    val icon = getMaterialWeatherIcon(desc)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp,
        modifier = Modifier.size(180.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
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

@Composable
fun ForecastRow(forecastState: ForecastUiState) {
    when (forecastState) {
        is ForecastUiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ForecastUiState.Error -> {
            Text(forecastState.message, color = MaterialTheme.colorScheme.error)
        }
        is ForecastUiState.Success -> {
            val forecastDays = forecastState.forecast.forecast.forecastday
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    forecastDays.forEach { day ->
                        val icon = getMaterialWeatherIcon(day.day.condition.text)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.date, style = MaterialTheme.typography.bodySmall)
                            Icon(
                                imageVector = icon,
                                contentDescription = day.day.condition.text,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("${day.day.mintemp_c.toInt()}°/${day.day.maxtemp_c.toInt()}°", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

suspend fun fetchLocationAndWeather(fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient, viewModel: WeatherViewModel) {
    try {
        val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        location?.let {
            viewModel.fetchWeather(it.latitude, it.longitude)
            viewModel.fetchForecast(it.latitude, it.longitude, 5)
        } ?: run {
            viewModel.fetchWeather(40.7128, -74.0060)
            viewModel.fetchForecast(40.7128, -74.0060, 5)
        }
    } catch (e: SecurityException) {
        viewModel.fetchWeather(40.7128, -74.0060)
        viewModel.fetchForecast(40.7128, -74.0060, 5)
    } catch (e: Exception) {
        viewModel.fetchWeather(40.7128, -74.0060)
        viewModel.fetchForecast(40.7128, -74.0060, 5)
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

@Composable
fun ThemePickerDialog(currentGradient: List<Color>, currentShape: RoundedCornerShape, onDismiss: () -> Unit, onSave: (List<Color>, RoundedCornerShape) -> Unit) {
    val gradients = listOf(
        listOf(Color(0xFFB3C6FF), Color(0xFFE3F2FD)),
        listOf(Color(0xFFFFE082), Color(0xFFFFF8E1)),
        listOf(Color(0xFF90CAF9), Color(0xFFB3E5FC)),
        listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1)),
        listOf(Color(0xFFE1F5FE), Color(0xFFFFFFFF))
    )
    val shapes = listOf(
        RoundedCornerShape(32.dp),
        RoundedCornerShape(16.dp),
        RoundedCornerShape(0.dp)
    )
    var selectedGradient by remember { mutableStateOf(currentGradient) }
    var selectedShape by remember { mutableStateOf(currentShape) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Theme Style") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Gradient")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    gradients.forEach { grad ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Brush.verticalGradient(grad), shape = RoundedCornerShape(16.dp))
                                .border(2.dp, if (selectedGradient == grad) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { selectedGradient = grad }
                        )
                    }
                }
                Text("Shape")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shapes.forEach { shape ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = shape)
                                .border(2.dp, if (selectedShape == shape) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
                                .clickable { selectedShape = shape }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedGradient, selectedShape) }) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
} 