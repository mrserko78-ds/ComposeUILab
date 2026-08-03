package com.uilab.showcase.components.cardstack

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Direction a card leaves the stack in. */
enum class SwipeDirection { Left, Right, Up, Down }

/**
 * Visual and physical tuning knobs for [LabCardStack].
 *
 * The defaults give a Tinder-like feel: three visible cards, a light peek of the
 * cards behind, and a swipe that commits at roughly a third of the container.
 */
@Immutable
data class CardStackStyle(
    /** How many cards are drawn behind the top one. */
    val visibleCards: Int = 3,
    /** Scale lost per depth level; the card at depth 1 renders at `1 - scaleStep`. */
    val scaleStep: Float = 0.05f,
    /** Vertical peek of each card behind the top one. */
    val peekOffset: Dp = 12.dp,
    /** Rotation of the top card at full horizontal drag, in degrees. */
    val maxRotationDeg: Float = 12f,
    /** Fraction of the container size the card must travel for a release to commit. */
    val swipeThreshold: Float = 0.35f,
    /** Release speed (per second) that commits a swipe even below [swipeThreshold]. */
    val flingVelocityThreshold: Dp = 900.dp,
)
