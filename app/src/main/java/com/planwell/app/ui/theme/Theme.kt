package com.planwell.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentGreen,
    onSecondary = Color.White,
    tertiary = WarningAmber,
    error = DangerRed,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = TextPrimary,
    secondary = AccentGreenDark,
    onSecondary = TextPrimary,
    tertiary = WarningAmberDark,
    error = DangerRedDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerColorDark,
)

@Composable
fun PlanWellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colour off: emulator GPU path previously rendered Compose as a black frame.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // dynamicColor reserved for a later Material You pass; ignored for now.
    @Suppress("UNUSED_PARAMETER")
    val ignoredDynamic = dynamicColor
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
