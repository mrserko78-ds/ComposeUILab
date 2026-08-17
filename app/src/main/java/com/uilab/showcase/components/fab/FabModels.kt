package com.uilab.showcase.components.fab

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A single action revealed when the FAB expands. Designed as a library-style public model. */
@Immutable
data class FabAction(
    val id: String,
    val icon: ImageVector,
    val label: String,
)

/**
 * Geometry knobs for [LabMorphingFab].
 *
 * The button morphs between a [collapsedSize] circle and an [expandedWidth]-wide panel
 * whose height follows the number of actions; everything in between is interpolated
 * from a single spring so shape, color and stagger never drift out of sync.
 */
@Immutable
data class MorphingFabStyle(
    /** Diameter of the collapsed button; also the size of the close button when expanded. */
    val collapsedSize: Dp = 56.dp,
    /** Width of the expanded panel. */
    val expandedWidth: Dp = 220.dp,
    /** Height of each action row inside the panel. */
    val actionHeight: Dp = 48.dp,
    /** Corner radius of the expanded panel; the collapsed state is always a circle. */
    val expandedCornerRadius: Dp = 22.dp,
)
