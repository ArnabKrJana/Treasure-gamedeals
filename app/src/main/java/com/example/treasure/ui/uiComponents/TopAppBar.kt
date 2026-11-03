package com.example.treasure.ui.uiComponents

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.treasure.data.local.entity.enums.Role
import com.example.treasure.domain.uiModels.User

/**
 * Top application bar component with dynamic scroll-linked styling, profile controls, and badges.
 *
 * Smoothly interpolates title opacity and action button pill backgrounds based on the
 * nested scroll offset provided by [scrollBehavior].
 *
 * @param title The text displayed as the primary app bar title.
 * @param canNavigateBack Whether to display an auto-mirrored back navigation icon.
 * @param isSettingScreen Flag indicating if the current screen is the Settings view (hiding notifications & profile).
 * @param notificationCount Number of unread alerts displayed on the notification bell badge.
 * @param currentUser The currently authenticated [User] profile, or null if guest / unauthenticated.
 * @param scrollBehavior Scroll behavior controller linking the app bar state to scrollable body content.
 * @param onBackBtnClick Invoked when the back navigation arrow is clicked.
 * @param onNotificationBtnClick Invoked when the notification bell action is clicked.
 * @param onLogoutClick Callback triggered when the user chooses "Log out" from the profile menu.
 * @param onDeleteAccountClick Callback triggered when the user confirms account deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarComponent(
    title: String,
    canNavigateBack: Boolean,
    isSettingScreen: Boolean,
    notificationCount: Int = 0,
    currentUser: User?,
    scrollBehavior: TopAppBarScrollBehavior,
    onBackBtnClick: () -> Unit,
    onNotificationBtnClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    var showProfileMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isScrolled = scrollBehavior.state.contentOffset < -40f

    val titleAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "TitleAlpha"
    )

    val floatingIconBg by animateColorAsState(
        targetValue = if (isScrolled) Color.Black.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "IconBg"
    )

    val floatingIconTint by animateColorAsState(
        targetValue = if (isScrolled) Color.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 500),
        label = "IconTint"
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha)
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onBackBtnClick,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(floatingIconBg, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = floatingIconTint
                    )
                }
            }
        },
        actions = {
            if (!isSettingScreen) {
                IconButton(
                    onClick = onNotificationBtnClick,
                    modifier = Modifier.background(
                        floatingIconBg,
                        CircleShape
                    )
                ) {
                    BadgedBox(
                        badge = {
                            if (notificationCount > 0) {
                                Badge {
                                    Text(text = if (notificationCount > 9) "9+" else notificationCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = floatingIconTint
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (currentUser != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(
                            onClick = { showProfileMenu = true },
                            modifier = Modifier.background(
                                floatingIconBg,
                                CircleShape
                            )
                        ) {
                            if (currentUser.profilePicture != null) {
                                AsyncImage(
                                    model = currentUser.profilePicture,
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(32.dp),
                                    tint = floatingIconTint
                                )
                            }
                        }

                        ProfileDropdownMenu(
                            expanded = showProfileMenu,
                            user = currentUser,
                            onDismiss = { showProfileMenu = false },
                            onLogoutClick = {
                                showProfileMenu = false
                                onLogoutClick()
                            },
                            onDeleteClick = {
                                showProfileMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    )

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccountClick()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

/**
 * Overflow dropdown menu anchored to the profile avatar icon, presenting user account info and actions.
 *
 * @param expanded Whether the dropdown menu is currently visible.
 * @param user The authenticated user data model.
 * @param onDismiss Invoked when the user dismisses the dropdown menu.
 * @param onLogoutClick Invoked when the logout action is tapped.
 * @param onDeleteClick Invoked when the delete account action is tapped.
 */
@Composable
private fun ProfileDropdownMenu(
    expanded: Boolean,
    user: User,
    onDismiss: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(240.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = user.fullName ?: "Gamer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        DropdownMenuItem(
            text = { Text("Log out", fontWeight = FontWeight.Medium) },
            onClick = onLogoutClick
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = "Delete Account",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            onClick = onDeleteClick
        )
    }
}

/**
 * Confirmation dialog shown before irrevocably deleting the user's account and personal cloud sync data.
 *
 * @param onConfirm Invoked when the destructive deletion action is confirmed.
 * @param onDismiss Invoked when the user cancels or dismisses the dialog.
 */
@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = "Delete Account?") },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to permanently delete your account? All your wishlist data, synced deals, and preferences will be lost immediately.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Learn more about our data deletion policy.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://your-treasure-app.com/privacy-policy")
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text("Delete Permanently") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

/**
 * Preview Composable for [TopAppBarComponent] in home screen configuration with dark/light themes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Phone - Light",
    widthDp = 360,
    heightDp = 100,
    showSystemUi = true
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
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
            onNotificationBtnClick = {},
            currentUser = User(
                id = 1234,
                email = "some1234@gmail.com",
                fullName = "Someone Doe",
                profilePicture = "https://images.unsplash.com/photo-1778392099969-e1799d7dd4ea",
                role = Role.USER.name
            ),
            onLogoutClick = {},
            onDeleteAccountClick = {}
        )
    }
}

/**
 * Preview Composable for [TopAppBarComponent] in detail screen configuration with back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Detail Screen",
    widthDp = 360,
    heightDp = 100
)
@Composable
fun TopAppBarPreview_Detail() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
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
            onNotificationBtnClick = {},
            currentUser = User(
                id = 1234,
                email = "some1234@gmail.com",
                fullName = "Someone Doe",
                profilePicture = "https://images.unsplash.com/photo-1778392099969-e1799d7dd4ea",
                role = Role.USER.name
            ),
            onLogoutClick = {},
            onDeleteAccountClick = {}
        )
    }
}
