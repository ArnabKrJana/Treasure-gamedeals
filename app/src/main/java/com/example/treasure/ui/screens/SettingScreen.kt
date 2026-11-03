package com.example.treasure.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.treasure.data.repositoryImpl.AppTheme
import com.example.treasure.ui.theme.TreasureTheme
import com.example.treasure.ui.viewModels.SettingsUiState
import com.example.treasure.ui.viewModels.SettingsViewModel
import com.example.treasure.utils.Constants.WEB_CLIENT_ID
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

/**
 * Stateful Composable screen hosting the settings, preferences, appearance choices,
 * background sync intervals, and Google Drive account connectivity.
 *
 * @param viewModel Injected [SettingsViewModel] coordinating user preferences.
 */
@Composable
fun SettingScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingScreenContent(
        state = state,
        onThemeChange = viewModel::updateTheme,
        onDynamicColorChange = viewModel::toggleDynamicColors,
        onNotificationChange = viewModel::toggleNotifications,
        onSyncFrequencyChange = viewModel::updateSyncFrequency,
        onThresholdChange = viewModel::updateNotifyThreshold,
        onClearCache = viewModel::clearImageCache,
        onLinkDrive = viewModel::linkDriveAccount,
        onUnlinkDrive = viewModel::unlinkDrive,
        onAuthFailed = viewModel::setDriveNeedsAuth
    )
}

/**
 * Stateless UI container rendering categorized settings sections.
 *
 * @param state Consolidated [SettingsUiState] containing active user preferences.
 * @param onThemeChange Invoked when a new theme mode is chosen.
 * @param onDynamicColorChange Invoked when dynamic colors setting is toggled.
 * @param onNotificationChange Invoked when notifications setting is toggled.
 * @param onSyncFrequencyChange Invoked when background sync frequency is updated.
 * @param onThresholdChange Invoked when price drop alert threshold is changed.
 * @param onClearCache Invoked to evict image cache.
 * @param onLinkDrive Invoked with Google OAuth server authorization code to link Drive.
 * @param onUnlinkDrive Invoked to disconnect Google Drive.
 * @param onAuthFailed Invoked when Google Drive authorization fails or is cancelled.
 */
@Composable
fun SettingScreenContent(
    state: SettingsUiState,
    onThemeChange: (AppTheme) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onSyncFrequencyChange: (Int) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onClearCache: () -> Unit,
    onLinkDrive: (String) -> Unit,
    onUnlinkDrive: () -> Unit,
    onAuthFailed: () -> Unit
) {
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showUnlinkDialog by remember { mutableStateOf(false) }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.serverAuthCode?.let { code -> onLinkDrive(code) } ?: onAuthFailed()
            } catch (_: ApiException) {
                onAuthFailed()
            }
        } else {
            onAuthFailed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSectionCard(title = "Appearance") {
            SettingsClickableItem(
                title = "App Theme",
                subtitle = state.theme.name.lowercase().replaceFirstChar { it.uppercase() },
                icon = Icons.Outlined.Palette,
                onClick = { showThemeDialog = true }
            )

            SettingsSwitchItem(
                title = "Dynamic Colors",
                subtitle = "Use system wallpaper colors (Android 12+)",
                icon = Icons.Outlined.FormatPaint,
                checked = state.dynamicColors,
                onCheckedChange = onDynamicColorChange
            )
        }

        SettingsSectionCard(title = "Cloud Sync") {
            SettingsClickableItem(
                title = "Google Drive",
                subtitle = if (state.isDriveLinked) "Connected. Tap to disconnect." else "Not connected. Tap to link.",
                icon = if (state.isDriveLinked) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                onClick = {
                    if (state.isDriveLinked) {
                        showUnlinkDialog = true
                    } else {
                        driveAuthLauncher.launch(getDriveSyncSettingsIntent(context))
                    }
                }
            )
        }

        SettingsSectionCard(title = "Wishlist Alerts") {
            SettingsSwitchItem(
                title = "Price Drop Notifications",
                subtitle = "Get alerted when wishlist games go on sale",
                icon = Icons.Outlined.Notifications,
                checked = state.notificationsEnabled,
                onCheckedChange = onNotificationChange
            )

            if (state.notificationsEnabled) {
                SettingsClickableItem(
                    title = "Sync Frequency",
                    subtitle = "Check for prices every ${state.syncFrequency} hours",
                    icon = Icons.Outlined.Sync,
                    onClick = { showFrequencyDialog = true }
                )

                SettingsClickableItem(
                    title = "Notification Threshold",
                    subtitle = "Only alert me if price drops by at least ${state.notifyThreshold}%",
                    icon = Icons.AutoMirrored.Outlined.TrendingDown,
                    onClick = { showThresholdDialog = true }
                )
            }
        }

        SettingsSectionCard(title = "Data & Storage") {
            SettingsClickableItem(
                title = "Clear Image Cache",
                subtitle = "Free up space by deleting cached thumbnails",
                icon = Icons.Outlined.DeleteOutline,
                onClick = onClearCache
            )
        }

        SettingsSectionCard(title = "About") {
            SettingsClickableItem(
                title = "Version",
                subtitle = state.version,
                icon = Icons.Outlined.Info,
                onClick = { /* Informational only */ }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("Disconnect Google Drive?") },
            text = { Text("Your wallpapers will no longer sync to the cloud. You can reconnect at any time from this menu.") },
            confirmButton = {
                TextButton(onClick = {
                    onUnlinkDrive()
                    showUnlinkDialog = false
                }) { Text("Disconnect", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showThemeDialog) {
        val themeOptions = AppTheme.entries

        SelectionDialog(
            title = "Choose Theme",
            options = themeOptions,
            selectedOption = state.theme,
            onOptionSelected = onThemeChange,
            onDismiss = { showThemeDialog = false },
            labelProvider = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
        )
    }

    if (showFrequencyDialog) {
        SelectionDialog(
            title = "Sync Frequency",
            options = listOf(4, 8, 12, 24),
            selectedOption = state.syncFrequency,
            onOptionSelected = onSyncFrequencyChange,
            onDismiss = { showFrequencyDialog = false },
            labelProvider = { "Every $it hours" }
        )
    }

    if (showThresholdDialog) {
        SelectionDialog(
            title = "Alert Threshold",
            options = listOf(1, 3, 5, 10, 20),
            selectedOption = state.notifyThreshold,
            onOptionSelected = onThresholdChange,
            onDismiss = { showThresholdDialog = false },
            labelProvider = { "Drop of $it% or more" }
        )
    }
}

/**
 * Builds an explicit [Intent] requesting Google Drive OAuth permissions from the Settings screen.
 *
 * @param context Android context used to initialize Google Sign-In client.
 * @return Configured Google Sign-In [Intent].
 */
@Suppress("DEPRECATION")
fun getDriveSyncSettingsIntent(context: Context): Intent {
    val webClientId = WEB_CLIENT_ID

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
        .requestServerAuthCode(webClientId, true)
        .build()

    val client = GoogleSignIn.getClient(context, gso)
    return client.signInIntent
}

/**
 * Visual card container wrapping a related group of settings items.
 *
 * @param title Section title header text.
 * @param content Slot rendering children settings composables within the card.
 */
@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(content = content)
        }
    }
}

/**
 * Text header for a settings section.
 *
 * @param title Section header text.
 */
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp, end = 16.dp)
    )
}

/**
 * Clickable settings row displaying an icon, title, and descriptive subtitle value.
 *
 * @param title Primary title text.
 * @param subtitle Descriptive subtitle or active selection.
 * @param icon Leading image vector icon.
 * @param onClick Invoked when the row is tapped.
 */
@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Settings row equipped with a toggle switch control.
 *
 * @param title Primary title text.
 * @param subtitle Descriptive subtitle explaining the setting.
 * @param icon Leading image vector icon.
 * @param checked Whether the toggle switch is currently active.
 * @param onCheckedChange Callback invoked when the toggle state is flipped.
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Generic single-selection dialog presenting a list of choices using radio buttons.
 *
 * @param title Dialog heading title.
 * @param options List of selectable option models.
 * @param selectedOption Currently selected option.
 * @param onOptionSelected Invoked when a new option is chosen.
 * @param onDismiss Invoked when the dialog is dismissed without changes.
 * @param labelProvider Function converting an option model into a displayable string.
 */
@Composable
fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    labelProvider: (T) -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = labelProvider(option))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Preview Composable for [SettingScreenContent].
 */
@Preview(showBackground = true)
@Composable
fun SettingScreenPreview() {
    TreasureTheme {
        SettingScreenContent(
            state = SettingsUiState(
                theme = AppTheme.SYSTEM,
                dynamicColors = true,
                notificationsEnabled = true,
                syncFrequency = 8,
                notifyThreshold = 3,
                version = "1.0.0",
                isDriveLinked = false
            ),
            onThemeChange = {},
            onDynamicColorChange = {},
            onNotificationChange = {},
            onSyncFrequencyChange = {},
            onThresholdChange = {},
            onClearCache = {},
            onLinkDrive = {},
            onUnlinkDrive = {},
            onAuthFailed = {}
        )
    }
}
