package com.example.treasure.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.treasure.data.repositoryImpl.AppTheme
import com.example.treasure.ui.viewModels.SettingsUiState
import com.example.treasure.ui.viewModels.SettingsViewModel

// 1. Stateful Composable (Injects ViewModel)
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
        onClearCache = viewModel::clearImageCache
    )
}

// 2. Stateless Composable (Pure UI - Works in Preview)
@Composable
fun SettingScreenContent(
    state: SettingsUiState,
    onThemeChange: (AppTheme) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onSyncFrequencyChange: (Int) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onClearCache: () -> Unit
) {
    val scrollState = rememberScrollState()

    // --- STATE VARIABLES MOVED HERE ---
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
//            Text(
//                text = "Settings",
//                style = MaterialTheme.typography.headlineMedium,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(16.dp)
//            )

            SettingsSectionTitle("Appearance")

            SettingsItem(
                icon = Icons.Outlined.DarkMode,
                title = "App Theme",
                subtitle = state.theme.label,
                onClick = { showThemeDialog = true }
            )

            SettingsSwitchItem(
                icon = Icons.Outlined.Palette,
                title = "Dynamic Colors",
                subtitle = "Use game art for card backgrounds",
                checked = state.dynamicColors,
                onCheckedChange = onDynamicColorChange
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("Notifications & Sync")

            SettingsSwitchItem(
                icon = Icons.Outlined.Notifications,
                title = "Price Alerts",
                subtitle = "Get notified when prices drop",
                checked = state.notificationsEnabled,
                onCheckedChange = onNotificationChange
            )

            SettingsItem(
                icon = Icons.Outlined.Sync,
                title = "Sync Frequency",
                subtitle = "Every ${state.syncFrequency} hours",
                enabled = state.notificationsEnabled,
                onClick = { showSyncDialog = true }
            )

            SettingsItem(
                icon = Icons.Outlined.TrendingDown,
                title = "Notify Threshold",
                subtitle = "Price drop > ${state.notifyThreshold}%",
                enabled = state.notificationsEnabled,
                onClick = { showThresholdDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("System & Data")

            SettingsItem(
                icon = Icons.Outlined.Public,
                title = "Store Region",
                subtitle = "India (IN)",
                enabled = false,
                onClick = {}
            )

            SettingsItem(
                icon = Icons.Outlined.CleaningServices,
                title = "Clear Image Cache",
                subtitle = "Free up storage space",
                onClick = onClearCache
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "App Version ${state.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // --- DIALOGS ---
    if (showThemeDialog) {
        SelectionDialog(
            title = "Choose Theme",
            options = AppTheme.entries.toList(),
            selectedOption = state.theme,
            onOptionSelected = onThemeChange,
            onDismiss = { showThemeDialog = false },
            labelProvider = { it.label }
        )
    }

    if (showSyncDialog) {
        val frequencies = listOf(4, 8, 12, 24)
        SelectionDialog(
            title = "Sync Frequency",
            options = frequencies,
            selectedOption = state.syncFrequency,
            onOptionSelected = onSyncFrequencyChange,
            onDismiss = { showSyncDialog = false },
            labelProvider = { "Every $it hours" }
        )
    }

    if (showThresholdDialog) {
        val thresholds = listOf(1, 3, 5, 10, 20, 50)
        SelectionDialog(
            title = "Notification Threshold",
            options = thresholds,
            selectedOption = state.notifyThreshold,
            onOptionSelected = onThresholdChange,
            onDismiss = { showThresholdDialog = false },
            labelProvider = { "Drop > $it%" }
        )
    }
}

// --- PREVIEW ---
@Preview(showBackground = true)
@Composable
fun SettingScreenPreview() {
    SettingScreenContent(
        state = SettingsUiState(
            theme = AppTheme.SYSTEM,
            dynamicColors = true,
            notificationsEnabled = true,
            syncFrequency = 8,
            notifyThreshold = 5
        ),
        onThemeChange = {},
        onDynamicColorChange = {},
        onNotificationChange = {},
        onSyncFrequencyChange = {},
        onThresholdChange = {},
        onClearCache = {}
    )
}

// --- HELPER COMPOSABLES (Keep these at the bottom of the file) ---
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
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
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
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
