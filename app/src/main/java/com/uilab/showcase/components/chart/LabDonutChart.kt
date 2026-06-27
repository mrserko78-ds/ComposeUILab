package com.uilab.showcase.components.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uilab.showcase.designsystem.theme.LabTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Angular gap between slices, in degrees. Shared by drawing and hit-testing. */
private const val SliceGapDeg = 2f

/**
 * A custom donut chart drawn on [Canvas] — no charting library underneath.
 *
 * - sweeps in clockwise from the top on first show / whenever the data changes
 * - tap a slice to select it: the slice springs outward, the rest dim back, and the
 *   center switches to that slice's share; tap again to deselect
 * - slice colors come from the design-token palette, so callers pass only label + value
 * - exposes an accessible breakdown + live selection through semantics
 */
@Composable
fun LabDonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerValue: String? = null,
    centerCaption: String? = null,
) {
    val colors = LabTheme.colors
    val motion = LabTheme.motion
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val palette = colors.chartPalette

    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val fractions = remember(slices) {
        if (total <= 0f) List(slices.size) { 0f } else slices.map { it.value / total }
    }

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = motion.slow, easing = motion.emphasized))
    }

    var selected by remember(slices) { mutableStateOf<Int?>(null) }
    val popSpec = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    // Fades the whole ring between "nothing selected" and "one slice focused".
    val selectionActive by animateFloatAsState(
        targetValue = if (selected != null) 1f else 0f,
        animationSpec = popSpec,
        label = "selectionActive",
    )
    // Each slice owns its pop, so switching slices cross-fades (the old one settles
    // back as the new one springs out) instead of snapping.
    val slicePops = slices.indices.map { i ->
        key(i) {
            animateFloatAsState(
                targetValue = if (selected == i) 1f else 0f,
                animationSpec = popSpec,
                label = "slicePop",
            ).value
        }
    }

    val breakdown = remember(slices) {
        if (slices.isEmpty() || total <= 0f) {
            "Donut chart, no data"
        } else {
            "Donut chart: " + slices.mapIndexed { i, s ->
                "${s.label} ${(fractions[i] * 100).roundToInt()} percent"
            }.joinToString(", ")
        }
    }
    val selectedState = selected?.let { i ->
        "${slices[i].label}: ${(fractions[i] * 100).roundToInt()} percent"
    }

    val valueStyle = remember { TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
    val captionStyle = remember { TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = breakdown
                liveRegion = LiveRegionMode.Polite
                selectedState?.let { stateDescription = it }
                if (slices.isNotEmpty() && total > 0f) {
                    customActions = slices.mapIndexed { i, s ->
                        CustomAccessibilityAction("Select ${s.label}") { selected = i; true }
                    } + CustomAccessibilityAction("Clear selection") { selected = null; true }
                }
            }
            .pointerInput(slices) {
                if (slices.isEmpty() || total <= 0f) return@pointerInput
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val stroke = strokeWidthPx(size.width.toFloat(), size.height.toFloat())
                    val outer = min(cx, cy) - popRoomPx()
                    val inner = outer - stroke
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val dist = hypot(dx, dy)
                    if (dist < inner - stroke * 0.4f || dist > outer + stroke * 0.4f) return@detectTapGestures
                    val deg = ((Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f) + 360f) % 360f
                    var acc = 0f
                    var hit: Int? = null
                    for (i in fractions.indices) {
                        val sweep = fractions[i] * 360f
                        // Match the drawn arc, which is inset by half a gap on each side,
                        // so a tap landing in a gap selects nothing.
                        val lo = acc + SliceGapDeg / 2f
                        val hi = acc + sweep - SliceGapDeg / 2f
                        if (deg in lo..hi) {
                            hit = i
                            break
                        }
                        acc += sweep
                    }
                    if (hit != null) {
                        selected = if (selected == hit) null else hit
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            },
    ) {
        if (slices.isEmpty() || total <= 0f) return@Canvas

        val cx = size.width / 2f
        val cy = size.height / 2f
        val stroke = strokeWidthPx(size.width, size.height)
        val outer = min(cx, cy) - popRoomPx()
        val radius = outer - stroke / 2f
        val maxShift = popRoomPx()

        // One progress angle sweeps clockwise from the top; each slice draws only the
        // portion of its arc the sweep has already reached (true wind-in, not all at once).
        val revealedDeg = 360f * reveal.value

        fun drawSlice(index: Int) {
            val accStart = fractions.take(index).sum() * 360f
            val fullSweep = fractions[index] * 360f
            val visible = (revealedDeg - accStart).coerceIn(0f, fullSweep)
            val sweep = (visible - SliceGapDeg).coerceAtLeast(0f)
            if (sweep <= 0f) return
            val start = -90f + accStart + SliceGapDeg / 2f

            val slicePop = slicePops.getOrElse(index) { 0f }
            val mid = Math.toRadians((start + sweep / 2f).toDouble())
            val shift = maxShift * slicePop
            val ox = cos(mid).toFloat() * shift
            val oy = sin(mid).toFloat() * shift
            val w = stroke + 4.dp.toPx() * slicePop
            // Dim non-focused slices, but only to the extent a selection is active —
            // everything animates, nothing snaps.
            val alpha = lerp(1f, lerp(0.42f, 1f, slicePop), selectionActive)
            drawArc(
                color = palette[index % palette.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - radius + ox, cy - radius + oy),
                size = Size(radius * 2f, radius * 2f),
                alpha = alpha,
                style = Stroke(width = w),
            )
        }

        // Draw unfocused slices first, the focused one last so its pop sits on top.
        fractions.indices.forEach { if (it != selected) drawSlice(it) }
        selected?.let { drawSlice(it) }

        // Center label.
        val sel = selected
        val valueText = if (sel != null) "${(fractions[sel] * 100).roundToInt()}%" else centerValue
        val captionText = if (sel != null) slices[sel].label else centerCaption

        val valueLayout = valueText?.let { textMeasurer.measure(it, valueStyle) }
        val captionLayout = captionText?.let { textMeasurer.measure(it, captionStyle) }
        val gap = if (valueLayout != null && captionLayout != null) 2.dp.toPx() else 0f
        val totalH = (valueLayout?.size?.height ?: 0) + (captionLayout?.size?.height ?: 0) + gap
        var yCursor = cy - totalH / 2f
        valueLayout?.let {
            drawText(it, color = colors.onSurface, topLeft = Offset(cx - it.size.width / 2f, yCursor))
            yCursor += it.size.height + gap
        }
        captionLayout?.let {
            drawText(it, color = colors.onSurfaceMuted, topLeft = Offset(cx - it.size.width / 2f, yCursor))
        }
    }
}

private fun Density.strokeWidthPx(w: Float, h: Float): Float {
    // Ring thickness scales gently with the smaller dimension, clamped to a sane band.
    val base = min(w, h) * 0.22f
    return base.coerceIn(20.dp.toPx(), 40.dp.toPx())
}

private fun Density.popRoomPx(): Float = 12.dp.toPx()
