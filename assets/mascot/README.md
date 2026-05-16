# Mascot frames

Frames are defined as Kotlin string constants — not bitmaps — in:

    wearos/app/src/main/kotlin/com/brunos3d/wearosclaude/mascot/MascotFrames.kt

See [`../../docs/ASSETS.md`](../../docs/ASSETS.md) for the grid rules and how
to add a new mood. This directory is reserved for any future PNG/SVG export
work (e.g. rasterizing the frames for a watch-face overlay), but is empty
today on purpose — the Kotlin file is the source of truth.
