package com.alveon.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Frosted-glass look: translucent white gradient with a hairline border. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(22.dp)): Modifier =
    this.clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0x47FFFFFF), Color(0x1FFFFFFF))))
        .border(1.dp, Color(0x38FFFFFF), shape)
