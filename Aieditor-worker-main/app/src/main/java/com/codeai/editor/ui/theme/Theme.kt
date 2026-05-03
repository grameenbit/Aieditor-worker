package com.codeai.editor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val EditorBg = Color(0xFF1E1E1E)
val EditorSurface = Color(0xFF252526)
val EditorSurfaceVariant = Color(0xFF2D2D2D)
val EditorPrimary = Color(0xFF1E88E5)
val EditorSecondary = Color(0xFF00E676)
val EditorText = Color(0xFFD4D4D4)
val EditorTextDim = Color(0xFF808080)
val TerminalBg = Color(0xFF0D1117)
val TerminalText = Color(0xFFC9D1D9)
val ErrorRed = Color(0xFFCF6679)
val SuccessGreen = Color(0xFF4CAF50)

private val DarkColorScheme = darkColorScheme(
    primary = EditorPrimary,
    secondary = EditorSecondary,
    background = EditorBg,
    surface = EditorSurface,
    surfaceVariant = EditorSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = EditorText,
    onSurface = EditorText,
    error = ErrorRed
)

@Composable
fun CodeAITheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = EditorBg.toArgb()
            window.navigationBarColor = EditorBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
