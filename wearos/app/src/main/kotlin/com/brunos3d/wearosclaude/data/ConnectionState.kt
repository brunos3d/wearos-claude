package com.brunos3d.wearosclaude.data

/**
 * Coarse-grained "did the last fetch work?" enum, intentionally separate
 * from the payload itself so the UI can render a diagnostic strip even
 * when the backend is unreachable.
 */
sealed interface ConnectionState {
    val timestamp: Long

    data class Connecting(override val timestamp: Long = System.currentTimeMillis()) : ConnectionState

    data class Online(
        override val timestamp: Long = System.currentTimeMillis(),
        val httpStatus: Int = 200,
    ) : ConnectionState

    data class Unauthorized(
        override val timestamp: Long = System.currentTimeMillis(),
    ) : ConnectionState

    data class Offline(
        override val timestamp: Long = System.currentTimeMillis(),
        val reason: String,
        val httpStatus: Int? = null,
    ) : ConnectionState

    companion object {
        val Idle: ConnectionState = Connecting(timestamp = 0L)
    }
}
