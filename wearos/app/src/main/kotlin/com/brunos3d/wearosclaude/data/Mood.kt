package com.brunos3d.wearosclaude.data

/**
 * Strongly-typed projection of the `mood` string sent by the backend.
 *
 * `idle` is the default fall-through so missing or unknown values from older
 * servers degrade gracefully.
 */
enum class Mood(val wire: String, val label: String) {
    Idle("idle", "idle"),
    Musing("musing", "musing"),
    Thinking("thinking", "thinking"),
    Coding("coding", "coding"),
    Compiling("compiling", "compiling"),
    Debugging("debugging", "debugging"),
    Overloaded("overloaded", "overloaded"),
    Sleeping("sleeping", "sleeping"),
    Offline("offline", "offline");

    companion object {
        fun fromWire(value: String?): Mood = entries.firstOrNull { it.wire == value } ?: Idle
    }
}
