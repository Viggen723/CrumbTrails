package com.example.routetracker.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.routetracker.featuresAPI.history.ui.HistoryUI
import com.example.routetracker.featuresAPI.tracking.ui.TrackingUI

// Link that explains NavHost: https://developer.android.com/codelabs/basic-android-kotlin-compose-navigation?hl=ja#3
@Composable
fun RouteTrackerNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = RouteTrackerDestination.TRACK.name,
        modifier = modifier.fillMaxSize()
    ) {
        composable(route = RouteTrackerDestination.TRACK.name) {
            TrackingUI(modifier = Modifier.fillMaxSize())
        }
        composable(route = RouteTrackerDestination.HISTORY.name) {
            HistoryUI(modifier = Modifier.fillMaxSize())
        }
    }
}
