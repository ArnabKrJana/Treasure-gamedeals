package com.example.treasure.ui.uiComponents

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarComponent(
    title: String,
    canNavigateBack: Boolean,
    isSettingScreen: Boolean,
    notificationCount: Int = 0,
    scrollBehavior: TopAppBarScrollBehavior,
    onBackBtnClick: () -> Unit,
    onNotificationBtnClick: () -> Unit
) {

    // Smoothly animate from Transparent to a 95% opaque surface color when scrolled
    val isOverlapping = scrollBehavior.state.overlappedFraction > 0.01f
    val animatedContainerColor by animateColorAsState(
        targetValue = if (isOverlapping) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 300),
        label = "TopBarColorAnimation"
    )

    TopAppBar(

        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },


        navigationIcon = {

            if (canNavigateBack) {

                IconButton(onClick = onBackBtnClick) {

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },

        actions = {

            if (!isSettingScreen) {

                IconButton(onClick = onNotificationBtnClick) {

                    BadgedBox(

                        badge = {

                            if (notificationCount > 0) {

                                Badge {

                                    Text(
                                        text =
                                            if (notificationCount > 9) "9+"
                                            else notificationCount.toString()
                                    )
                                }
                            }
                        }

                    ) {

                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications"
                        )
                    }
                }
            }
        },

        scrollBehavior = scrollBehavior,

        // Apply the animated color to both states so it relies strictly on our custom animation
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = animatedContainerColor,
            scrolledContainerColor = animatedContainerColor,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Phone - Light",
    widthDp = 360,
    heightDp = 100
)
@Preview(
    name = "Phone - Dark",
    widthDp = 360,
    heightDp = 100,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Tablet",
    widthDp = 800,
    heightDp = 100
)
@Preview(
    name = "Tablet - Dark",
    widthDp = 800,
    heightDp = 100,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun TopAppBarPreview_Home() {

    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(
            state = rememberTopAppBarState()
        )

    MaterialTheme {

        TopAppBarComponent(
            title = "Treasure",
            canNavigateBack = false,
            isSettingScreen = false,
            notificationCount = 4,
            scrollBehavior = scrollBehavior,
            onBackBtnClick = {},
            onNotificationBtnClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Detail Screen",
    widthDp = 360,
    heightDp = 100
)
@Composable
fun TopAppBarPreview_Detail() {

    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(
            state = rememberTopAppBarState()
        )

    MaterialTheme {

        TopAppBarComponent(
            title = "Game Details",
            canNavigateBack = true,
            isSettingScreen = false,
            notificationCount = 0,
            scrollBehavior = scrollBehavior,
            onBackBtnClick = {},
            onNotificationBtnClick = {}
        )
    }
}