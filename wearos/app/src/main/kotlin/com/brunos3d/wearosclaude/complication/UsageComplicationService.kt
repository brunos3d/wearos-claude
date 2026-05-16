package com.brunos3d.wearosclaude.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import com.brunos3d.wearosclaude.AppGraph
import com.brunos3d.wearosclaude.MainActivity
import com.brunos3d.wearosclaude.R
import com.brunos3d.wearosclaude.data.Mood
import com.brunos3d.wearosclaude.data.UsagePayload
import androidx.wear.watchface.complications.data.ComplicationData as ComplicationDataBase
import android.graphics.drawable.Icon as DrawableIcon

/**
 * Watch face complication exposing the current rolling-window usage percent.
 *
 * Supports both SHORT_TEXT (`50%`) and RANGED_VALUE (gauge) so any face that
 * wants either flavor can pick us up.
 */
class UsageComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val preview = UsagePayload(
            currentUsagePercent = 42,
            weeklyUsagePercent = 11,
            currentResetIn = "1h22m",
            weeklyResetIn = "6d8h",
            status = "active",
            mood = "musing",
        )
        return buildData(type, preview)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val payload = AppGraph.repository(applicationContext).load(force = false)
        return buildData(request.complicationType, payload)
    }

    private fun buildData(type: ComplicationType, payload: UsagePayload): ComplicationData? {
        val mood = Mood.fromWire(payload.mood)
        val percent = payload.currentUsagePercent.coerceIn(0, 100)
        val tap = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = MonochromaticImage.Builder(
            DrawableIcon.createWithResource(this, R.drawable.ic_complication),
        ).build()
        val description = PlainComplicationText.Builder("Claude usage $percent%").build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("$percent%").build(),
                contentDescription = description,
            )
                .setMonochromaticImage(icon)
                .setTitle(PlainComplicationText.Builder(mood.label.take(7)).build())
                .setTapAction(tap)
                .build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = percent.toFloat(),
                min = 0f,
                max = 100f,
                contentDescription = description,
            )
                .setText(PlainComplicationText.Builder("$percent%").build())
                .setTitle(PlainComplicationText.Builder("claude").build())
                .setMonochromaticImage(icon)
                .setTapAction(tap)
                .build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = icon,
                contentDescription = description,
            ).setTapAction(tap).build()

            else -> null
        }
    }
}
