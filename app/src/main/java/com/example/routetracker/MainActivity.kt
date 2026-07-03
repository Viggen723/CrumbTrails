package com.example.routetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.routetracker.navigation.RouteTrackerDestination
import com.example.routetracker.navigation.RouteTrackerNavGraph
import com.example.routetracker.ui.theme.RouteTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RouteTrackerTheme {
                Application()
            }
        }
    }
}

@Composable
fun Application() {

    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomBar(navController = navController) }
    ) { paddingValues ->
        RouteTrackerNavGraph(
            navController = navController,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        )
    }
}

@Composable
fun BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            // Pop back to the graph's start destination to avoid stacking
            // up duplicate copies of a tab as the user bounces between them.
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Filled.Map, contentDescription = "Track") },
            label = { Text("Track") },
            selected = currentRoute?.hierarchy?.any { it.route == RouteTrackerDestination.TRACK.name } == true,
            onClick = { navigateToTab(RouteTrackerDestination.TRACK.name) }
        )
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Filled.History, contentDescription = "History") },
            label = { Text("History") },
            selected = currentRoute?.hierarchy?.any { it.route == RouteTrackerDestination.HISTORY.name } == true,
            onClick = { navigateToTab(RouteTrackerDestination.HISTORY.name) }
        )
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Filled.DynamicFeed, contentDescription = "Feed") },
            label = { Text("Feed") },
            selected = false,
            onClick = { }
        )
    }
}
