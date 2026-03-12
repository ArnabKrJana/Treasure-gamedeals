package com.example.treasure

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.treasure.data.repositoryImpl.AppTheme
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.ui.navigationGraphs.RootNavigationGraph
import com.example.treasure.ui.theme.TreasureTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 2. Observe the Settings
            val theme by settingsRepository.appTheme.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
            val dynamicColorEnabled by settingsRepository.isDynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)

            // 3. Determine Dark Mode logic
            val isDarkTheme = when(theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            // 4. Pass 'dynamicColor' to TreasureTheme
            TreasureTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColorEnabled // Make sure your Theme.kt accepts this!
            ) {
                val context = LocalContext.current

                // --- PERMISSION LOGIC START ---
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        Log.d("MainActivity", "Permission Result: $isGranted")
                        // No specific action needed here as HomeScreen handles its own retries/loading,
                        // but completing the dialog allows the UI to resume properly.
                    }
                )

                LaunchedEffect(Unit) {
                    // Small delay to allow the initial composition and data fetch to start
                    // before blocking with a system dialog
                    delay(500)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permission = Manifest.permission.POST_NOTIFICATIONS
                        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(permission)
                        }
                    }
                }
                // --- PERMISSION LOGIC END ---

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Pass innerPadding to avoid content overlapping with system bars if needed,
                    // or just use your existing graph logic.
                    RootNavigationGraph(rootNavController = rememberNavController())
                }
            }
        }
    }
}