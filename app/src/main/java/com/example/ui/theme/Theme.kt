package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicSlateColorScheme = darkColorScheme(
    primary = CosmicSlatePrimary,
    secondary = CosmicSlateSecondary,
    tertiary = CosmicSlateAccent,
    background = CosmicSlateBg,
    surface = CosmicSlateSurface,
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF1F5F9)
)

private val EmeraldStudyColorScheme = darkColorScheme(
    primary = EmeraldStudyPrimary,
    secondary = EmeraldStudySecondary,
    tertiary = EmeraldStudyAccent,
    background = EmeraldStudyBg,
    surface = EmeraldStudySurface,
    onPrimary = Color(0xFF022C22),
    onSecondary = Color(0xFF022C22),
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFD1FAE5)
)

private val LavenderChillColorScheme = darkColorScheme(
    primary = LavenderChillPrimary,
    secondary = LavenderChillSecondary,
    tertiary = LavenderChillAccent,
    background = LavenderChillBg,
    surface = LavenderChillSurface,
    onPrimary = Color(0xFF1E1B4B),
    onSecondary = Color(0xFF1E1B4B),
    onBackground = Color(0xFFEEF2FF),
    onSurface = Color(0xFFE0E7FF)
)

private val ClassicDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkAccent,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFE2E8F0)
)

private val ClassicLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightAccent,
    background = LightBg,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
)

private val BentoGridColorScheme = lightColorScheme(
    primary = BentoPrimary,
    secondary = BentoSecondary,
    tertiary = BentoAccent,
    background = BentoBg,
    surface = BentoSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = BentoOnBackground,
    onSurface = BentoOnSurface
)

@Composable
fun MyApplicationTheme(
    themeName: String = "Bento Grid",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Bento Grid" -> BentoGridColorScheme
        "Cosmic Slate" -> CosmicSlateColorScheme
        "Emerald Study" -> EmeraldStudyColorScheme
        "Lavender Chill" -> LavenderChillColorScheme
        "Dark Mode" -> ClassicDarkColorScheme
        "Light Mode" -> ClassicLightColorScheme
        else -> BentoGridColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
