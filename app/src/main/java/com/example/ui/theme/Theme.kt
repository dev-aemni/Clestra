package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClestraLightColorScheme = lightColorScheme(
    primary = ClestraCopper,
    onPrimary = Color.White,
    primaryContainer = ClestraBubbleSent,
    onPrimaryContainer = ClestraCharcoal,
    secondary = ClestraSand,
    onSecondary = ClestraCharcoal,
    secondaryContainer = ClestraWarmBeige,
    onSecondaryContainer = ClestraCharcoal,
    tertiary = ClestraCopperDark,
    onTertiary = ClestraCream,
    tertiaryContainer = ClestraLightSkin,
    onTertiaryContainer = ClestraCharcoal,
    background = ClestraCream,
    onBackground = ClestraCharcoal,
    surface = ClestraBubbleRecv,
    onSurface = ClestraCharcoal,
    surfaceVariant = ClestraWarmBeige,
    onSurfaceVariant = ClestraTextSoft,
    outline = ClestraTextSoft,
    error = ClestraError,
    onError = Color.White
)

private val ClestraDarkColorScheme = darkColorScheme(
    primary = ClestraCopper,
    onPrimary = Color.White,
    primaryContainer = ClestraCopperDark,
    onPrimaryContainer = ClestraCream,
    secondary = ClestraSand,
    onSecondary = ClestraCharcoal,
    background = ClestraCharcoal,
    onBackground = ClestraCream,
    surface = Color(0xFF36281F),
    onSurface = ClestraCream,
    surfaceVariant = Color(0xFF47362B),
    onSurfaceVariant = ClestraSand,
    outline = ClestraTextSoft,
    error = ClestraError,
    onError = Color.White
)

@Composable
fun ClestraTheme(
    darkTheme: Boolean = false, // Follow the Cream/Charcoal/Copper XML theme by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ClestraDarkColorScheme else ClestraLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ClestraTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
