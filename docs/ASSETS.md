# Mascot assets

The mascot is shipped as **text**, not bitmaps. The canonical silhouette
comes straight from upstream Claude Code:

```
  ▐▛███▜▌
 ▝▜█████▛▘
   ▘▘ ▝▝
```

Every mood is a tiny loop of frames defined in
`wearos/app/src/main/kotlin/com/brunos3d/wearosclaude/mascot/MascotFrames.kt`.

## Adding a mood

1. Add the wire value to the `Mood` union in `shared/usage-contract.ts` and
   the `Mood` enum in `wearos/.../data/Mood.kt`.
2. Map the new mood inside `backend/src/lib/mood.ts → deriveMood`.
3. Define a frame loop in `MascotFrames`. Each frame **must** be 4 lines,
   each line **must** be at least 11 chars (will be padded automatically).
4. Add a tagline in `MascotFrames.tagline(...)`.

## Grid rules

- 4 rows × 11 cols, monospaced.
- The top row is "emote space" — clouds, dots, `?`, `!!!`.
- Rows 2-3 are the body. Keep the silhouette intact unless the mood
  genuinely warrants distortion (`overloaded`, `sleeping`).
- Row 4 is the feet/cursor row — small perturbations here read as
  "breathing" without distracting.

## Why text and not pixel art bitmaps

- Same source of truth between Tile (ProtoLayout `Text`) and Compose
  (`Text`) — no resource sync to maintain.
- Trivial diffs in code review; ships as a few KB of UTF-8.
- Re-rendered in the user's preferred mono font, so it inherits the
  system pixel density automatically.

If we ever need pixel-perfect bitmaps (eg. for a watch face overlay), the
straightforward path is to rasterise these strings at build time into PNGs
inside `wearos/app/src/main/res/drawable/`.
