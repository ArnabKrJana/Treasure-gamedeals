package com.example.treasure.ui.uiComponents

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    NavigationBar {
        screenList.forEach { screen ->
            // Check if the current destination belongs to this tab's hierarchy
            val isSelected = currentDestination?.hierarchy?.any {
                it.hasRoute(screen.route::class)
            } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to avoid stacking
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
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