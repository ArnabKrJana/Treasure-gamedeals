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

/**
 * Top-level navigation graph governing top-level routes and global session lifecycles.
 *
 * Automatically monitors [AuthViewModel.isSessionActive]. If the session expires or the user
 * logs out / deletes their account, the entire backstack is immediately cleared and navigation
 * resets to [RootGraphDestination.WelcomeScreenRoute].
 *
 * @param rootNavController Root navigation controller managing top-level destination transitions.
 * @param authViewModel Injected [AuthViewModel] managing session authentication state.
 */
@Composable
fun RootNavigationGraph(
    rootNavController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isSessionActive by authViewModel.isSessionActive.collectAsState()

    LaunchedEffect(isSessionActive) {
        if (!isSessionActive) {
            rootNavController.navigate(RootGraphDestination.WelcomeScreenRoute) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val startDestination = if (isSessionActive) {
        RootGraphDestination.MainScreenRoute
    } else {
        RootGraphDestination.WelcomeScreenRoute
    }

    NavHost(navController = rootNavController, startDestination = startDestination) {
        composable<RootGraphDestination.WelcomeScreenRoute> {
            WelcomeScreen(
                onNavigateToHome = {
                    rootNavController.navigate(RootGraphDestination.MainScreenRoute) {
                        popUpTo(RootGraphDestination.WelcomeScreenRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<RootGraphDestination.MainScreenRoute> {
            MainScreenComposable(rootNavController)
        }

        composable<RootGraphDestination.NotificationRoute> {
            NotificationScreen {
                rootNavController.popBackStack()
            }
        }

        composable<RootGraphDestination.CartRoute> {
            CartScreen()
        }
    }
}
