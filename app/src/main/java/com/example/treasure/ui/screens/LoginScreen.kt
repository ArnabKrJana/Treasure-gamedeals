package com.example.treasure.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.treasure.R

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onGoogleSignInClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo or Illustration
        Icon(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = "App Logo",
            modifier = Modifier.size(400.dp),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Treasure",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Discover hidden gems around you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Google Sign-In Button
        OutlinedButton(
            onClick = onGoogleSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            // You would typically use a Google Logo SVG here
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_set_as),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "By continuing, you agree to our Terms of Service",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen()
}