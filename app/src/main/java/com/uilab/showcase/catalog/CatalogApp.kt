package com.uilab.showcase.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.uilab.showcase.designsystem.theme.LabTheme

private enum class CatalogEntry(val tab: String, val subtitle: String) {
    BottomNav("Bottom Nav", "Animated Bottom Navigation"),
    Charts("Charts", "Canvas Line & Donut"),
}

/**
 * Host of the component catalog. A lightweight header + segmented switch let the
 * showcase grow one entry at a time; each component owns its own demo body.
 */
@Composable
fun CatalogApp(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography

    var entry by rememberSaveable { mutableStateOf(CatalogEntry.BottomNav) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = spacing.xl)
                .padding(top = spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Compose UI Lab", style = typography.title, color = colors.onSurface)
                    Spacer(Modifier.height(spacing.xs))
                    Text(
                        text = entry.subtitle,
                        style = typography.subtitle,
                        color = colors.onSurfaceMuted,
                    )
                }
                CatalogChip(
                    label = if (darkTheme) "Dark" else "Light",
                    selected = true,
                    onClick = onToggleTheme,
                )
            }

            Spacer(Modifier.height(spacing.l))

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                CatalogEntry.entries.forEach { item ->
                    CatalogChip(
                        label = item.tab,
                        selected = entry == item,
                        onClick = { entry = item },
                    )
                }
            }

            Spacer(Modifier.height(spacing.xl))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (entry) {
                    CatalogEntry.BottomNav -> BottomNavDemo()
                    CatalogEntry.Charts -> ChartsDemo()
                }
            }
        }
    }
}

/** Small pill control shared across demo screens. */
@Composable
internal fun CatalogChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LabTheme.colors
    val shapes = LabTheme.shapes
    val typography = LabTheme.typography
    Box(
        modifier = Modifier
            .clip(shapes.pill)
            .background(if (selected) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = typography.body,
            color = if (selected) colors.onAccent else colors.onSurfaceMuted,
        )
    }
}
