package io.trialwatch.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFFC8FF4D),
    onPrimary = Color(0xFF0B0B0C),
    secondary = Color(0xFFFFB648),
    background = Color(0xFF0B0B0C),
    onBackground = Color(0xFFEDEDF0),
    surface = Color(0xFF141416),
    onSurface = Color(0xFFEDEDF0),
    surfaceVariant = Color(0xFF1A1A1F),
    onSurfaceVariant = Color(0xFF8B8B95),
    outline = Color(0xFF26262A)
)

@Composable
fun TrialwatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
