package com.brunos3d.wearosclaude.ui

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Human-friendly absolute reset times.
 *
 * Same day → "8:20pm".
 * Same year, different day → "May 18, 11pm".
 * Different year → "May 18 2027, 11pm".
 *
 * Always rendered in the watch's system default timezone, and the IANA
 * zone id is returned alongside so the UI can annotate it.
 */
data class FormattedReset(val time: String, val zoneId: String)

// NOTE: Locale.ROOT yields `M05` instead of `May` because CLDR's root data
// has no localized month names — fall back to English which always does.
private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
private val sameYearFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mma", Locale.ENGLISH)
private val fullFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d yyyy, h:mma", Locale.ENGLISH)

fun formatReset(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): FormattedReset {
    if (epochMs <= 0L) return FormattedReset("—", zone.id)
    val target = Instant.ofEpochMilli(epochMs).atZone(zone)
    val now = ZonedDateTime.now(zone)
    val fmt = when {
        target.toLocalDate() == now.toLocalDate() -> timeFmt
        target.year == now.year -> sameYearFmt
        else -> fullFmt
    }
    val text = target.format(fmt).lowercase(Locale.ROOT)
    return FormattedReset(text, zone.id)
}

/**
 * Pure-ASCII progress bar. Always renders exactly [width] cells so callers
 * can rely on a stable visual width (no jitter as the percentage climbs).
 */
fun progressBar(percent: Int, width: Int = 10): String {
    val safe = percent.coerceIn(0, 100)
    val filled = (safe * width + 50) / 100   // round-half-up
    return buildString {
        repeat(filled) { append('▰') }
        repeat(width - filled) { append('▱') }
    }
}
