package com.uilab.showcase.components.shimmer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tuning knobs for the shimmer sweep shared by every skeleton under one
 * [LabShimmerState]. Colors come from the theme ([LabColors.skeleton] and
 * [LabColors.skeletonHighlight]); this only shapes the motion.
 */
@Immutable
data class ShimmerStyle(
    /** Time for one band to travel across the host, edge to edge. */
    val sweepMillis: Int = 1300,
    /** Pause between sweeps, so the motion breathes instead of strobing. */
    val pauseMillis: Int = 350,
    /** Tilt of the band from vertical, in degrees. 0 is an upright band. */
    val angleDeg: Float = 20f,
    /** Width of the highlight band. */
    val bandWidth: Dp = 180.dp,
)
