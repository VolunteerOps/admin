package com.example.admin.core.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TacticalLoader(
    modifier: Modifier = Modifier,
    statusText: String = "Synchronizing response data and team updates..."
) {
    val overlock = getOverlockFontFamily()

    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 6.dp.toPx()

                drawCircle(
                    color = TacticalBorderStroke,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                rotate(rotationAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                TacticalOliveGreen.copy(alpha = 0.1f),
                                TacticalOliveGreen.copy(alpha = 0.6f),
                                TacticalOliveGreen
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp * pulseScale)
                    .clip(CircleShape)
                    .liquidGlass(tint = TacticalOliveGreen, shape = CircleShape)
                    .drawWithCache {
                        onDrawBehind {
                            drawCircle(
                                color = TacticalOliveGreen.copy(alpha = 0.25f),
                                radius = size.minDimension / 2f
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(TacticalOliveGreen)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusText,
            fontFamily = overlock,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = TacticalTextSecondary
        )
    }
}
