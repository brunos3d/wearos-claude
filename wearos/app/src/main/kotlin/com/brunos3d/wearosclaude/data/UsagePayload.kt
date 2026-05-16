package com.brunos3d.wearosclaude.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format returned by `GET /usage` on the local backend. Mirrors
 * `shared/usage-contract.ts` — keep in sync.
 */
@Serializable
data class UsagePayload(
    val currentUsagePercent: Int = 0,
    val weeklyUsagePercent: Int = 0,
    val currentResetIn: String = "—",
    val weeklyResetIn: String = "—",
    val currentResetAt: Long = 0L,
    val weeklyResetAt: Long = 0L,
    val status: String = "offline",
    val mood: String = "idle",
    val lastUpdated: Long = 0L,
    val extras: Extras? = null,
) {
    @Serializable
    data class Extras(
        val source: String = "oauth-probe",
        @SerialName("currentWindowMinutes") val currentWindowMinutes: Int = 300,
        @SerialName("sessionsActive") val sessionsActive: Int = 0,
        @SerialName("tokensInWindow") val tokensInWindow: Long = 0L,
        @SerialName("tokensWeekly") val tokensWeekly: Long = 0L,
        @SerialName("messagesInWindow") val messagesInWindow: Int = 0,
        @SerialName("messagesWeekly") val messagesWeekly: Int = 0,
        val plan: String? = null,
    )

    companion object {
        val OFFLINE = UsagePayload(status = "offline", mood = "offline")
    }
}
