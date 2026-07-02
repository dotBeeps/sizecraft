# SizeCraft

NeoForge mod (Minecraft 1.21.5, NeoForge 21.5.96, Java 21) written in Kotlin
via Kotlin for Forge. Player scaling, capture of smaller players into items,
and a personal pocket dimension ("hammerspace") with named compartments.
Cloth Config provides the in-game settings screens.

## Commands

All dev tasks go through `just`. Run `just` to list recipes.
Key recipes: `setup` (hooks + commit template) · `ci` (non-mutating full
gate: gradle check + build) · `check` · `test` · `build` · `run` (dev
client). No formatter/linter is configured (no ktlint/spotless/detekt), so
there are deliberately no `lint`/`format` recipes.
Gradle here is slow (NeoGradle dev env): prefer `test` for quick loops.

## Architecture

- `SizeCraftMod.kt` — common entry point; `SizeCraftClient.kt` — client-only
  setup (renderers, config screen hookup).
- `registry/` — deferred registration: blocks, items, data components, menu
  types, the hammerspace dimension keys, and permission nodes.
- `player/` — `SizeData` attachment + `SizeEvents`: step-based size math
  (scale = 6^steps), clamping, and application to the player.
- `capture/` — capturing smaller players into `captured_player` items and
  managing captives (`CaptureManager`, `CaptureEvents`).
- `dimension/` — hammerspace layout, compartment entries, and populator.
- `network/` — S2C sync packets (`SizeSyncPacket`, `CaptureStatusPacket`,
  both registered `playToClient`).
- `command/`, `config/`, `gui/`, `rendering/` — `/sizecraft` +
  `/hammerspace` commands, Cloth Config screens, hammerspace view
  menu/screen, size animation and captured-player rendering.
- `src/main/resources/data/sizecraft/` — dimension, biome, and noise JSON
  for the hammerspace void world.

## Conventions

- Conventional commits (template in `.gitmessage`; enforced by the lefthook
  commit-msg hook).
- Sizes are step-based end to end: commands and config bounds take steps
  (−10..10), `SizeEvents.clampSteps` is the single clamp point. Don't
  reintroduce raw scale floats at API boundaries; convert via 6^steps.
- Verification gates run with the Gradle configuration cache disabled
  (`-Dorg.gradle.configuration-cache=false`) — keep that flag when running
  `check` outside `just`.
- `META-INF/neoforge.mods.toml` is templated at build time from
  `gradle.properties` (mod id, version, MC/Neo ranges) — edit
  `gradle.properties`, not the toml.
- `src/generated/resources` is a resource root populated by the
  `clientData` datagen run; don't hand-edit generated files.
