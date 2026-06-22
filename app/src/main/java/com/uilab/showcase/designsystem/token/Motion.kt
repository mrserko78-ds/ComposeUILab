package com.uilab.showcase.designsystem.token

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable

/**
 * Central motion language. Every component pulls its durations / easings from here,
 * so the whole library moves with one consistent personality.
 */
@Immutable
data class LabMotion(
    val fast: Int = 150,
    val medium: Int = 280,
    val slow: Int = 480,
    val standard: Easing = FastOutSlowInEasing,
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)
