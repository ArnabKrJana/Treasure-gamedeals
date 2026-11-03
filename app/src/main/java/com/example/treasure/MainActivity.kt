package com.example.treasure

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.treasure.data.repositoryImpl.AppTheme
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.ui.navigationGraphs.RootNavigationGraph
import com.example.treasure.ui.theme.TreasureTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Main application activity and single entry point hosting the Jetpack Compose hierarchy.
 *
 * Configures edge-to-edge transparent system bars, observes persistent theme preferences from
 * [SettingsRepository], prompts for Android 13+ runtime notification permissions, and launches
 * the top-level [RootNavigationGraph].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            val theme by settingsRepository.appTheme.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
            val dynamicColorEnabled by settingsRepository.isDynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)

            val isDarkTheme = when (theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            TreasureTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColorEnabled
            ) {
                val context = LocalContext.current

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        Log.d("MainActivity", "Permission Result: $isGranted")
                    }
                )

                LaunchedEffect(Unit) {
                    delay(500)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permission = Manifest.permission.POST_NOTIFICATIONS
                        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(permission)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootNavigationGraph(rootNavController = rememberNavController())
                }
            }
        }
    }
}
