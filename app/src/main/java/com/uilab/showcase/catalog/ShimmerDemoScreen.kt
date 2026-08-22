package com.uilab.showcase.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.uilab.showcase.components.shimmer.LabSkeleton
import com.uilab.showcase.components.shimmer.LabSkeletonBox
import com.uilab.showcase.components.shimmer.LabSkeletonCircle
import com.uilab.showcase.components.shimmer.LabSkeletonLine
import com.uilab.showcase.designsystem.theme.LabTheme
import kotlinx.coroutines.delay

private data class FeedPost(val author: String, val handle: String, val text: String)

private val feed = listOf(
    FeedPost("Compose UI Lab", "@uilab", "Skeletons that shimmer as one surface, not a field of blinking tiles."),
    FeedPost("Design tokens", "@tokens", "Every placeholder reads its fill and highlight from the theme."),
    FeedPost("Motion", "@motion", "One sweep, a short breath, repeat — then a crossfade into content."),
)

private const val LoadMillis = 2600L

/** Demo body for shimmer skeletons: a feed that reloads into placeholders and back. */
@Composable
fun ShimmerDemo() {
    val spacing = LabTheme.spacing

    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(reloadKey) {
        loading = true
        delay(LoadMillis)
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CatalogChip(label = "Reload", selected = true, onClick = { reloadKey++ })
            CatalogChip(
                label = if (loading) "Loading…" else "Loaded",
                selected = false,
                onClick = { loading = !loading },
            )
        }

        Spacer(Modifier.height(spacing.xl))

        LabSkeleton(
            loading = loading,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            skeleton = { FeedSkeleton() },
            content = { FeedContent() },
        )
    }
}

@Composable
private fun FeedSkeleton() {
    val spacing = LabTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
        repeat(feed.size) { index ->
            FeedCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabSkeletonCircle(size = 40.dp)
                    Spacer(Modifier.width(spacing.m))
                    Column {
                        LabSkeletonLine(width = 120.dp, height = 12.dp)
                        Spacer(Modifier.height(spacing.s))
                        LabSkeletonLine(width = 72.dp, height = 10.dp)
                    }
                }
                Spacer(Modifier.height(spacing.l))
                LabSkeletonLine()
                Spacer(Modifier.height(spacing.s))
                LabSkeletonLine(modifier = Modifier.fillMaxWidth(0.8f))
                if (index == 0) {
                    Spacer(Modifier.height(spacing.l))
                    LabSkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        shape = LabTheme.shapes.medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedContent() {
    val colors = LabTheme.colors
    val shapes = LabTheme.shapes
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography
    val palette = colors.chartPalette
    Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
        feed.forEachIndexed { index, post ->
            FeedCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette[index % palette.size]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = post.author.first().toString(),
                            style = typography.body,
                            color = colors.onAccent,
                        )
                    }
                    Spacer(Modifier.width(spacing.m))
                    Column {
                        Text(text = post.author, style = typography.body, color = colors.onSurface)
                        Text(text = post.handle, style = typography.label, color = colors.onSurfaceMuted)
                    }
                }
                Spacer(Modifier.height(spacing.l))
                Text(text = post.text, style = typography.subtitle, color = colors.onSurface)
                if (index == 0) {
                    Spacer(Modifier.height(spacing.l))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(shapes.medium)
                            .background(
                                Brush.linearGradient(listOf(palette[0], palette[5], palette[1])),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedCard(content: @Composable () -> Unit) {
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
