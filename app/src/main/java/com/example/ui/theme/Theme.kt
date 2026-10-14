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
    primary = GeminiViolet,
    onPrimary = Color.White,
    primaryContainer = GeminiVioletDark,
    onPrimaryContainer = GeminiVioletLight,
    secondary = AiStudioBlue,
    onSecondary = Color.White,
    secondaryContainer = AiStudioBlueDark,
    onSecondaryContainer = AiStudioBlueLight,
    tertiary = GeminiAmber,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AiStudioBlue,
    onPrimary = Color.White,
    primaryContainer = AiStudioBlueLight,
    onPrimaryContainer = AiStudioBlueDark,
    secondary = GeminiViolet,
    onSecondary = Color.White,
    secondaryContainer = GeminiVioletLight,
    onSecondaryContainer = GeminiVioletDark,
    tertiary = GeminiAmber,
    onTertiary = Color.Black,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature consistent with Google AI Studio branding
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
