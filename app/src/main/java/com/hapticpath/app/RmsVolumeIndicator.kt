package com.hapticpath.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RmsVolumeIndicator(
    rmsVolume: Float, // Värde mellan 0.0f och 1.0f från AudioRecorder
    modifier: Modifier = Modifier
) {
    // Skala från 1.0x (tystnad) upp till 2.2x (max volym)
    val targetScale = 1.0f + (rmsVolume * 1.2f)

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 60),
        label = "RmsScaleAnimation"
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Yttre pulserande cirkel
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(animatedScale)
                .background(
                    color = Color(0xFF00E676).copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
        // Inre fast cirkel
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = Color(0xFF00E676),
                    shape = CircleShape
                )
        )
    }
}