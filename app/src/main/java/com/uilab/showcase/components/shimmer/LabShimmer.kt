package com.uilab.showcase.components.shimmer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uilab.showcase.designsystem.theme.LabTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.coroutines.delay

/**
 * Shared driver of one shimmer sweep. Every skeleton bound to the same state reads the
 * same progress, so a single band travels across the whole host — the placeholders
 * read as one loading surface instead of a field of independently blinking tiles.
 *
 * Host bounds are captured by [LabSkeleton]; without a host each skeleton sweeps
 * across its own bounds.
 */
@Stable
class LabShimmerState(val style: ShimmerStyle = ShimmerStyle()) {
    /** 0..1 position of the band along the sweep; animated by [rememberLabShimmerState]. */
    var progress by mutableFloatStateOf(0f)
        internal set

    internal var hostBounds by mutableStateOf(Rect.Zero)

    /**
     * Gradient for an element whose top-left sits at [origin] (root coordinates) with
     * the given [size], expressed in that element's local space.
     */
    internal fun brush(origin: Offset, size: Size, bandPx: Float, base: Color, highlight: Color): Brush {
        val host = if (hostBounds.isEmpty) Rect(origin, size) else hostBounds
        val radians = style.angleDeg * PI.toFloat() / 180f
        val direction = Offset(cos(radians), sin(radians))
        // Extend the path so the tilted band fully clears both host edges.
        val overshoot = bandPx + host.height * tan(radians)
        val sweepX = host.left - overshoot + (host.width + overshoot * 2) * progress
        val center = Offset(sweepX - origin.x, host.center.y - origin.y)
        val half = direction * (bandPx / 2f)
        return Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = center - half,
            end = center + half,
            tileMode = TileMode.Clamp,
        )
    }
}

/**
 * Remembers a [LabShimmerState] and keeps its sweep running while [active]:
 * one linear pass, a short pause, repeat.
 */
@Composable
fun rememberLabShimmerState(
    active: Boolean = true,
    style: ShimmerStyle = ShimmerStyle(),
): LabShimmerState {
    val state = remember(style) { LabShimmerState(style) }
    LaunchedEffect(state, active) {
        if (!active) return@LaunchedEffect
        while (true) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = style.sweepMillis, easing = LinearEasing),
            ) { value, _ -> state.progress = value }
            delay(style.pauseMillis.toLong())
        }
    }
    return state
}

/** The shimmer state skeleton primitives bind to; provided by [LabSkeleton]. */
val LocalLabShimmer = compositionLocalOf<LabShimmerState?> { null }

/**
 * Crossfades between a [skeleton] and the real [content], hosting the shimmer sweep
 * that every skeleton primitive inside [skeleton] shares.
 *
 * - the band sweeps across *this* host's bounds, continuous through every placeholder
 * - the sweep keeps running through the fade-out, so skeletons never freeze mid-band
 * - the skeleton announces itself as loading to accessibility services
 */
@Composable
fun LabSkeleton(
    loading: Boolean,
    modifier: Modifier = Modifier,
    style: ShimmerStyle = ShimmerStyle(),
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val motion = LabTheme.motion
    // Keep sweeping until the fade-out has finished, then stop spending frames.
    var active by remember { mutableStateOf(loading) }
    LaunchedEffect(loading) {
        if (loading) active = true else { delay(motion.medium.toLong()); active = false }
    }
    val state = rememberLabShimmerState(active = active, style = style)

    Box(modifier = modifier.onGloballyPositioned { state.hostBounds = it.boundsInRoot() }) {
        Crossfade(
            targetState = loading,
            animationSpec = tween(durationMillis = motion.medium, easing = motion.standard),
            label = "skeleton",
        ) { showSkeleton ->
            if (showSkeleton) {
                CompositionLocalProvider(LocalLabShimmer provides state) {
                    Box(modifier = Modifier.semantics { contentDescription = "Loading" }) { skeleton() }
                }
            } else {
                content()
            }
        }
    }
}

/**
 * Paints this element as a skeleton placeholder: the resting [LabColors.skeleton]
 * fill with the shared shimmer band passing through, clipped to [shape].
 */
@Composable
fun Modifier.labShimmer(
    state: LabShimmerState,
    shape: Shape,
): Modifier {
    val colors = LabTheme.colors
    val bandPx = with(LocalDensity.current) { state.style.bandWidth.toPx() }
    return this.then(ShimmerElement(state, shape, bandPx, colors.skeleton, colors.skeletonHighlight))
}

private data class ShimmerElement(
    val state: LabShimmerState,
    val shape: Shape,
    val bandPx: Float,
    val base: Color,
    val highlight: Color,
) : ModifierNodeElement<ShimmerNode>() {
    override fun create() = ShimmerNode(state, shape, bandPx, base, highlight)
    override fun update(node: ShimmerNode) {
        node.state = state
        node.shape = shape
        node.bandPx = bandPx
        node.base = base
        node.highlight = highlight
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "labShimmer"
    }
}

private class ShimmerNode(
    var state: LabShimmerState,
    var shape: Shape,
    var bandPx: Float,
    var base: Color,
    var highlight: Color,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var origin = Offset.Zero

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        origin = coordinates.positionInRoot()
    }

    override fun ContentDrawScope.draw() {
        // Reading state.progress here ties redraws to the sweep — no recomposition.
        val brush = state.brush(origin, size, bandPx, base, highlight)
        drawOutline(shape.createOutline(size, layoutDirection, this), brush)
        drawContent()
    }
}

private val NoShimmer = LabShimmerState()

@Composable
private fun shimmerState(): LabShimmerState = LocalLabShimmer.current ?: NoShimmer

/** A text-line placeholder. Fills the width unless [width] is given. */
@Composable
fun LabSkeletonLine(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 12.dp,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .labShimmer(shimmerState(), LabTheme.shapes.pill),
    )
}

/** An avatar / icon placeholder. */
@Composable
fun LabSkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size).labShimmer(shimmerState(), CircleShape))
}

/** A free-form block placeholder (image, card); size it through [modifier]. */
@Composable
fun LabSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = LabTheme.shapes.small,
) {
    Box(modifier = modifier.labShimmer(shimmerState(), shape))
}
