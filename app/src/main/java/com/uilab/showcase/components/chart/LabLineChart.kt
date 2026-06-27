package com.uilab.showcase.components.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uilab.showcase.designsystem.theme.LabTheme
import kotlin.math.roundToInt

private val HInset = 12.dp
private val TopInset = 14.dp
private val BottomInset = 14.dp

/**
 * A custom line chart drawn entirely on [Canvas] — no charting library underneath.
 *
 * - smooth Catmull-Rom curve through the points (no jagged segments)
 * - draws itself in left-to-right on first show / whenever the data changes
 * - gradient area fill under the line
 * - touch to scrub: drag across the chart to read any point; a guide line, a focus
 *   dot and a value tooltip follow the finger, with a light haptic tick per step
 * - exposes an accessible summary + live selected value through semantics
 *
 * Stateless and token-driven: colors, motion and type all come from [LabTheme].
 */
@Composable
fun LabLineChart(
    data: LineChartData,
    modifier: Modifier = Modifier,
    valueFormat: (Float) -> String = { it.roundToInt().toString() },
) {
    val colors = LabTheme.colors
    val motion = LabTheme.motion
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()

    val values = data.values
    val count = values.size

    // Draw-in animation, restarted whenever the dataset changes.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(data) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = motion.slow, easing = motion.emphasized))
    }

    var activeIndex by remember(data) { mutableStateOf<Int?>(null) }

    val summary = remember(data) {
        if (count == 0) {
            "Line chart, no data"
        } else {
            "Line chart, $count points, from " +
                "${valueFormat(values.min())} to ${valueFormat(values.max())}"
        }
    }
    val selectedState = activeIndex?.let { i ->
        val label = data.labels.getOrNull(i)
        if (label != null) "$label: ${valueFormat(values[i])}" else valueFormat(values[i])
    }

    val tooltipStyle = remember { TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    val axisStyle = remember { TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = summary
                liveRegion = LiveRegionMode.Polite
                selectedState?.let { stateDescription = it }
                if (count > 0) {
                    customActions = listOf(
                        CustomAccessibilityAction("Next point") {
                            activeIndex = ((activeIndex ?: -1) + 1).coerceAtMost(count - 1)
                            true
                        },
                        CustomAccessibilityAction("Previous point") {
                            activeIndex = ((activeIndex ?: count) - 1).coerceAtLeast(0)
                            true
                        },
                        CustomAccessibilityAction("Clear selection") {
                            activeIndex = null
                            true
                        },
                    )
                }
            }
            .pointerInput(data) {
                if (count < 2) return@pointerInput
                val left = HInset.toPx()
                val plotW = size.width - left * 2f
                fun indexAt(x: Float): Int {
                    val rel = ((x - left) / plotW).coerceIn(0f, 1f)
                    return (rel * (count - 1)).roundToInt().coerceIn(0, count - 1)
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var current = indexAt(down.position.x)
                    activeIndex = current
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val next = indexAt(change.position.x)
                        if (next != current) {
                            current = next
                            activeIndex = next
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        change.consume()
                    }
                    activeIndex = null
                }
            },
    ) {
        if (count == 0) return@Canvas

        val left = HInset.toPx()
        val plotW = size.width - left * 2f
        val plotTop = TopInset.toPx()
        val plotBottom = size.height - BottomInset.toPx()
        val plotH = plotBottom - plotTop

        val minV = values.min()
        val maxV = values.max()
        val pad = ((maxV - minV) * 0.14f).let { if (it == 0f) 1f else it }
        val lo = minV - pad
        val hi = maxV + pad

        fun px(i: Int): Float =
            if (count == 1) left + plotW / 2f else left + plotW * (i / (count - 1f))

        fun py(v: Float): Float = plotBottom - ((v - lo) / (hi - lo)) * plotH

        val points = values.mapIndexed { i, v -> Offset(px(i), py(v)) }

        // Subtle baseline + a couple of interior gridlines.
        val gridLines = 3
        for (g in 0..gridLines) {
            val y = plotTop + plotH * (g / gridLines.toFloat())
            drawLine(
                color = colors.chartGrid,
                start = Offset(left, y),
                end = Offset(left + plotW, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val linePath = buildSmoothPath(points)
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, plotBottom)
            lineTo(points.first().x, plotBottom)
            close()
        }

        // Reveal the line + fill left-to-right.
        val revealRight = left + plotW * reveal.value
        clipRect(left = 0f, top = 0f, right = revealRight.coerceAtLeast(left), bottom = size.height) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(colors.accent.copy(alpha = 0.26f), colors.accent.copy(alpha = 0f)),
                    startY = plotTop,
                    endY = plotBottom,
                ),
            )
            drawPath(
                path = linePath,
                color = colors.accent,
                style = Stroke(width = 3.dp.toPx()),
            )
        }

        // Endpoint dot once the line is essentially drawn.
        if (reveal.value > 0.96f && activeIndex == null) {
            val last = points.last()
            drawCircle(colors.accent, radius = 4.dp.toPx(), center = last)
            drawCircle(colors.surface, radius = 1.8.dp.toPx(), center = last)
        }

        // X labels (optional).
        if (data.labels.isNotEmpty()) {
            val step = (count / 4).coerceAtLeast(1)
            for (i in 0 until count step step) {
                val label = data.labels.getOrNull(i) ?: continue
                val layout = textMeasurer.measure(label, axisStyle)
                val tx = (px(i) - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(
                    textLayoutResult = layout,
                    color = colors.onSurfaceMuted,
                    topLeft = Offset(tx, plotBottom + 3.dp.toPx()),
                )
            }
        }

        // Scrub overlay: guide line, focus dot, value tooltip.
        val active = activeIndex
        if (active != null && active in points.indices) {
            val p = points[active]
            drawLine(
                color = colors.accent.copy(alpha = 0.35f),
                start = Offset(p.x, plotTop),
                end = Offset(p.x, plotBottom),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawCircle(colors.accent.copy(alpha = 0.18f), radius = 9.dp.toPx(), center = p)
            drawCircle(colors.accent, radius = 5.dp.toPx(), center = p)
            drawCircle(colors.surface, radius = 2.dp.toPx(), center = p)

            val text = valueFormat(values[active])
            val layout = textMeasurer.measure(text, tooltipStyle)
            val padH = 8.dp.toPx()
            val padV = 5.dp.toPx()
            val bw = layout.size.width + padH * 2
            val bh = layout.size.height + padV * 2
            val bx = (p.x - bw / 2f).coerceIn(0f, size.width - bw)
            val by = (p.y - bh - 12.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(
                color = colors.surfaceElevated,
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            drawRoundRect(
                color = colors.accent.copy(alpha = 0.30f),
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawText(
                textLayoutResult = layout,
                color = colors.onSurface,
                topLeft = Offset(bx + padH, by + padV),
            )
        }
    }
}

/**
 * Smooth path through [points] using Catmull-Rom segments converted to cubic Béziers,
 * so the line reads as a continuous curve instead of straight joints.
 */
private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    for (i in 0 until points.size - 1) {
        val p0 = points[if (i == 0) i else i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[if (i + 2 > points.lastIndex) i + 1 else i + 2]
        val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
        val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}
