package com.example.treasure.ui.uiComponents

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Reusable two-tone informational row composable displaying a bold label alongside a styled value string.
 *
 * Used across game detail specification cards to format publishers, developers, release dates, and franchises.
 *
 * @param label The descriptive property key (e.g., "Developer:", "Release Date:").
 * @param value The corresponding property value text.
 * @param modifier Composable modifier for layout and padding customization.
 */
@Composable
fun DetailInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                append("$label ")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append(value)
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 2.dp)
    )
}
