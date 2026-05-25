package com.example.treasure.ui.navigationGraphs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.treasure.ui.screens.CartScreen
import com.example.treasure.ui.screens.MainScreenComposable
import com.example.treasure.ui.screens.NotificationScreen
import com.example.treasure.ui.screens.WelcomeScreen
import com.example.treasure.ui.viewModels.AuthViewModel


@Composable
fun RootNavigationGraph(
    rootNavController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel() // Inject the AuthViewModel
) {
    // Observe session state
    val isSessionActive by authViewModel.isSessionActive.collectAsState()

    // --- THE EJECT SEAT ---
    LaunchedEffect(isSessionActive) {
        if (!isSessionActive) {
            rootNavController.navigate(RootGraphDestination.WelcomeScreenRoute) {
                popUpTo(0) { inclusive = true } // Wipe the backstack
                launchSingleTop = true
            }
        }
    }

    // --- DYNAMIC START DESTINATION ---
    val startDestination = if (isSessionActive) {
        RootGraphDestination.MainScreenRoute
    } else {
        RootGraphDestination.WelcomeScreenRoute
    }

    NavHost(navController = rootNavController, startDestination = startDestination) {

        // --- WELCOME SCREEN ---
        composable<RootGraphDestination.WelcomeScreenRoute> {
            WelcomeScreen(
                onNavigateToHome = {
                    rootNavController.navigate(RootGraphDestination.MainScreenRoute) {
                        popUpTo(RootGraphDestination.WelcomeScreenRoute) { inclusive = true }
                    }
                }
            )
        }

        // --- MAIN SCREEN (Bottom Bar) ---
        composable<RootGraphDestination.MainScreenRoute> {
            MainScreenComposable(rootNavController)
        }

        // --- NOTIFICATIONS ---
        composable<RootGraphDestination.NotificationRoute> {
            NotificationScreen {
                rootNavController.popBackStack()
            }
        }

        // --- CART ---
        composable<RootGraphDestination.CartRoute> {
            CartScreen()
        }
    }
}