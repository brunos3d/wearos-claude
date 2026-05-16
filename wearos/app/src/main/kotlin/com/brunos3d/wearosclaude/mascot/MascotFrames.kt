package com.brunos3d.wearosclaude.mascot

import com.brunos3d.wearosclaude.data.Mood

/**
 * Pixel/ASCII frames for the Claude Code mascot.
 *
 * The canonical silhouette (provided by upstream Claude Code) is:
 * ```
 *   ▐▛███▜▌
 *  ▝▜█████▛▘
 *    ▘▘ ▝▝
 * ```
 *
 * Every mood is a short loop of frames. We keep them as String arrays so the
 * Tile (ProtoLayout text) and the Activity (Compose Text) can share the same
 * canon — and so we don't have to ship bitmaps that would bloat the APK.
 */
object MascotFrames {

    /** Number of character columns / rows in each frame. Kept fixed for layout stability. */
    const val COLS = 11
    const val ROWS = 4

    /** Mood-aware frame loop. Caller picks the active frame via `(tick % size)`. */
    fun framesFor(mood: Mood): List<String> = when (mood) {
        Mood.Idle -> IDLE
        Mood.Musing -> MUSING
        Mood.Thinking -> THINKING
        Mood.Coding -> CODING
        Mood.Compiling -> COMPILING
        Mood.Debugging -> DEBUGGING
        Mood.Overloaded -> OVERLOADED
        Mood.Sleeping -> SLEEPING
        Mood.Offline -> OFFLINE
    }

    fun tagline(mood: Mood): String = when (mood) {
        Mood.Idle -> "* idle"
        Mood.Musing -> "* musing..."
        Mood.Thinking -> "* thinking?"
        Mood.Coding -> "* coding"
        Mood.Compiling -> "* compiling"
        Mood.Debugging -> "* debugging!"
        Mood.Overloaded -> "* overloaded"
        Mood.Sleeping -> "* zzz..."
        Mood.Offline -> "x offline"
    }

    /** Standard breathing loop with a tiny vertical bob. */
    private val IDLE = listOf(
        frame(
            "           ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
            "           ",
        ),
        frame(
            "           ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "           ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "  ▝▘▝▘ ▝▝  ",
        ),
    )

    private val MUSING = listOf(
        frame(
            "    .      ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "    . .    ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "    . . .  ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "      . .  ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
    )

    private val THINKING = listOf(
        frame(
            "     ?     ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "      ?    ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "  ▝▘ ▘ ▝▝  ",
        ),
        frame(
            "    ?      ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘ ▝▝▘▝  ",
        ),
    )

    private val CODING = listOf(
        frame(
            "    < >    ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "   < / >   ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "  ▝▘▝▘ ▝▝  ",
        ),
        frame(
            "    < />   ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘▝▘▝▘  ",
        ),
    )

    private val COMPILING = listOf(
        frame(
            "    [▰  ]  ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "    [▰▰ ]  ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "    [▰▰▰]  ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "    [ ▰▰]  ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
    )

    private val DEBUGGING = listOf(
        frame(
            "    !      ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "  ✗ ▘▘ ▝▝  ",
        ),
        frame(
            "      !    ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "   ▘▘✗▝▝   ",
        ),
        frame(
            "     ! !   ",
            "  ▐▛███▜▌  ",
            " ▝▜█████▛▘ ",
            "    ▘▘ ▝✗  ",
        ),
    )

    private val OVERLOADED = listOf(
        frame(
            "   *!!!*   ",
            "  ▐▛█▘▘█▜▌ ",
            " ▝▜█████▛▘ ",
            "  ✗▘▘ ▝▝✗  ",
        ),
        frame(
            "  !!*!*!!  ",
            "  ▐▛▘███▜▌ ",
            " ▝▜█████▛▘ ",
            "  ▘✗ ▝▝✗▘  ",
        ),
        frame(
            "   !!!!!   ",
            "  ▐▛███▘▌  ",
            " ▝▜█████▛▘ ",
            "  ✗▘  ▝▝✗  ",
        ),
    )

    private val SLEEPING = listOf(
        frame(
            "    z      ",
            "  ▐▛▄██▄▜▌ ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "    z z    ",
            "  ▐▛▄██▄▜▌ ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
        frame(
            "   z z z   ",
            "  ▐▛▄██▄▜▌ ",
            " ▝▜█████▛▘ ",
            "   ▘▘ ▝▝   ",
        ),
    )

    private val OFFLINE = listOf(
        frame(
            "    ?      ",
            "  ▐▛░░░▜▌  ",
            " ▝▜░░░░░▛▘ ",
            "   ░░ ░░   ",
        ),
        frame(
            "    ?      ",
            "  ▐▛░ ░▜▌  ",
            " ▝▜░░░░░▛▘ ",
            "   ░░ ░░   ",
        ),
    )

    /** Joins lines and asserts the grid invariant. Keeps the catalog honest. */
    private fun frame(vararg lines: String): String {
        require(lines.size == ROWS) { "frame must have $ROWS lines, got ${lines.size}" }
        return lines.joinToString("\n") { line ->
            // pad to COLS so the column count is stable for layout maths
            if (line.length >= COLS) line else line.padEnd(COLS, ' ')
        }
    }
}
