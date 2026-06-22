package com.uilab.showcase.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.dp
import com.uilab.showcase.components.bottomnav.LabBottomNav
import com.uilab.showcase.components.bottomnav.NavItem
import com.uilab.showcase.designsystem.theme.LabTheme

private val allItems = listOf(
    NavItem("home", Icons.Filled.Home, "Home"),
    NavItem("search", Icons.Filled.Search, "Search"),
    NavItem("alerts", Icons.Filled.Notifications, "Alerts"),
    NavItem("settings", Icons.Filled.Settings, "Settings"),
    NavItem("profile", Icons.Filled.Person, "Profile"),
)

@Composable
fun BottomNavDemoScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography

    var itemCount by remember { mutableIntStateOf(4) }
    val items = remember(itemCount) { allItems.take(itemCount) }
    var selectedId by remember(itemCount) { mutableStateOf(items.first().id) }
    val selectedLabel = items.firstOrNull { it.id == selectedId }?.label ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(spacing.xl),
        ) {
            Text(text = "Compose UI Lab", style = typography.title, color = colors.onSurface)
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = "Animated Bottom Navigation",
                style = typography.subtitle,
                color = colors.onSurfaceMuted,
            )

            Spacer(Modifier.height(spacing.xl))

            Text(text = "ITEMS", style = typography.label, color = colors.onSurfaceMuted)
            Spacer(Modifier.height(spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                listOf(3, 4, 5).forEach { count ->
                    Chip(
                        label = count.toString(),
                        selected = itemCount == count,
                        onClick = { itemCount = count },
                    )
                }
            }

            Spacer(Modifier.height(spacing.l))

            Text(text = "THEME", style = typography.label, color = colors.onSurfaceMuted)
            Spacer(Modifier.height(spacing.s))
            Chip(
                label = if (darkTheme) "Dark" else "Light",
                selected = true,
                onClick = onToggleTheme,
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "Selected: $selectedLabel",
                style = typography.body,
                color = colors.onSurface,
            )

            Spacer(Modifier.weight(1f))
        }

        LabBottomNav(
            items = items,
            selectedId = selectedId,
            onSelect = { selectedId = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = spacing.xl, vertical = spacing.l),
        )
    }
}

@Composable
private fun Chip(
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
