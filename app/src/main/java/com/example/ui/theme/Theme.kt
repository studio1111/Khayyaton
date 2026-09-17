package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    primaryContainer = Blue700,
    onPrimaryContainer = Blue100,
    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald600,
    onSecondaryContainer = Emerald100,
    tertiary = Amber500,
    background = Slate950,
    surface = Slate900,
    surfaceVariant = Slate800,
    onBackground = Slate50,
    onSurface = Slate100,
    onSurfaceVariant = Slate300,
    outline = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF047857),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF0F172A),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF94A3B8)
)

private val PurpleGlassColorScheme = darkColorScheme(
    primary = PurpleAccent,
    onPrimary = Color.White,
    secondary = Emerald400,
    onSecondary = Color.White,
    tertiary = Amber500,
    background = PurpleBg,
    surface = PurpleSurface,
    surfaceVariant = Color(0xFF2C1C4F),
    onBackground = Color(0xFFF3E8FF),
    onSurface = Color(0xFFF3E8FF),
    onSurfaceVariant = Color(0xFFD8B4FE),
    outline = Color(0xFF4C2889)
)

private val BlueGlassColorScheme = darkColorScheme(
    primary = NavyAccent,
    onPrimary = Slate950,
    secondary = Emerald400,
    onSecondary = Color.White,
    tertiary = Amber500,
    background = NavyBg,
    surface = NavySurface,
    surfaceVariant = Color(0xFF132A4F),
    onBackground = Color(0xFFE0F2FE),
    onSurface = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF7DD3FC),
    outline = Color(0xFF1E3A8A)
)

private val NeonGlassColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF031E11),
    primaryContainer = NeonGreenContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = NeonYellow,
    onSecondary = Color(0xFF2A1C00),
    secondaryContainer = NeonYellowContainer,
    onSecondaryContainer = Color(0xFFFEF08A),
    tertiary = NeonYellowGlow,
    background = NeonGlassBg,
    surface = NeonGlassSurface,
    surfaceVariant = NeonGlassSurfaceVariant,
    onBackground = Color(0xFFF0FDF4),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF86EFAC),
    outline = NeonGlassOutline
)

@Composable
fun KhayyatonTheme(
    themeMode: AppThemeMode = AppThemeMode.NEON_GLASS,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.NEON_GLASS -> NeonGlassColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.GLASS_PURPLE -> PurpleGlassColorScheme
        AppThemeMode.GLASS_BLUE -> BlueGlassColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
