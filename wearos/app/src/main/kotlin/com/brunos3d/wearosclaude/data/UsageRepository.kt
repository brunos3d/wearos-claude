package com.brunos3d.wearosclaude.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide cache + in-flight deduplication for the `/usage` payload.
 *
 * Three surfaces (Tile, Complication, Activity) feed off the same singleton
 * so a flurry of wake-ups produces one network round-trip — not three. The
 * repo also tracks a `ConnectionState` so the UI can render a precise
 * "what just went wrong?" strip without needing logcat.
 */
class UsageRepository(
    private val api: UsageApi,
    private val maxStaleMs: Long = 60_000,
) {
    private data class Slot(val payload: UsagePayload, val fetchedAt: Long)

    private val slot = MutableStateFlow<Slot?>(null)
    private val connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    private val mutex = Mutex()

    /** Read-only stream of the latest payload (null until first success). */
    val payloads: Flow<UsagePayload?> = slot.map { it?.payload }

    /** Read-only stream of "what does the network look like right now?". */
    val connectionState: StateFlow<ConnectionState> = connection.asStateFlow()

    /**
     * Returns a payload, fetching only when the cache is older than [maxStaleMs]
     * or [force] is true. The repository never throws — failure is reported via
     * `connectionState` and the previous payload (or [UsagePayload.OFFLINE]) is
     * returned so the UI can keep rendering something useful.
     */
    suspend fun load(force: Boolean = false): UsagePayload {
        val cached = slot.value
        val now = System.currentTimeMillis()
        if (!force && cached != null && now - cached.fetchedAt < maxStaleMs) {
            return cached.payload
        }
        mutex.withLock {
            val again = slot.value
            if (!force && again != null && System.currentTimeMillis() - again.fetchedAt < maxStaleMs) {
                return again.payload
            }
            connection.value = ConnectionState.Connecting()
            return when (val result = api.fetchUsage()) {
                is UsageApi.UsageResult.Ok -> {
                    slot.value = Slot(result.payload, System.currentTimeMillis())
                    connection.value = ConnectionState.Online(httpStatus = result.httpStatus)
                    result.payload
                }
                is UsageApi.UsageResult.Unauthorized -> {
                    connection.value = ConnectionState.Unauthorized()
                    // Probe /ping anonymously so the UI can hint whether
                    // auth is really the problem vs general unreachability.
                    cached?.payload ?: UsagePayload.OFFLINE
                }
                is UsageApi.UsageResult.Failed -> {
                    connection.value = ConnectionState.Offline(
                        reason = result.message,
                        httpStatus = result.httpStatus,
                    )
                    cached?.payload ?: UsagePayload.OFFLINE
                }
            }
        }
    }

    /**
     * Anonymous reachability probe. Used by the Settings/diagnostics screen
     * to confirm "the server is up, you just need a token" vs "the URL is
     * dead".
     */
    suspend fun ping(): UsageApi.PingResult = api.ping()

    fun snapshot(): UsagePayload? = slot.value?.payload

    suspend fun current(): UsagePayload = snapshot() ?: load()
}
