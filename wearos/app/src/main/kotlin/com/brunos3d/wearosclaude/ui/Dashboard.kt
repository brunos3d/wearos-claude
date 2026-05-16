package com.brunos3d.wearosclaude.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.brunos3d.wearosclaude.data.ConnectionState
import com.brunos3d.wearosclaude.data.Mood
import com.brunos3d.wearosclaude.data.UsagePayload
import com.brunos3d.wearosclaude.mascot.MascotFrames

/**
 * Single-screen retro dashboard. Layout:
 *
 *   <mascot>
 *   * <state>
 *   CURR  ▰▰▰▰▱▱▱▱▱▱  40% used
 *   Resets 8:20pm (America/Sao_Paulo)
 *   WEEK  ▰▰▱▱▱▱▱▱▱▱  20% used
 *   Resets May 18, 6:20pm (America/Sao_Paulo)
 *
 * Tap anywhere outside the mascot → force refresh.
 * Tap the mascot → cycle mood, held for ≥5 s by the parent activity.
 * Long-press the mascot → open Settings.
 */
@Composable
fun Dashboard(
    payload: UsagePayload,
    moodOverride: Mood?,
    connection: ConnectionState,
    onTapMascot: () -> Unit,
    onLongPressMascot: () -> Unit,
    onTapRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mono = FontFamily.Monospace
    val mood = moodOverride ?: Mood.fromWire(payload.mood)
    val accent = accentFor(mood, connection)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RetroPalette.Bg)
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .clickable(onClick = onTapRefresh),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Top),
        ) {
            Header(connection = connection, accent = accent, mono = mono)
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onTapMascot() },
                            onLongPress = { onLongPressMascot() },
                        )
                    },
            ) {
                MascotView(mood = mood, accent = accent, fontFamily = mono)
            }
            Text(
                text = MascotFrames.tagline(mood),
                color = accent,
                fontFamily = mono,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            StatBlock(
                label = "CURR",
                percent = payload.currentUsagePercent,
                resetAt = payload.currentResetAt,
                accent = accent,
                mono = mono,
            )
            StatBlock(
                label = "WEEK",
                percent = payload.weeklyUsagePercent,
                resetAt = payload.weeklyResetAt,
                // The weekly bar stays muted to keep visual hierarchy: the
                // current 5h window is the load-bearing number.
                accent = RetroPalette.Mid,
                mono = mono,
            )
        }
    }
}

@Composable
private fun Header(connection: ConnectionState, accent: Color, mono: FontFamily) {
    val statusGlyph = when (connection) {
        is ConnectionState.Online -> "●"
        is ConnectionState.Connecting -> "◌"
        is ConnectionState.Unauthorized -> "⚠"
        is ConnectionState.Offline -> "✗"
    }
    val transition = rememberInfiniteTransition(label = "cursor")
    val blink by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursor-blink",
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "claude.watch",
            color = accent,
            fontFamily = mono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusGlyph,
            color = accent,
            fontFamily = mono,
            fontSize = 11.sp,
            modifier = Modifier.alpha(blink),
        )
    }
}

@Composable
private fun StatBlock(
    label: String,
    percent: Int,
    resetAt: Long,
    accent: Color,
    mono: FontFamily,
) {
    val reset = formatReset(resetAt)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = RetroPalette.OrangeDim,
                fontFamily = mono,
                fontSize = 10.sp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = progressBar(percent),
                color = accent,
                fontFamily = mono,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${percent}%",
                color = accent,
                fontFamily = mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = "Resets ${reset.time}",
            color = RetroPalette.Mid,
            fontFamily = mono,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Accent rule: orange almost always, red reserved for unambiguous problem
 * states (`overloaded` mood, offline connection, auth error).
 */
private fun accentFor(mood: Mood, connection: ConnectionState): Color = when {
    connection is ConnectionState.Offline || connection is ConnectionState.Unauthorized -> RetroPalette.Red
    mood == Mood.Offline -> RetroPalette.Red
    mood == Mood.Overloaded -> RetroPalette.Red
    else -> RetroPalette.Orange
}
