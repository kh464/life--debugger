package com.lifedbg.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeDebuggerColors = lightColorScheme(
    primary = Color(0xFF14342B),
    secondary = Color(0xFF7B4B2A),
    tertiary = Color(0xFF2F6F73),
    background = Color(0xFFF5F1E8),
    surface = Color(0xFFFFFCF4),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1D211F),
    onSurface = Color(0xFF1D211F),
)

@Composable
fun LifeDebuggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LifeDebuggerColors,
        content = content,
    )
}
