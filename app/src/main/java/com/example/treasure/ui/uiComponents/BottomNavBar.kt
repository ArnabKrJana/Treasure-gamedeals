package com.example.treasure.ui.uiComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.treasure.utils.navigation.MainScreen

@Composable
fun BottomNavBar(navController: NavHostController) {
    val screenList = listOf(
        MainScreen.Home,
        MainScreen.Search,
        MainScreen.Wishlist,
        MainScreen.Setting
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination


    Box(
        modifier = Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surface
                )
            )
        )
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            screenList.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.hasRoute(screen.route::class)
                } == true

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (isSelected) screen.selectedIcon else screen.unselectedIcon
                            ),
                            contentDescription = screen.title,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(text = screen.title) }
                )
            }
        }
    }
}