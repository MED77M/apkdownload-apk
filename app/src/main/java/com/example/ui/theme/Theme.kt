package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SchoolDarkPrimary,
    onPrimary = Color(0xFF001B3E),
    primaryContainer = Color(0xFF0A2B49),
    onPrimaryContainer = SchoolPrimaryContainer,
    secondary = SchoolDarkSecondary,
    onSecondary = Color(0xFF410002),
    secondaryContainer = Color(0xFF6B0B0E),
    onSecondaryContainer = SchoolSecondaryContainer,
    tertiary = SchoolTertiary,
    background = SchoolDarkBackground,
    onBackground = Color(0xFFE2E8F0),
    surface = SchoolDarkSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = SchoolDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF64748B)
)

private val LightColorScheme = lightColorScheme(
    primary = SchoolPrimary,
    onPrimary = Color.White,
    primaryContainer = SchoolPrimaryContainer,
    onPrimaryContainer = SchoolOnPrimaryContainer,
    secondary = SchoolSecondary,
    onSecondary = Color.White,
    secondaryContainer = SchoolSecondaryContainer,
    onSecondaryContainer = SchoolOnSecondaryContainer,
    tertiary = SchoolTertiary,
    background = SchoolBackground,
    onBackground = SchoolTextPrimary,
    surface = SchoolSurface,
    onSurface = SchoolTextPrimary,
    surfaceVariant = SchoolSurfaceVariant,
    onSurfaceVariant = SchoolTextSecondary,
    outline = SchoolOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep official logo colors (Royal Blue + Cardinal Red) consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
