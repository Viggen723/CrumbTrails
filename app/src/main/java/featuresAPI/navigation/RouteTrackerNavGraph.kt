package com.example.routetracker.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.routetracker.featuresAPI.auth.ui.LoginScreen
import com.example.routetracker.featuresAPI.history.ui.HistoryUI
import com.example.routetracker.featuresAPI.tracking.ui.TrackingUI
import featuresAPI.authentication.data.AuthenticationRepository
import featuresAPI.authentication.viewModel.AuthenticationViewModel
import featuresAPI.feed.ui.FeedUI
import featuresAPI.settings.ui.SettingsUI

@Composable
fun RouteTrackerNavGraph(
    navController: NavHostController,
    authRepository: AuthenticationRepository,
    authViewModel: AuthenticationViewModel,
    modifier: Modifier = Modifier
) {
    // Figure out what is displayed first on start up (If already logged in or not)
    val initialDestination = if (authRepository.hasUserSession) {
        RouteTrackerDestination.TRACK.name
    } else {
        RouteTrackerDestination.LOGIN.name
    }

    NavHost(
        navController = navController,
        startDestination = initialDestination,
        modifier = modifier.fillMaxSize()
    ) {
        composable(route = RouteTrackerDestination.LOGIN.name) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccessRoute = {
                    // Navigate to tracking and then clear the Login from the staack
                    navController.navigate(RouteTrackerDestination.TRACK.name) {
                        popUpTo(RouteTrackerDestination.LOGIN.name) { inclusive = true }
                    }
                }
            )
        }

        composable(route = RouteTrackerDestination.TRACK.name) {
            TrackingUI(modifier = Modifier.fillMaxSize())
        }
        composable(route = RouteTrackerDestination.HISTORY.name) {
            HistoryUI(modifier = Modifier.fillMaxSize())
        }
        composable(route = RouteTrackerDestination.FEED.name) {
            FeedUI(modifier = Modifier.fillMaxSize())
        }
        composable(route = RouteTrackerDestination.SETTINGS.name) {
            SettingsUI(
                modifier = Modifier.fillMaxSize(),
                authenticationViewModel = authViewModel,
                onLoggedOut = {
                    navController.navigate(RouteTrackerDestination.LOGIN.name) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
    }
}
