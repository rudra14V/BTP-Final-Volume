package com.example.btp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun VoiceWaveformVisualizer(
    isRecording: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phaseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .height(100.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = width / (barCount * 2f)
            val spacing = barWidth

            for (i in 0 until barCount) {
                val x = i * (barWidth + spacing) + spacing / 2
                val normalizedIdx = i.toFloat() / barCount
                val waveFactor = sin(normalizedIdx * Math.PI.toFloat() * 3 + phaseAnim)
                val currentAmp = if (isRecording) {
                    (0.2f + amplitude * 0.8f) * (0.4f + 0.6f * abs(waveFactor))
                } else {
                    0.08f
                }
                val barHeight = (height * currentAmp.coerceIn(0.05f, 1.0f)).coerceAtLeast(6f)
                val y = (height - barHeight) / 2f

                drawRoundRect(
                    color = if (isRecording) activeColor else inactiveColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

private fun abs(value: Float): Float = if (value < 0f) -value else value

@Composable
fun PulsingMicVisualizer(
    isRecording: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier,
    pulseColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val currentScale = if (isRecording) (scalePulse + amplitude * 0.5f).coerceIn(1f, 2f) else 1f

    Canvas(modifier = modifier.size(140.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.width / 3f

        if (isRecording) {
            drawCircle(
                color = pulseColor.copy(alpha = 0.35f),
                radius = baseRadius * currentScale,
                center = center
            )
            drawCircle(
                color = pulseColor.copy(alpha = 0.6f),
                radius = baseRadius * (1f + amplitude * 0.3f),
                center = center
            )
        }
        drawCircle(
            color = pulseColor,
            radius = baseRadius,
            center = center
        )
    }
}
