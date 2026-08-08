package com.uilab.showcase.components.cardstack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for [LabCardStack].
 *
 * The stack walks a caller-owned, immutable list: cards are never removed, only
 * [topIndex] moves forward on a swipe and back on a [rewind]. Hoist it with
 * [rememberLabCardStackState] and drive it programmatically from buttons or tests.
 */
@Stable
class LabCardStackState(initialTopIndex: Int = 0) {

    /** Index of the card currently on top. Moves forward on swipe, back on [rewind]. */
    var topIndex by mutableIntStateOf(initialTopIndex)
        private set

    /** True when at least one card has been swiped away and can be brought back. */
    val canRewind: Boolean
        get() = topIndex > 0

    /**
     * Live drag progress of the top card, each axis in -1..1, where +-1 means the
     * release threshold is reached on that axis. Back cards scale and rise off this
     * value, so anything else (badges, tint overlays) can follow the finger too.
     */
    val dragProgress: Offset
        get() {
            val threshold = thresholdPx
            if (threshold.x <= 0f || threshold.y <= 0f) return Offset.Zero
            return Offset(
                (offset.value.x / threshold.x).coerceIn(-1f, 1f),
                (offset.value.y / threshold.y).coerceIn(-1f, 1f),
            )
        }

    /** Pixel offset of the top card; animated for drags, spring-backs and fly-outs. */
    internal val offset = Animatable(Offset.Zero, Offset.VectorConverter)

    /** 1 -> 0 progress of the rewind drop-in; active only while [enteringIndex] >= 0. */
    internal val enter = Animatable(0f)
    internal var enteringIndex by mutableIntStateOf(-1)

    /** +1 when the finger grabbed the top half of the card, -1 for the bottom half. */
    internal var rotationSign by mutableFloatStateOf(1f)

    internal var containerSize by mutableStateOf(IntSize.Zero)
    internal var thresholdFraction by mutableFloatStateOf(0.35f)
    internal var flingThresholdPx = 0f
    internal var itemCount = 0
    internal var onSwipedListener: ((Int, SwipeDirection) -> Unit)? = null
    internal var onStackEndListener: (() -> Unit)? = null
    private var stackEndNotified = false

    private val thresholdPx: Offset
        get() = Offset(
            containerSize.width * thresholdFraction,
            containerSize.height * thresholdFraction,
        )

    /**
     * Swipes the top card away in [direction], suspending until the fly-out animation
     * completes; the stack's `onSwiped` fires afterwards. No-op when the stack is
     * exhausted or not yet measured.
     */
    suspend fun swipe(direction: SwipeDirection) {
        if (topIndex >= itemCount || containerSize == IntSize.Zero) return
        flyOut(direction, initialVelocity = Offset.Zero)
    }

    /** Brings the last swiped card back with a springy drop-in. No-op when [canRewind] is false. */
    suspend fun rewind() {
        if (!canRewind) return
        stackEndNotified = false
        offset.snapTo(Offset.Zero)
        enter.snapTo(1f)
        val newTop = topIndex - 1
        // One atomic commit: the returning card first appears already lifted
        // (enter == 1) and the back cards still at their pre-rewind depths, so no
        // frame ever shows the card resting in place before the drop-in starts.
        Snapshot.withMutableSnapshot {
            enteringIndex = newTop
            topIndex = newTop
        }
        try {
            enter.animateTo(0f, ReenterSpring)
        } finally {
            enteringIndex = -1
        }
    }

    internal suspend fun settleRelease(velocity: Offset, allowed: Set<SwipeDirection>) {
        val direction = releaseDirection(velocity, allowed)
        if (direction != null) {
            flyOut(direction, velocity)
        } else {
            offset.animateTo(Offset.Zero, ReturnSpring)
        }
    }

    internal suspend fun flyOut(direction: SwipeDirection, initialVelocity: Offset) {
        offset.animateTo(flyTarget(direction, initialVelocity), FlyOutSpring, initialVelocity)
        val swiped = topIndex
        // snapTo resumes and the index bump runs with no suspension in between, so a
        // frame can never show the old top card back at rest before it is re-keyed.
        offset.snapTo(Offset.Zero)
        topIndex = swiped + 1
        onSwipedListener?.invoke(swiped, direction)
        if (topIndex >= itemCount && !stackEndNotified) {
            stackEndNotified = true
            onStackEndListener?.invoke()
        }
    }

    private fun releaseDirection(velocity: Offset, allowed: Set<SwipeDirection>): SwipeDirection? {
        val threshold = thresholdPx
        if (threshold.x <= 0f || threshold.y <= 0f) return null
        // A fast enough fling commits even below the positional threshold.
        if (max(abs(velocity.x), abs(velocity.y)) >= flingThresholdPx) {
            val byVelocity =
                if (abs(velocity.x) >= abs(velocity.y)) horizontal(velocity.x) else vertical(velocity.y)
            if (byVelocity in allowed) return byVelocity
        }
        val position = offset.value
        val overX = abs(position.x) / threshold.x
        val overY = abs(position.y) / threshold.y
        if (max(overX, overY) >= 1f) {
            val byPosition = if (overX >= overY) horizontal(position.x) else vertical(position.y)
            if (byPosition in allowed) return byPosition
        }
        return null
    }

    /** Off-screen point continuing the card's current trajectory (velocity first, else offset). */
    private fun flyTarget(direction: SwipeDirection, velocity: Offset): Offset {
        val width = containerSize.width.toFloat()
        val height = containerSize.height.toFloat()
        val distance = sqrt(width * width + height * height)
        val start = offset.value
        return when (direction) {
            SwipeDirection.Left, SwipeDirection.Right -> {
                val sign = if (direction == SwipeDirection.Right) 1f else -1f
                val slope = when {
                    abs(velocity.x) > 1f -> velocity.y / abs(velocity.x)
                    abs(start.x) > 1f -> start.y / abs(start.x)
                    else -> 0f
                }.coerceIn(-MaxFlySlope, MaxFlySlope)
                Offset(sign * distance, start.y + (distance - sign * start.x) * slope)
            }
            SwipeDirection.Up, SwipeDirection.Down -> {
                val sign = if (direction == SwipeDirection.Down) 1f else -1f
                val slope = when {
                    abs(velocity.y) > 1f -> velocity.x / abs(velocity.y)
                    abs(start.y) > 1f -> start.x / abs(start.y)
                    else -> 0f
                }.coerceIn(-MaxFlySlope, MaxFlySlope)
                Offset(start.x + (distance - sign * start.y) * slope, sign * distance)
            }
        }
    }

    private fun horizontal(x: Float) = if (x >= 0f) SwipeDirection.Right else SwipeDirection.Left
    private fun vertical(y: Float) = if (y >= 0f) SwipeDirection.Down else SwipeDirection.Up
}

/** Remembers a [LabCardStackState] starting at [initialTopIndex]. */
@Composable
fun rememberLabCardStackState(initialTopIndex: Int = 0): LabCardStackState =
    remember { LabCardStackState(initialTopIndex) }

/**
 * A swipeable card stack in the spirit of dating-app decks — built from scratch on
 * `pointerInput` + [Animatable], no third-party gesture or physics libraries.
 *
 * - drag the top card; releasing below [CardStackStyle.swipeThreshold] springs it
 *   back, releasing beyond it — or flinging faster than
 *   [CardStackStyle.flingVelocityThreshold] — sends it off along its trajectory
 * - the top card rotates away from the grab point, like a real card held off-center
 * - back cards scale up and rise in lockstep with the live drag, not after it
 * - drags toward a direction outside [allowedDirections] rubber-band with resistance
 * - [onSwiped] fires only after the fly-out animation completes; [onStackEnd] fires
 *   once when the last card leaves (and re-arms after a rewind)
 *
 * The stack never mutates [items]: [LabCardStackState.topIndex] walks the list, and
 * [LabCardStackState.rewind] steps it back with a spring drop-in.
 */
@Composable
fun <T> LabCardStack(
    items: List<T>,
    state: LabCardStackState,
    onSwiped: (T, SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    onStackEnd: () -> Unit = {},
    allowedDirections: Set<SwipeDirection> = setOf(SwipeDirection.Left, SwipeDirection.Right),
    style: CardStackStyle = CardStackStyle(),
    key: ((T) -> Any)? = null,
    content: @Composable (T) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val latestItems by rememberUpdatedState(items)
    val latestOnSwiped by rememberUpdatedState(onSwiped)
    val latestOnStackEnd by rememberUpdatedState(onStackEnd)
    val flingPx = with(density) { style.flingVelocityThreshold.toPx() }
    val peekPx = with(density) { style.peekOffset.toPx() }
    val visibleCards = style.visibleCards.coerceAtLeast(1)

    SideEffect {
        state.itemCount = items.size
        state.thresholdFraction = style.swipeThreshold
        state.flingThresholdPx = flingPx
        state.onSwipedListener = { index, direction ->
            latestItems.getOrNull(index)?.let { latestOnSwiped(it, direction) }
        }
        state.onStackEndListener = { latestOnStackEnd() }
    }

    Box(
        modifier = modifier.onSizeChanged { state.containerSize = it },
        contentAlignment = Alignment.Center,
    ) {
        val top = state.topIndex
        if (top > items.lastIndex) return@Box
        // One extra card past the visible window fades in as the top card departs.
        val deepest = (top + visibleCards).coerceAtMost(items.lastIndex)
        for (index in deepest downTo top) {
            val item = items[index]
            key(key?.invoke(item) ?: index) {
                val depth = index - top
                if (depth == 0) {
                    Box(
                        modifier = Modifier
                            .topCardGraphics(state, style)
                            .topCardGestures(state, allowedDirections, scope)
                            .topCardSemantics(state, allowedDirections, scope),
                    ) { content(item) }
                } else {
                    Box(
                        modifier = Modifier.backCardGraphics(state, style, depth, visibleCards, peekPx),
                    ) { content(item) }
                }
            }
        }
    }
}

private fun Modifier.topCardGraphics(state: LabCardStackState, style: CardStackStyle): Modifier =
    graphicsLayer {
        if (state.enteringIndex == state.topIndex) {
            // Rewind drop-in: the card returns from above and spring-settles into place.
            translationY = -state.containerSize.height * DropInHeightFraction * state.enter.value
        } else {
            val position = state.offset.value
            translationX = position.x
            translationY = position.y
            rotationZ = state.rotationSign * style.maxRotationDeg * state.dragProgress.x
        }
    }

private fun Modifier.backCardGraphics(
    state: LabCardStackState,
    style: CardStackStyle,
    depth: Int,
    visibleCards: Int,
    peekPx: Float,
): Modifier = graphicsLayer {
    // Depth eases toward the surface in lockstep with the top card's live drag —
    // and back down while a rewound card drops in.
    val progress = state.dragProgress
    val lift = max(abs(progress.x), abs(progress.y))
    val entering = if (state.enteringIndex >= 0) state.enter.value else 0f
    val effectiveDepth = (depth - lift - entering).coerceAtLeast(0f)
    val scale = 1f - style.scaleStep * effectiveDepth
    scaleX = scale
    scaleY = scale
    translationY = peekPx * effectiveDepth
    alpha = if (depth >= visibleCards) (depth - effectiveDepth).coerceIn(0f, 1f) else 1f
}

private fun Modifier.topCardGestures(
    state: LabCardStackState,
    allowedDirections: Set<SwipeDirection>,
    scope: CoroutineScope,
): Modifier = pointerInput(state, allowedDirections) {
    val velocityTracker = VelocityTracker()
    var rawDrag = Offset.Zero
    detectDragGestures(
        onDragStart = { down ->
            velocityTracker.resetTracking()
            // Continue from wherever the card is — catching a springing card mid-air
            // must not make it jump.
            rawDrag = state.offset.value
            state.rotationSign = if (down.y < size.height / 2f) 1f else -1f
            scope.launch { state.offset.stop() }
        },
        onDrag = { change, dragAmount ->
            change.consume()
            velocityTracker.addPointerInputChange(change)
            rawDrag += dragAmount
            val target = Offset(
                x = withResistance(rawDrag.x, SwipeDirection.Right, SwipeDirection.Left, allowedDirections),
                y = withResistance(rawDrag.y, SwipeDirection.Down, SwipeDirection.Up, allowedDirections),
            )
            scope.launch { state.offset.snapTo(target) }
        },
        onDragEnd = {
            val velocity = velocityTracker.calculateVelocity()
            scope.launch { state.settleRelease(Offset(velocity.x, velocity.y), allowedDirections) }
        },
        onDragCancel = {
            scope.launch { state.settleRelease(Offset.Zero, allowedDirections) }
        },
    )
}

private fun Modifier.topCardSemantics(
    state: LabCardStackState,
    allowedDirections: Set<SwipeDirection>,
    scope: CoroutineScope,
): Modifier = semantics {
    customActions = allowedDirections.map { direction ->
        CustomAccessibilityAction("Swipe ${direction.name.lowercase()}") {
            scope.launch { state.swipe(direction) }
            true
        }
    }
}

/** Rubber-band factor for drags toward a direction the stack does not allow. */
private fun withResistance(
    value: Float,
    positive: SwipeDirection,
    negative: SwipeDirection,
    allowed: Set<SwipeDirection>,
): Float = when {
    value > 0f && positive !in allowed -> value * RubberBandFactor
    value < 0f && negative !in allowed -> value * RubberBandFactor
    else -> value
}

private const val RubberBandFactor = 0.3f
private const val DropInHeightFraction = 0.35f
private const val MaxFlySlope = 0.7f

/** Springy return to center after a below-threshold release. */
private val ReturnSpring = spring(
    dampingRatio = 0.6f,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Offset.VisibilityThreshold,
)

/** Carries the card off-screen along its release trajectory without bouncing back in. */
private val FlyOutSpring = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 250f,
    visibilityThreshold = Offset.VisibilityThreshold,
)

/** Drop-in of a rewound card, with a light overshoot as it lands. */
private val ReenterSpring = spring<Float>(
    dampingRatio = 0.6f,
    stiffness = Spring.StiffnessMediumLow,
)
