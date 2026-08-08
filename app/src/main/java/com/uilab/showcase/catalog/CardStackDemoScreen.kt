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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.uilab.showcase.components.cardstack.CardStackStyle
import com.uilab.showcase.components.cardstack.LabCardStack
import com.uilab.showcase.components.cardstack.SwipeDirection
import com.uilab.showcase.components.cardstack.rememberLabCardStackState
import com.uilab.showcase.designsystem.theme.LabTheme
import kotlinx.coroutines.launch

private data class ShowcaseCard(val title: String, val subtitle: String)

private val deck = listOf(
    ShowcaseCard("LabBottomNav", "Morphing spring indicator"),
    ShowcaseCard("LabLineChart", "Scrub-able Canvas line"),
    ShowcaseCard("LabDonutChart", "Tap-to-select slices"),
    ShowcaseCard("LabCardStack", "The one you are swiping"),
    ShowcaseCard("LabTheme", "Tokens for color & motion"),
    ShowcaseCard("Compose UI Lab", "More components soon"),
)

/** Interactive demo body for the swipeable card stack. */
@Composable
fun CardStackDemo() {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography

    val state = rememberLabCardStackState()
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<SwipeDirection>() }
    var finished by remember { mutableStateOf(false) }
    val liked = history.count { it == SwipeDirection.Right }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${state.topIndex.coerceAtMost(deck.size)} / ${deck.size}",
                style = typography.body,
                color = colors.onSurfaceMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Liked: $liked",
                style = typography.body,
                color = colors.onSurface,
            )
        }

        Spacer(Modifier.height(spacing.l))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (finished) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "Deck finished", style = typography.title, color = colors.onSurface)
                    Spacer(Modifier.height(spacing.xs))
                    Text(
                        text = "Rewind to bring the cards back",
                        style = typography.subtitle,
                        color = colors.onSurfaceMuted,
                    )
                    Spacer(Modifier.height(spacing.l))
                    CatalogChip(
                        label = "Restart",
                        selected = true,
                        onClick = {
                            history.clear()
                            finished = false
                            scope.launch { while (state.canRewind) state.rewind() }
                        },
                    )
                }
            }

            LabCardStack(
                items = deck,
                state = state,
                onSwiped = { _, direction -> history.add(direction) },
                onStackEnd = { finished = true },
                key = { it.title },
                // Larger peek than the default: tall demo cards shrink ~11dp per depth
                // level, so extra offset keeps the stack visibly layered.
                style = CardStackStyle(peekOffset = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) { card -> DeckCardFace(card) }
        }

        Spacer(Modifier.height(spacing.l))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.l),
            horizontalArrangement = Arrangement.spacedBy(spacing.xl, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(
                icon = Icons.Filled.Close,
                contentDescription = "Skip",
                tint = colors.onSurfaceMuted,
                enabled = !finished,
                onClick = { scope.launch { state.swipe(SwipeDirection.Left) } },
            )
            ActionButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Rewind",
                tint = colors.accent,
                enabled = state.canRewind,
                onClick = {
                    if (history.isNotEmpty()) history.removeAt(history.lastIndex)
                    finished = false
                    scope.launch { state.rewind() }
                },
            )
            ActionButton(
                icon = Icons.Filled.Favorite,
                contentDescription = "Like",
                tint = colors.chartPalette[3], // rose
                enabled = !finished,
                onClick = { scope.launch { state.swipe(SwipeDirection.Right) } },
            )
        }
    }
}

@Composable
private fun DeckCardFace(card: ShowcaseCard) {
    val colors = LabTheme.colors
    val shapes = LabTheme.shapes
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography
    val index = deck.indexOf(card)
    val background = colors.chartPalette[index % colors.chartPalette.size]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = spacing.xxl) // room for the peeking back cards
            .clip(shapes.medium)
            .background(background)
            .padding(spacing.xl),
    ) {
        Text(
            text = "0${index + 1}",
            style = typography.title,
            color = colors.onAccent.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(text = card.title, style = typography.title, color = colors.onAccent)
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = card.subtitle,
                style = typography.subtitle,
                color = colors.onAccent.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LabTheme.colors
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.35f)
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
    }
}
