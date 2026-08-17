package com.uilab.showcase.catalog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.uilab.showcase.components.fab.FabAction
import com.uilab.showcase.components.fab.LabMorphingFab
import com.uilab.showcase.designsystem.theme.LabTheme

private val allActions = listOf(
    FabAction("note", Icons.Filled.Edit, "New note"),
    FabAction("photo", Icons.Filled.PhotoCamera, "Take photo"),
    FabAction("voice", Icons.Filled.Mic, "Voice memo"),
    FabAction("reminder", Icons.Filled.Notifications, "Set reminder"),
)

private val notes = listOf(
    "Grocery list" to "Milk, eggs, coffee beans, basil",
    "Sprint notes" to "Retro on Thursday; demo the card stack",
    "Ideas" to "Morphing FAB, shimmer loaders, segmented picker",
)

/** Interactive demo body for the morphing FAB: a note list with the FAB in its corner. */
@Composable
fun MorphingFabDemo() {
    val colors = LabTheme.colors
    val motion = LabTheme.motion
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography

    var actionCount by remember { mutableIntStateOf(3) }
    val actions = remember(actionCount) { allActions.take(actionCount) }
    var expanded by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = "ACTIONS", style = typography.label, color = colors.onSurfaceMuted)
            Spacer(Modifier.height(spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                listOf(2, 3, 4).forEach { count ->
                    CatalogChip(
                        label = count.toString(),
                        selected = actionCount == count,
                        onClick = { actionCount = count },
                    )
                }
            }

            Spacer(Modifier.height(spacing.xl))

            Text(
                text = lastAction?.let { "Last action: $it" } ?: "Tap + to expand",
                style = typography.body,
                color = colors.onSurface,
            )

            Spacer(Modifier.height(spacing.xl))

            notes.forEach { (title, body) ->
                NoteCard(title = title, body = body)
                Spacer(Modifier.height(spacing.m))
            }
        }

        // Dim the page while the panel is open; tapping the dim collapses it.
        val scrim by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = tween(durationMillis = motion.medium, easing = motion.standard),
            label = "scrim",
        )
        if (scrim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrim }
                    .background(colors.background.copy(alpha = 0.72f))
                    .clickable(
                        enabled = expanded,
                        indication = null,
                        interactionSource = null,
                        onClick = { expanded = false },
                    ),
            )
        }

        LabMorphingFab(
            actions = actions,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onActionClick = { action ->
                lastAction = action.label
                expanded = false
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = spacing.l),
        )
    }
}

@Composable
private fun NoteCard(title: String, body: String) {
    val colors = LabTheme.colors
    val shapes = LabTheme.shapes
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surface)
            .padding(spacing.l),
    ) {
        Text(text = title, style = typography.body, color = colors.onSurface)
        Spacer(Modifier.height(spacing.xs))
        Text(text = body, style = typography.subtitle, color = colors.onSurfaceMuted, maxLines = 1)
        Spacer(Modifier.height(spacing.m))
        // Faux content lines, so the cards read as notes rather than empty tiles.
        listOf(1f, 0.7f).forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(shapes.pill)
                    .background(colors.surfaceElevated),
            )
            Spacer(Modifier.height(spacing.s))
        }
    }
}
