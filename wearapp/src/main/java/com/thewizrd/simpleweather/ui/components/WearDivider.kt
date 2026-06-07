package com.thewizrd.simpleweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx

@Preview
@Composable
fun WearDivider() {
    val ctx = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val strokeWidth = ctx.dpToPx(3f)
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(0f, strokeWidth * 2))

        Canvas(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
        ) {
            drawLine(
                color = Color.White,
                strokeWidth = strokeWidth,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                cap = StrokeCap.Round,
                pathEffect = pathEffect,
            )
        }
    }
}