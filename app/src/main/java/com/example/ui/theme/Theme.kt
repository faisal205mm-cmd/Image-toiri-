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
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = MinimalistPrimaryContainer,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = MinimalistSecondaryContainer,
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = MinimalistTertiaryContainer,
    background = CleanDarkBg,
    onBackground = CleanDarkOnSurface,
    surface = CleanDarkSurface,
    onSurface = CleanDarkOnSurface,
    surfaceVariant = CleanDarkSurfaceVariant,
    onSurfaceVariant = CleanDarkOnSurfaceVariant,
    outline = CleanDarkOutline,
    outlineVariant = CleanDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalistPrimary,
    onPrimary = Color.White,
    primaryContainer = MinimalistPrimaryContainer,
    onPrimaryContainer = MinimalistOnPrimaryContainer,
    secondary = MinimalistSecondary,
    onSecondary = Color.White,
    secondaryContainer = MinimalistSecondaryContainer,
    onSecondaryContainer = MinimalistOnSecondaryContainer,
    tertiary = MinimalistTertiary,
    onTertiary = Color.White,
    tertiaryContainer = MinimalistTertiaryContainer,
    onTertiaryContainer = MinimalistOnTertiaryContainer,
    background = CleanLightBg,
    onBackground = CleanLightOnSurface,
    surface = CleanLightSurface,
    onSurface = CleanLightOnSurface,
    surfaceVariant = CleanLightSurfaceVariant,
    onSurfaceVariant = CleanLightOnSurfaceVariant,
    outline = CleanLightOutline,
    outlineVariant = CleanLightOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our tailored fashion business theme
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
