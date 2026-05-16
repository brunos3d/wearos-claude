package com.brunos3d.wearosclaude

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.tiles.TileService
import com.brunos3d.wearosclaude.data.ConnectionState
import com.brunos3d.wearosclaude.data.Mood
import com.brunos3d.wearosclaude.data.UsagePayload
import com.brunos3d.wearosclaude.tile.UsageTileService
import com.brunos3d.wearosclaude.ui.Dashboard
import com.brunos3d.wearosclaude.ui.SettingsScreen
import com.brunos3d.wearosclaude.ui.WearOsClaudeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single Activity. Polls the backend, drives the dashboard, hosts the tap
 * interactions, kicks the Tile to refresh, and gates a Settings screen.
 *
 * Long-pressing the mascot opens Settings; tapping it cycles mood; tapping
 * anywhere else on the dashboard forces a refresh.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearOsClaudeTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val repository = remember(context) { AppGraph.repository(context) }
    val settings = remember(context) { AppGraph.settings(context) }
    val scope = rememberCoroutineScope()

    var payload by remember { mutableStateOf(UsagePayload.OFFLINE) }
    var override by remember { mutableStateOf<Mood?>(null) }
    // Single revert Job so rapid taps reset the 5 s hold instead of letting
    // the previous tap's coroutine clear `override` prematurely.
    var revertJob by remember { mutableStateOf<Job?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val settingsSnapshot by settings.flow.collectAsState(initial = null)
    val connection by repository.connectionState.collectAsState()

    LaunchedEffect(Unit) {
        // Cold-load + poll loop. We re-read every 60 s while in foreground so
        // the dashboard stays close to fresh — the repository's internal
        // 60 s cache means we don't actually re-hit the network twice if the
        // tile pulled it for us in between.
        while (true) {
            val fresh = repository.load(force = false)
            payload = fresh
            TileService.getUpdater(context).requestUpdate(UsageTileService::class.java)
            delay(60_000L)
        }
    }

    if (showSettings) {
        SettingsScreen(
            backendUrl = settingsSnapshot?.backendUrl.orEmpty(),
            authToken = settingsSnapshot?.authToken,
            repository = repository,
            settings = settings,
            onCloseClicked = {
                showSettings = false
                // Settings change probably means the URL/token moved — force
                // a fresh fetch so the user immediately sees if it worked.
                scope.launch {
                    payload = repository.load(force = true)
                    TileService.getUpdater(context).requestUpdate(UsageTileService::class.java)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Dashboard(
            payload = payload,
            moodOverride = override,
            connection = connection,
            onTapMascot = {
                override = nextMood(override ?: Mood.fromWire(payload.mood))
                revertJob?.cancel()
                revertJob = scope.launch {
                    delay(MASCOT_HOLD_MS)
                    override = null
                    revertJob = null
                }
            },
            onLongPressMascot = { showSettings = true },
            onTapRefresh = {
                scope.launch {
                    payload = repository.load(force = true)
                    TileService.getUpdater(context).requestUpdate(UsageTileService::class.java)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * How long a tap-selected mood sticks before reverting to the backend-driven
 * default. Resets on every tap.
 */
private const val MASCOT_HOLD_MS = 5_000L

private fun nextMood(current: Mood): Mood {
    val order = listOf(
        Mood.Musing,
        Mood.Thinking,
        Mood.Coding,
        Mood.Compiling,
        Mood.Debugging,
        Mood.Overloaded,
        Mood.Sleeping,
        Mood.Idle,
    )
    val idx = order.indexOf(current).coerceAtLeast(-1)
    return order[(idx + 1) % order.size]
}
