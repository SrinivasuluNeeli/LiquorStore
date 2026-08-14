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
    primary = TechPurpleContainer,
    onPrimary = TechOnPurpleContainer,
    primaryContainer = TechPurplePrimary,
    onPrimaryContainer = Color.White,
    secondary = TechBlueAccent,
    onSecondary = TechOnBlueAccent,
    background = Color(0xFF141218),
    surface = Color(0xFF211F26),
    surfaceVariant = Color(0xFF2B2930),
    onBackground = Color(0xFFE6E0E9),
    onSurface = Color(0xFFE6E0E9),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = TechErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = TechPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = TechPurpleContainer,
    onPrimaryContainer = TechOnPurpleContainer,
    secondary = TechBlueAccent,
    onSecondary = TechOnBlueAccent,
    background = TechBgLight,
    surface = TechSurfaceLight,
    surfaceVariant = TechSurfaceVariant,
    onBackground = TechTextPrimary,
    onSurface = TechTextPrimary,
    onSurfaceVariant = TechTextSecondary,
    outline = TechOutline,
    outlineVariant = TechOutlineVariant,
    error = TechErrorRed,
    errorContainer = TechErrorContainer
)

@Composable
fun LiquorInventoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
