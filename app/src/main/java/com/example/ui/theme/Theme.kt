package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PeachPrimaryDark,
    onPrimary = PeachOnPrimaryDark,
    primaryContainer = PeachPrimaryContainerDark,
    onPrimaryContainer = PeachOnPrimaryContainerDark,
    secondary = PeachSecondaryDark,
    onSecondary = PeachOnSecondaryDark,
    secondaryContainer = PeachSecondaryContainerDark,
    onSecondaryContainer = PeachOnSecondaryContainerDark,
    tertiary = PeachTertiaryDark,
    onTertiary = PeachOnTertiaryDark,
    tertiaryContainer = PeachTertiaryContainerDark,
    onTertiaryContainer = PeachOnTertiaryContainerDark,
    background = PeachBackgroundDark,
    onBackground = PeachOnBackgroundDark,
    surface = PeachSurfaceDark,
    onSurface = PeachOnSurfaceDark,
    surfaceVariant = PeachSurfaceVariantDark,
    onSurfaceVariant = PeachOnSurfaceVariantDark,
    outline = PeachOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PeachPrimaryLight,
    onPrimary = PeachOnPrimaryLight,
    primaryContainer = PeachPrimaryContainerLight,
    onPrimaryContainer = PeachOnPrimaryContainerLight,
    secondary = PeachSecondaryLight,
    onSecondary = PeachOnSecondaryLight,
    secondaryContainer = PeachSecondaryContainerLight,
    onSecondaryContainer = PeachOnSecondaryContainerLight,
    tertiary = PeachTertiaryLight,
    onTertiary = PeachOnTertiaryLight,
    tertiaryContainer = PeachTertiaryContainerLight,
    onTertiaryContainer = PeachOnTertiaryContainerLight,
    background = PeachBackgroundLight,
    onBackground = PeachOnBackgroundLight,
    surface = PeachSurfaceLight,
    onSurface = PeachOnSurfaceLight,
    surfaceVariant = PeachSurfaceVariantLight,
    onSurfaceVariant = PeachOnSurfaceVariantLight,
    outline = PeachOutlineLight
)

@Composable
fun ClestraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature Peach/Orange theme active
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ClestraTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
