package com.uilab.showcase.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.uilab.showcase.designsystem.token.DarkLabColors
import com.uilab.showcase.designsystem.token.LabColors
import com.uilab.showcase.designsystem.token.LabMotion
import com.uilab.showcase.designsystem.token.LabShapes
import com.uilab.showcase.designsystem.token.LabSpacing
import com.uilab.showcase.designsystem.token.LabTypography
import com.uilab.showcase.designsystem.token.LightLabColors

private val LocalLabColors = staticCompositionLocalOf<LabColors> {
    error("LabColors not provided. Wrap content in LabTheme { }.")
}
private val LocalLabTypography = staticCompositionLocalOf { LabTypography() }
private val LocalLabSpacing = staticCompositionLocalOf { LabSpacing() }
private val LocalLabShapes = staticCompositionLocalOf { LabShapes() }
private val LocalLabMotion = staticCompositionLocalOf { LabMotion() }

/**
 * Entry point to the design system. Components read tokens through [LabTheme],
 * e.g. LabTheme.colors.accent — never via raw values.
 */
object LabTheme {
    val colors: LabColors
        @Composable @ReadOnlyComposable get() = LocalLabColors.current
    val typography: LabTypography
        @Composable @ReadOnlyComposable get() = LocalLabTypography.current
    val spacing: LabSpacing
        @Composable @ReadOnlyComposable get() = LocalLabSpacing.current
    val shapes: LabShapes
        @Composable @ReadOnlyComposable get() = LocalLabShapes.current
    val motion: LabMotion
        @Composable @ReadOnlyComposable get() = LocalLabMotion.current
}

@Composable
fun LabTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkLabColors else LightLabColors
    CompositionLocalProvider(
        LocalLabColors provides colors,
        LocalLabTypography provides LabTypography(),
        LocalLabSpacing provides LabSpacing(),
        LocalLabShapes provides LabShapes(),
        LocalLabMotion provides LabMotion(),
        content = content,
    )
}
