package com.huzeifa.flipline.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FliplineDarkColors = darkColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED)
)

@Composable
fun FliplineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FliplineDarkColors,
        content = content
    )
}
