package com.ethan.materialhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ethan.materialhub.ui.theme.MaterialHubTheme
import com.ethan.materialhub.ui.screens.news.NewsScreen
import com.ethan.materialhub.ui.screens.weather.WeatherScreen
import com.ethan.materialhub.ui.screens.todo.TodoScreen
import com.ethan.materialhub.ui.screens.calendar.CalendarScreen
import com.ethan.materialhub.ui.screens.MainScreen
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val icon: Int, val label: String) {
    object News : Screen("news", android.R.drawable.ic_menu_compass, "News")
    object Weather : Screen("weather", android.R.drawable.ic_menu_compass, "Weather")
    object Todo : Screen("todo", android.R.drawable.ic_menu_edit, "Todo")
    object Calendar : Screen("calendar", android.R.drawable.ic_menu_my_calendar, "Calendar")
}

val screens = listOf(Screen.News, Screen.Weather, Screen.Todo, Screen.Calendar)

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialHubTheme {
                MainScreen()
            }
        }
    }
}