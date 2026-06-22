package com.uilab.showcase.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class LabTypography(
    val title: TextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    val subtitle: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    val body: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    val label: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
)
