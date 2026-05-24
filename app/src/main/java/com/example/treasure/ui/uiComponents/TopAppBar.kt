package com.example.treasure.ui.uiComponents


import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

    // 1. Detect if the user has scrolled down (offset goes negative as you scroll down)
    val isScrolled = scrollBehavior.state.contentOffset < -40f

    // 2. Animate Title Opacity (Fades out when scrolled)
    val titleAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "TitleAlpha"
    )

    // 3. Animate Icon Backgrounds (Creates the floating circular pill effect over content)
    val floatingIconBg by animateColorAsState(
        targetValue = if (isScrolled) Color.Black.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "IconBg"
    )

    // Optional: Animate icon tint so they turn white when floating over a dark pill
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
                // Apply the fading alpha here
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha)
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onBackBtnClick,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(floatingIconBg, CircleShape) // Floating background
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
                // Notifications
                IconButton(
                    onClick = onNotificationBtnClick,
                    modifier = Modifier.background(
                        floatingIconBg,
                        CircleShape
                    ) // Floating background
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

                // Profile Menu
                if (currentUser != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(
                            onClick = { showProfileMenu = true },
                            modifier = Modifier.background(
                                floatingIconBg,
                                CircleShape
                            ) // Floating background
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
        // FORCE the background to always be transparent, no matter the scroll state
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

// ... [Keep your existing ProfileDropdownMenu and DeleteAccountDialog here] ...
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