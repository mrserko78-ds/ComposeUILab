package com.uilab.showcase.components.chart

import androidx.compose.runtime.Immutable

/**
 * Data for [LabLineChart]: a single series of equally-spaced values with optional
 * x-axis labels. Kept deliberately small so it reads like a real library model.
 */
@Immutable
data class LineChartData(
    val values: List<Float>,
    val labels: List<String> = emptyList(),
)

/**
 * A single slice for [LabDonutChart]. Callers pass a [label] and a [value]; the
 * component assigns colors from the design-token palette, so the chart stays on-brand
 * without the caller touching raw colors.
 */
@Immutable
data class DonutSlice(
    val label: String,
    val value: Float,
)
