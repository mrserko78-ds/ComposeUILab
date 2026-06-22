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
)

val LightLabColors = LabColors(
    background = Color(0xFFF5F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    onSurfaceMuted = Color(0xFF9094A1),
    accent = Color(0xFF5B5BD6),
    onAccent = Color(0xFFFFFFFF),
    indicator = Color(0x335B5BD6), // accent tint (~20% alpha) for a readable morph
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
)
