package com.uilab.showcase.components.bottomnav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uilab.showcase.designsystem.theme.LabTheme

/** A single navigation destination. Designed as a library-style public model. */
@Immutable
data class NavItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
)

/**
 * Custom animated bottom navigation with a morphing, spring-driven indicator.
 *
 * Stateless: caller owns [selectedId] and reacts to [onSelect] (unidirectional flow).
 * Built from scratch — no Material NavigationBar underneath.
 */
@Composable
fun LabBottomNav(
    items: List<NavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return // library precondition: render nothing for an empty bar

    val colors = LabTheme.colors
    val shapes = LabTheme.shapes
    val haptic = LocalHapticFeedback.current
    val selectedIndex = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shapes.pill)
            .background(colors.surface),
    ) {
        val itemWidth = maxWidth / items.size.toFloat()
        val indicatorWidth = itemWidth * 0.66f
        val indicatorHeight = 46.dp
        val targetX = itemWidth * selectedIndex.toFloat() + (itemWidth - indicatorWidth) / 2f

        // Spring with a sub-critical damping ratio => a subtle overshoot as it settles.
        val animatedX by animateDpAsState(
            targetValue = targetX,
            animationSpec = spring(
                dampingRatio = 0.62f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "indicatorOffset",
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = animatedX)
                .size(width = indicatorWidth, height = indicatorHeight)
                .clip(shapes.pill)
                .background(colors.indicator),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                NavCell(
                    item = item,
                    isSelected = index == selectedIndex,
                    onClick = {
                        if (index != selectedIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(item.id)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun NavCell(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LabTheme.colors
    val motion = LabTheme.motion
    val typography = LabTheme.typography

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.onSurfaceMuted,
        animationSpec = tween(durationMillis = motion.medium, easing = motion.standard),
        label = "iconColor",
    )
    // Bouncy settle when selected.
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "iconScale",
    )

    // Custom press feedback (no Material ripple — it looks cheap on a nav bar).
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                selected = isSelected
                role = Role.Tab
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale * pressScale
                    scaleY = iconScale * pressScale
                },
        )
        // Label is revealed only for the active destination -> expanding-pill feel.
        AnimatedVisibility(visible = isSelected) {
            Text(
                text = item.label,
                color = colors.accent,
                style = typography.label,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
