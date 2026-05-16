package com.brunos3d.wearosclaude.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_MEDIUM
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.brunos3d.wearosclaude.AppGraph
import com.brunos3d.wearosclaude.MainActivity
import com.brunos3d.wearosclaude.data.Mood
import com.brunos3d.wearosclaude.data.UsagePayload
import com.brunos3d.wearosclaude.mascot.MascotFrames
import com.brunos3d.wearosclaude.ui.formatReset
import com.brunos3d.wearosclaude.ui.progressBar
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val RESOURCES_VERSION = "1"
private const val RETRO_ORANGE = 0xFFD77757.toInt()
private const val RETRO_ORANGE_DIM = 0xFF7A3C2B.toInt()
private const val RETRO_RED = 0xFFFF5560.toInt()
private const val RETRO_MID = 0xFF9AA0A6.toInt()
private const val RETRO_BG = 0xFF000000.toInt()

private const val ID_OPEN_APP = "open_app"

/**
 * Wear OS Tile rendering the Claude Code usage dashboard.
 *
 * Same data + mascot as the full Activity, but laid out as a static
 * ProtoLayout snapshot the system can render in the carousel without
 * waking our process every second.
 */
class UsageTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        val future = SettableFuture.create<TileBuilders.Tile>()
        scope.launch {
            val payload = AppGraph.repository(applicationContext).load(force = false)
            future.set(buildTile(payload))
        }
        return future
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> {
        val future = SettableFuture.create<ResourceBuilders.Resources>()
        future.set(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build(),
        )
        return future
    }

    private fun buildTile(payload: UsagePayload): TileBuilders.Tile {
        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        Layout.Builder().setRoot(tileRoot(payload)).build(),
                    )
                    .build(),
            )
            .build()
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .setFreshnessIntervalMillis(FRESH_MS)
            .build()
    }

    private fun tileRoot(payload: UsagePayload): LayoutElementBuilders.LayoutElement {
        val mood = Mood.fromWire(payload.mood)
        val mascot = MascotFrames.framesFor(mood)
        val mascotFrame = mascot.first()
        val accent = accentFor(mood)

        val launchActivity = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(applicationContext.packageName)
                    .setClassName(MainActivity::class.java.name)
                    .build(),
            )
            .build()
        val clickRefresh = Clickable.Builder()
            .setId(ID_OPEN_APP)
            .setOnClick(launchActivity)
            .build()

        return Box.Builder()
            .setWidth(androidx.wear.protolayout.DimensionBuilders.expand())
            .setHeight(androidx.wear.protolayout.DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(RETRO_BG))
                            .build(),
                    )
                    .setPadding(
                        Padding.Builder()
                            .setStart(dp(10f))
                            .setEnd(dp(10f))
                            .setTop(dp(10f))
                            .setBottom(dp(10f))
                            .build(),
                    )
                    .setClickable(clickRefresh)
                    .build(),
            )
            .addContent(
                Column.Builder()
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .setWidth(androidx.wear.protolayout.DimensionBuilders.expand())
                    .addContent(monoText("claude.watch", 10f, accent).build())
                    .addContent(Spacer.Builder().setHeight(dp(2f)).build())
                    .addContent(mascotBox(mascotFrame, accent))
                    .addContent(monoText(MascotFrames.tagline(mood), 11f, accent).build())
                    .addContent(Spacer.Builder().setHeight(dp(4f)).build())
                    .addContent(statBlock("CURR", payload.currentUsagePercent, payload.currentResetAt, accent))
                    .addContent(Spacer.Builder().setHeight(dp(2f)).build())
                    .addContent(statBlock("WEEK", payload.weeklyUsagePercent, payload.weeklyResetAt, RETRO_MID))
                    .build(),
            )
            .build()
    }

    private fun mascotBox(frame: String, accent: Int): LayoutElementBuilders.LayoutElement {
        val column = Column.Builder()
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setWidth(androidx.wear.protolayout.DimensionBuilders.expand())
        for (line in frame.split('\n')) {
            column.addContent(monoText(line, 12f, accent).build())
        }
        return column.build()
    }

    private fun statBlock(label: String, percent: Int, resetAt: Long, accent: Int): LayoutElementBuilders.LayoutElement {
        val reset = formatReset(resetAt)
        return Column.Builder()
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setWidth(androidx.wear.protolayout.DimensionBuilders.expand())
            .addContent(
                Row.Builder()
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(monoText(label, 10f, RETRO_ORANGE_DIM).build())
                    .addContent(Spacer.Builder().setWidth(dp(4f)).build())
                    .addContent(monoText(progressBar(percent), 11f, accent).build())
                    .addContent(Spacer.Builder().setWidth(dp(4f)).build())
                    .addContent(monoText("${percent}%", 10f, accent).build())
                    .build(),
            )
            .addContent(
                monoText("Resets ${reset.time}", 8f, RETRO_MID).build(),
            )
            .build()
    }

    private fun monoText(value: String, sizeSp: Float, color: Int): Text.Builder {
        return Text.Builder()
            .setText(value)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(sizeSp))
                    .setWeight(FONT_WEIGHT_MEDIUM)
                    .setColor(argb(color))
                    .build(),
            )
            .setMaxLines(1)
            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_TRUNCATE)
    }

    private fun accentFor(mood: Mood): Int = when (mood) {
        Mood.Overloaded, Mood.Offline -> RETRO_RED
        else -> RETRO_ORANGE
    }

    companion object {
        private const val FRESH_MS = 60_000L
    }
}
