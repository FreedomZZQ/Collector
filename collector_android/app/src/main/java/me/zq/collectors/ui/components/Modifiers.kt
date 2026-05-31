package me.zq.collectors.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.zq.collectors.ui.theme.Radius

/** Dashed rounded border (iOS dashed strokeBorder). */
fun Modifier.dashedBorder(
    color: Color,
    radius: Dp = Radius.r,
    width: Dp = 1.dp,
    dash: Dp = 5.dp,
): Modifier = this.drawBehind {
    val d = dash.toPx()
    val stroke = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(d, d)))
    drawRoundRect(color = color, cornerRadius = CornerRadius(radius.toPx()), style = stroke)
}
