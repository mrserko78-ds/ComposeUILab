package com.uilab.showcase.components.fab

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.uilab.showcase.designsystem.theme.LabTheme

/**
 * A floating action button that morphs into an action panel — a container transform
 * built from scratch, no Material FAB or menu underneath.
 *
 * - one spring drives everything: the circle grows into a rounded panel while its
 *   corners, background and icon tint interpolate in lockstep, so nothing lags
 * - the "+" rotates into a "×" (with the spring's slight overshoot) and stays anchored
 *   in the corner it started from — the panel unfolds *out of* the button
 * - actions reveal in a stagger from the button upward, each fading and rising off
 *   the same progress value; collapsing plays the sequence backward
 * - custom press feedback (no ripple) + a haptic tick on toggle
 * - semantics: the button reports its expanded state; actions are real buttons only
 *   while expanded and disappear from the tree when collapsed
 *
 * Stateless: the caller owns [expanded] and reacts to [onExpandedChange] /
 * [onActionClick] (unidirectional flow). Place it where a FAB goes — the panel opens
 * up and to the start from the button's bottom-end corner.
 */
@Composable
fun LabMorphingFab(
    actions: List<FabAction>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onActionClick: (FabAction) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Add,
    contentDescription: String = "Actions",
    style: MorphingFabStyle = MorphingFabStyle(),
) {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val haptic = LocalHapticFeedback.current

    // The single driver. Low stiffness keeps the unfold readable (~400ms), slightly
    // under-damped so the panel lands with a soft bounce.
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "fabMorph",
    )
    val clamped = progress.coerceIn(0f, 1f)

    val expandedHeight = style.actionHeight * actions.size + spacing.s * 2 + style.collapsedSize
    val width = lerp(style.collapsedSize, style.expandedWidth, progress).coerceAtLeast(style.collapsedSize * 0.9f)
    val height = lerp(style.collapsedSize, expandedHeight, progress).coerceAtLeast(style.collapsedSize * 0.9f)
    val corner = lerp(style.collapsedSize / 2, style.expandedCornerRadius, clamped)
    val shape = RoundedCornerShape(corner)
    val containerColor = lerp(colors.accent, colors.surface, clamped)
    val iconTint = lerp(colors.onAccent, colors.accent, clamped)

    val toggle = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onExpandedChange(!expanded)
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(elevation = 8.dp, shape = shape, clip = false)
            .clip(shape)
            .background(containerColor),
    ) {
        // Actions live in the tree only while there is something to see, so a collapsed
        // FAB exposes no phantom buttons to touch or accessibility.
        if (progress > 0.001f) {
            Column(
                modifier = Modifier
                    .anchoredAboveButton(style)
                    .padding(top = spacing.s),
            ) {
                actions.forEachIndexed { index, action ->
                    // Reveal from the button upward: the last row leads, the first trails.
                    val order = actions.lastIndex - index
                    val reveal = ((clamped - StaggerStart - order * StaggerStep) / StaggerSpan).coerceIn(0f, 1f)
                    ActionRow(
                        action = action,
                        reveal = reveal,
                        enabled = expanded,
                        height = style.actionHeight,
                        onClick = { onActionClick(action) },
                    )
                }
            }
        }

        MainButton(
            icon = icon,
            tint = iconTint,
            rotation = IconRotationDeg * progress,
            size = style.collapsedSize,
            expanded = expanded,
            contentDescription = contentDescription,
            onClick = toggle,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/**
 * Measures the actions at the full expanded width (so text never reflows mid-morph)
 * and pins them to the bottom-start of the container, just above the button row —
 * the container's animated clip does the revealing.
 */
private fun Modifier.anchoredAboveButton(style: MorphingFabStyle): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints.fixedWidth(style.expandedWidth.roundToPx()))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeRelative(
            x = 0,
            y = constraints.maxHeight - style.collapsedSize.roundToPx() - placeable.height,
        )
    }
}

@Composable
private fun MainButton(
    icon: ImageVector,
    tint: Color,
    rotation: Float,
    size: Dp,
    expanded: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "fabPress",
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = pressScale
                    scaleY = pressScale
                },
        )
    }
}

@Composable
private fun ActionRow(
    action: FabAction,
    reveal: Float,
    enabled: Boolean,
    height: Dp,
    onClick: () -> Unit,
) {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressTint by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "actionPress",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * RevealRise.toPx()
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .background(colors.indicator.copy(alpha = colors.indicator.alpha * pressTint))
            .padding(horizontal = spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(spacing.m))
        Text(
            text = action.label,
            style = typography.body,
            color = colors.onSurface,
            maxLines = 1,
        )
    }
}

private const val IconRotationDeg = 45f
private const val StaggerStart = 0.25f
private const val StaggerStep = 0.09f
private const val StaggerSpan = 0.45f
private val RevealRise = 10.dp
