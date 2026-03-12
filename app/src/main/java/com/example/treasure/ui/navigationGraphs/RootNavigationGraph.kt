package com.example.treasure.ui.navigationGraphs

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.treasure.ui.screens.CartScreen
import com.example.treasure.ui.screens.MainScreenComposable
import com.example.treasure.ui.screens.NotificationScreen

@Composable
fun RootNavigationGraph(rootNavController: NavHostController) {
    NavHost(navController = rootNavController, startDestination = RootGraphDestination.MainScreenRoute){
        // The Main Screen (Bottom Bar Container)
        composable<RootGraphDestination.MainScreenRoute> {
            MainScreenComposable(rootNavController)
        }

        // Global Screens (Overlay on top of everything)
        composable<RootGraphDestination.NotificationRoute> {
            NotificationScreen {
                // Handle back click or specific action
                rootNavController.popBackStack()
            }
        }

        composable<RootGraphDestination.CartRoute> {
            CartScreen()
        }
    }
}