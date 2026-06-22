package com.uilab.showcase.designsystem.token

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class LabShapes(
    val small: RoundedCornerShape = RoundedCornerShape(10.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(18.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)
