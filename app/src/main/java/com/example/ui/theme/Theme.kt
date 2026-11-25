package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = RoseContainer,
    onPrimaryContainer = RoseDeep,
    secondary = LavenderPrimary,
    onSecondary = Color.White,
    secondaryContainer = LavenderSoft,
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldSoft,
    onTertiaryContainer = Color(0xFF78350F),
    background = CreamBackground,
    onBackground = PlumInk,
    surface = CreamCard,
    onSurface = PlumInk,
    surfaceVariant = RoseBlush,
    onSurfaceVariant = PlumMuted,
    outline = SoftBorder,
    outlineVariant = Color(0xFFF9E6EE),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = DarkRosePrimary,
    onPrimary = Color(0xFF3E0518),
    primaryContainer = Color(0xFF5D132B),
    onPrimaryContainer = RoseContainer,
    secondary = Color(0xFFC084FC),
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = LavenderSoft,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = DarkPlumBg,
    onBackground = DarkCreamText,
    surface = DarkPlumSurface,
    onSurface = DarkCreamText,
    surfaceVariant = DarkPlumCard,
    onSurfaceVariant = Color(0xFFD9B8C7),
    outline = Color(0xFF573247)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun GiftForYouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
