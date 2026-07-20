package com.example.migo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary          = MigoViolet,
    onPrimary        = Color.White,
    primaryContainer = MigoVioletDeep,
    onPrimaryContainer = Color(0xFFD8D6FF),
    secondary        = MigoCyan,
    onSecondary      = Color(0xFF003D35),
    secondaryContainer = Color(0xFF004D42),
    onSecondaryContainer = MigoCyanSoft,
    tertiary         = MigoGold,
    onTertiary       = Color(0xFF3C2800),
    tertiaryContainer = Color(0xFF523800),
    onTertiaryContainer = Color(0xFFFFDEA0),
    background       = MigoDarkBg,
    onBackground     = Color(0xFFF0EEFF),
    surface          = MigoDarkSurface,
    onSurface        = Color(0xFFECEBFF),
    surfaceVariant   = MigoDarkSurfaceAlt,
    onSurfaceVariant = MigoDarkMuted,
    surfaceTint      = MigoViolet,
    outline          = MigoDarkOutline.copy(alpha = 0.7f),
    outlineVariant   = MigoDarkOutline,
    error            = MigoError,
    onError          = Color.White,
    errorContainer   = Color(0xFF4A1515),
    onErrorContainer = Color(0xFFFFB3B3),
    scrim            = Color.Black.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary          = MigoViolet,
    onPrimary        = Color.White,
    primaryContainer = MigoVioletContainer,
    onPrimaryContainer = MigoVioletDeep,
    secondary        = Color(0xFF008B7A),
    onSecondary      = Color.White,
    secondaryContainer = MigoCyanSoft,
    onSecondaryContainer = Color(0xFF003D35),
    tertiary         = Color(0xFFE0820C),
    onTertiary       = Color.White,
    tertiaryContainer = MigoWarningLight,
    onTertiaryContainer = Color(0xFF3C2800),
    background       = MigoLightBackground,
    onBackground     = MigoLightInk,
    surface          = MigoLightSurface,
    onSurface        = MigoLightInk,
    surfaceVariant   = MigoLightSurfaceAlt,
    onSurfaceVariant = MigoLightMuted,
    surfaceTint      = MigoViolet,
    outline          = MigoLightOutline.copy(alpha = 0.6f),
    outlineVariant   = MigoLightOutline,
    error            = Color(0xFFD32F2F),
    onError          = Color.White,
    errorContainer   = MigoErrorLight,
    onErrorContainer = Color(0xFF8B1C1C),
    scrim            = Color.Black.copy(alpha = 0.3f)
)

private val MigoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(18.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MigoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = MigoTypography,
        shapes      = MigoShapes,
        content     = content
    )
}
