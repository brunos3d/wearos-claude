package com.brunos3d.wearosclaude.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.brunos3d.wearosclaude.data.Mood
import com.brunos3d.wearosclaude.mascot.MascotFrames
import kotlinx.coroutines.delay

/**
 * Animated ASCII mascot. Cycles through the frames declared in
 * `MascotFrames` for the given mood, with a slow cursor flicker on the
 * last row to give the body a touch of "alive".
 */
@Composable
fun MascotView(
    mood: Mood,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Monospace,
    accent: Color = RetroPalette.Orange,
    frameMillis: Long = 380L,
) {
    val frames = remember(mood) { MascotFrames.framesFor(mood) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(mood, frames) {
        while (true) {
            delay(frameMillis)
            tick = (tick + 1) % frames.size
        }
    }
    val frame = frames[tick.coerceAtMost(frames.lastIndex)]
    // Subtle alpha pulse — drives the "phosphor glow" feel without burning
    // pixels permanently in any one spot.
    val pulse by animateFloatAsState(
        targetValue = if (tick % 2 == 0) 1f else 0.82f,
        animationSpec = tween(durationMillis = frameMillis.toInt(), easing = LinearEasing),
        label = "mascot-pulse",
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        for (line in frame.split('\n')) {
            Text(
                text = line,
                color = accent,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(pulse)
                    .fillMaxWidth(),
            )
        }
    }
}
