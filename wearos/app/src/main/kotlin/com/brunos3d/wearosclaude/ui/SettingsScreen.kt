package com.brunos3d.wearosclaude.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.brunos3d.wearosclaude.data.UsageApi
import com.brunos3d.wearosclaude.data.UsageRepository
import com.brunos3d.wearosclaude.data.Settings
import kotlinx.coroutines.launch

/**
 * Minimal config + diagnostics surface. Tap-to-edit each field via Wear
 * RemoteInput (voice or on-watch keyboard), then "test" to confirm the
 * backend is reachable.
 */
@Composable
fun SettingsScreen(
    backendUrl: String,
    authToken: String?,
    repository: UsageRepository,
    settings: Settings,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mono = FontFamily.Monospace
    val scope = rememberCoroutineScope()
    var pingStatus by remember { mutableStateOf<String?>(null) }
    var usageStatus by remember { mutableStateOf<String?>(null) }

    val urlLauncher = rememberRemoteInputLauncher(
        label = "backend url",
        initial = backendUrl,
    ) { result ->
        if (!result.isNullOrBlank()) {
            scope.launch { settings.setBackendUrl(result.trim()) }
        }
    }
    val tokenLauncher = rememberRemoteInputLauncher(
        label = "auth token",
        initial = "",
    ) { result ->
        scope.launch {
            // empty string clears the token, anything else replaces it
            settings.setAuthToken(result?.takeIf { it.isNotBlank() })
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RetroPalette.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("> config", color = RetroPalette.Orange, fontFamily = mono, fontSize = 13.sp)

        DiagnosticRow("URL", backendUrl.replace("http://", ""), RetroPalette.Mid, mono)
        DiagnosticRow(
            "AUTH",
            authToken?.let { it.take(4) + "…" + it.takeLast(2) } ?: "(none)",
            if (!authToken.isNullOrBlank()) RetroPalette.Cyan else RetroPalette.OrangeDim,
            mono,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Chip(
            onClick = { urlLauncher.launch(Unit) },
            colors = ChipDefaults.primaryChipColors(
                backgroundColor = RetroPalette.OrangeDim,
                contentColor = RetroPalette.Orange,
            ),
            label = { Text("edit url", fontFamily = mono, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
        )
        Chip(
            onClick = { tokenLauncher.launch(Unit) },
            colors = ChipDefaults.primaryChipColors(
                backgroundColor = RetroPalette.OrangeDim,
                contentColor = RetroPalette.Orange,
            ),
            label = { Text("edit token", fontFamily = mono, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Chip(
            onClick = {
                scope.launch {
                    pingStatus = "…ping"
                    pingStatus = when (val r = repository.ping()) {
                        is UsageApi.PingResult.Ok -> "ok ${r.httpStatus}${if (r.authRequired) " (auth req)" else ""}"
                        is UsageApi.PingResult.Failed -> "fail ${r.message}"
                    }
                }
            },
            colors = ChipDefaults.primaryChipColors(
                backgroundColor = RetroPalette.OrangeDim,
                contentColor = RetroPalette.Orange,
            ),
            label = { Text("test /ping", fontFamily = mono, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
        )
        pingStatus?.let {
            Text(
                text = "ping: $it",
                color = RetroPalette.Mid,
                fontFamily = mono,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }

        Chip(
            onClick = {
                scope.launch {
                    usageStatus = "…fetch"
                    val payload = repository.load(force = true)
                    val conn = repository.connectionState.value
                    usageStatus = describe(conn, payload.status)
                }
            },
            colors = ChipDefaults.primaryChipColors(
                backgroundColor = RetroPalette.OrangeDim,
                contentColor = RetroPalette.Orange,
            ),
            label = { Text("test /usage", fontFamily = mono, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
        )
        usageStatus?.let {
            Text(
                text = "usage: $it",
                color = RetroPalette.Mid,
                fontFamily = mono,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Chip(
            onClick = onCloseClicked,
            colors = ChipDefaults.primaryChipColors(
                backgroundColor = RetroPalette.Orange,
                contentColor = RetroPalette.Bg,
            ),
            label = { Text("back", fontFamily = mono, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, mono: FontFamily) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = RetroPalette.OrangeDim,
            fontFamily = mono,
            fontSize = 9.sp,
        )
        Text(
            text = value,
            color = valueColor,
            fontFamily = mono,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun describe(conn: com.brunos3d.wearosclaude.data.ConnectionState, payloadStatus: String): String = when (conn) {
    is com.brunos3d.wearosclaude.data.ConnectionState.Online -> "ok ${conn.httpStatus} ($payloadStatus)"
    is com.brunos3d.wearosclaude.data.ConnectionState.Unauthorized -> "401 token?"
    is com.brunos3d.wearosclaude.data.ConnectionState.Offline -> "fail ${conn.reason}"
    is com.brunos3d.wearosclaude.data.ConnectionState.Connecting -> "…"
}
