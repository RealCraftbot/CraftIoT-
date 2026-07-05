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
    primary = CraftCobaltBlue,
    secondary = CraftLavender,
    tertiary = CraftNeonMint,
    background = CraftDarkNavy,
    surface = CraftSurfaceDark,
    onPrimary = CraftPureWhite,
    onSecondary = CraftDarkNavy,
    onBackground = CraftPureWhite,
    onSurface = CraftPureWhite,
    error = CraftDangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = CraftCobaltBlue,
    secondary = CraftLavender,
    tertiary = CraftBrightLime,
    background = CraftSurfaceLight,
    surface = CraftPureWhite,
    onPrimary = CraftPureWhite,
    onSecondary = CraftDarkNavy,
    onBackground = CraftDarkNavy,
    onSurface = CraftDarkNavy,
    error = CraftDangerRed
)

@Composable
fun CraftIoTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable system dynamic color to force our premium theme
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

// Keep a backward compatible alias to prevent compilation breaks elsewhere
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CraftIoTTheme(darkTheme = darkTheme, content = content)
}
