package com.uilab.showcase.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.uilab.showcase.components.chart.DonutSlice
import com.uilab.showcase.components.chart.LabDonutChart
import com.uilab.showcase.components.chart.LabLineChart
import com.uilab.showcase.components.chart.LineChartData
import com.uilab.showcase.designsystem.theme.LabTheme
import kotlin.math.roundToInt

private enum class Range(val label: String) { Week("Week"), Month("Month") }

private val weekLine = LineChartData(
    values = listOf(42f, 58f, 35f, 70f, 64f, 88f, 53f),
    labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
)

private val monthLine = LineChartData(
    values = listOf(
        30f, 44f, 38f, 52f, 47f, 61f, 55f, 49f, 66f, 72f,
        60f, 58f, 74f, 69f, 81f, 77f, 64f, 70f, 88f, 84f,
        79f, 92f, 86f, 75f, 90f, 97f, 83f, 95f, 88f, 101f,
    ),
    labels = (1..30).map { it.toString() },
)

private val weekDonut = listOf(
    DonutSlice("Food", 320f),
    DonutSlice("Transport", 180f),
    DonutSlice("Shopping", 220f),
    DonutSlice("Bills", 280f),
)

private val monthDonut = listOf(
    DonutSlice("Food", 1280f),
    DonutSlice("Transport", 640f),
    DonutSlice("Shopping", 870f),
    DonutSlice("Bills", 1120f),
    DonutSlice("Fun", 410f),
)

/** Demo body for the Canvas charts: a scrub-able line and a tap-to-select donut. */
@Composable
fun ChartsDemo() {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography

    var range by remember { mutableStateOf(Range.Week) }
    val line = if (range == Range.Week) weekLine else monthLine
    val donut = if (range == Range.Week) weekDonut else monthDonut
    val donutTotal = donut.sumOf { it.value.toDouble() }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
            Range.entries.forEach { r ->
                CatalogChip(
                    label = r.label,
                    selected = range == r,
                    onClick = { range = r },
                )
            }
        }

        Spacer(Modifier.height(spacing.l))

        Card {
            Text(text = "Spending", style = typography.body, color = colors.onSurface)
            Text(
                text = "Drag across to read any day",
                style = typography.label,
                color = colors.onSurfaceMuted,
            )
            Spacer(Modifier.height(spacing.m))
            LabLineChart(
                data = line,
                valueFormat = { "$" + it.roundToInt() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
            )
        }

        Spacer(Modifier.height(spacing.l))

        Card {
            Text(text = "By category", style = typography.body, color = colors.onSurface)
            Text(
                text = "Tap a slice for its share",
                style = typography.label,
                color = colors.onSurfaceMuted,
            )
            Spacer(Modifier.height(spacing.m))
            LabDonutChart(
                slices = donut,
                centerValue = "$" + donutTotal.roundToInt(),
                centerCaption = "Spent",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(212.dp),
            )
            Spacer(Modifier.height(spacing.l))
            Legend(donut, donutTotal)
        }

        Spacer(Modifier.height(spacing.xl))
    }
}

@Composable
private fun Legend(slices: List<DonutSlice>, total: Float) {
    val colors = LabTheme.colors
    val typography = LabTheme.typography
    val palette = colors.chartPalette
    Column(verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.s)) {
        slices.forEachIndexed { i, slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(palette[i % palette.size]),
                )
                Spacer(Modifier.size(LabTheme.spacing.s))
                Text(
                    text = slice.label,
                    style = typography.body,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                val pct = if (total > 0f) (slice.value / total * 100).roundToInt() else 0
                Text(text = "$pct%", style = typography.body, color = colors.onSurfaceMuted)
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val colors = LabTheme.colors
    val shapes = LabTheme.shapes
    val spacing = LabTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surface)
            .padding(spacing.l),
    ) {
        content()
    }
}
