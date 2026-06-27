package com.uilab.showcase.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color roles. Components reference roles (e.g. [accent]), never raw hex.
 * This is the contract every component renders against.
 */
@Immutable
data class LabColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val indicator: Color,
    /** Hairline grid / baseline behind data graphics. */
    val chartGrid: Color,
    /** Categorical palette for multi-series graphics (donut slices, legends). */
    val chartPalette: List<Color>,
)

val LightLabColors = LabColors(
    background = Color(0xFFF5F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEDEEF6), // a step off white, so it reads above a white surface

    onSurface = Color(0xFF1B1B1F),
    onSurfaceMuted = Color(0xFF9094A1),
    accent = Color(0xFF5B5BD6),
    onAccent = Color(0xFFFFFFFF),
    indicator = Color(0x335B5BD6), // accent tint (~20% alpha) for a readable morph
    chartGrid = Color(0xFFE6E8F1),
    chartPalette = listOf(
        Color(0xFF5B5BD6), // indigo (accent)
        Color(0xFF2BB8A3), // teal
        Color(0xFFE8A13A), // amber
        Color(0xFFE2557B), // rose
        Color(0xFF4C8DF6), // blue
        Color(0xFF9A6CF0), // violet
    ),
)

val DarkLabColors = LabColors(
    background = Color(0xFF0E0E12),
    surface = Color(0xFF17171E),
    surfaceElevated = Color(0xFF20202A),
    onSurface = Color(0xFFF2F2F5),
    onSurfaceMuted = Color(0xFF82869A),
    accent = Color(0xFF9A9AF5),
    onAccent = Color(0xFF12121A),
    indicator = Color(0x3D9A9AF5), // accent tint (~24% alpha) for a readable morph
    chartGrid = Color(0xFF2A2A36),
    chartPalette = listOf(
        Color(0xFF9A9AF5), // indigo (accent)
        Color(0xFF4FD1BC), // teal
        Color(0xFFF2B66B), // amber
        Color(0xFFF4789B), // rose
        Color(0xFF79ABFF), // blue
        Color(0xFFB79BF5), // violet
    ),
)
