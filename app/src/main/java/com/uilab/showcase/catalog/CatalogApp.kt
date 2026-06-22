package com.uilab.showcase.catalog

import androidx.compose.runtime.Composable

/**
 * Host of the component catalog. Currently shows the first showcase component.
 * New components get their own entry here as the library grows.
 */
@Composable
fun CatalogApp(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    BottomNavDemoScreen(
        darkTheme = darkTheme,
        onToggleTheme = onToggleTheme,
    )
}
