package com.example.laketownturf.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LTTDarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = TextOnPrimary,
    primaryContainer = DarkGreen,
    onPrimaryContainer = GreenTint,
    secondary = AmberCTA,
    onSecondary = DarkNavyVariant,
    secondaryContainer = AmberDark,
    onSecondaryContainer = AmberLight,
    tertiary = PrimaryGreen,
    onTertiary = TextOnPrimary,
    error = DangerRed,
    onError = TextOnPrimary,
    errorContainer = DangerSurface,
    onErrorContainer = DangerRed,
    background = DarkNavy,
    onBackground = TextPrimary,
    surface = DarkNavySurface,
    onSurface = TextPrimary,
    surfaceVariant = CardBackgroundDark,
    onSurfaceVariant = TextSecondary,
    outline = CardBorderDark,
    outlineVariant = Divider,
)

private val LTTLightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = TextOnPrimary,
    primaryContainer = GreenTint,
    onPrimaryContainer = DarkGreen,
    secondary = AmberCTA,
    onSecondary = LightVariant,
    secondaryContainer = AmberLight,
    onSecondaryContainer = AmberDark,
    tertiary = PrimaryGreen,
    onTertiary = TextOnPrimary,
    error = DangerRed,
    onError = TextOnPrimary,
    errorContainer = DangerRed.copy(alpha = 0.1f),
    onErrorContainer = DangerRed,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardBackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = CardBorderLight,
    outlineVariant = CardBorderLight,
)

private val LTTShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun LakeTownTurfTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isDarkTheme) LTTDarkColorScheme else LTTLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LTTTypography,
        shapes = LTTShapes,
        content = content,
    )
}
