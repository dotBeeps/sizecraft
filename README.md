# SizeCraft

Shrink your friends, pocket your enemies, redecorate your reality.

## About

SizeCraft is a NeoForge mod for playing with player scale. Shrink down to pocket-sized, stretch up to tower over the landscape, or anything in between. If you're big enough, you can scoop up smaller players, carry them around in your inventory, or eat them — tucking them away into your own personal pocket dimension called a hammerspace. Each hammerspace has named compartments you can customize, and captured players can wiggle their way out with configurable escape mechanics. Everything is consent-driven with predator/prey opt-in and fully adjustable per-server.

## Features

- **Player scaling** (0.001x–100x) with smooth animation and sparkle particles
- **Capture system** — sneak+right-click to pick up or eat players smaller than you (size-ratio gated, with predator/prey role opt-in)
- **Hammerspace dimension** — personal void rooms with named compartments, escape mechanics, and configurable delays
- **Permission-based access control** — granular permission nodes for resizing, capture roles, dimension access, and admin overrides
- **Cloth Config integration** — server and client settings configurable via in-game GUI
- **Full command suite** — `/sizecraft` and `/hammerspace` with subcommands for everything

## Requirements

| Dependency       | Version   |
| ---------------- | --------- |
| Minecraft        | 1.21.5    |
| NeoForge         | 21.5.96+  |
| Kotlin for Forge | 5.11.0+   |
| Cloth Config     | 18.0.145+ |
| Java             | 21        |

## Installation

1. Download the SizeCraft JAR from [Releases](https://github.com/dotBeeps/sizecraft/releases)
2. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.5
3. Install [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge) and [Cloth Config](https://modrinth.com/mod/cloth-config)
4. Drop all JARs into your `mods/` folder

## Commands

### `/sizecraft`

| Subcommand | Usage                               | Description                        |
| ---------- | ----------------------------------- | ---------------------------------- |
| _(scale)_  | `/sizecraft <0.001-100>`            | Set your own scale                 |
| `get`      | `/sizecraft get [player]`           | Get your or another player's scale |
| `set`      | `/sizecraft set <player> <scale>`   | Set another player's scale         |
| `reset`    | `/sizecraft reset [player]`         | Reset scale to default             |
| `min`      | `/sizecraft min <player> <scale>`   | Set a player's minimum scale       |
| `max`      | `/sizecraft max <player> <scale>`   | Set a player's maximum scale       |
| `predator` | `/sizecraft predator [true\|false]` | Toggle or set predator role        |
| `prey`     | `/sizecraft prey [true\|false]`     | Toggle or set prey role            |

### `/hammerspace`

| Subcommand              | Usage                                          | Description                                          |
| ----------------------- | ---------------------------------------------- | ---------------------------------------------------- |
| `enter`                 | `/hammerspace enter [player]`                  | Enter your own or visit another player's hammerspace |
| `leave`                 | `/hammerspace leave [player]`                  | Leave hammerspace                                    |
| `create`                | `/hammerspace create <slotId>`                 | Create a new compartment                             |
| `delete`                | `/hammerspace delete <slotId>`                 | Delete an empty compartment                          |
| `rename`                | `/hammerspace rename <slotId> <name>`          | Rename a compartment                                 |
| `stomach`               | `/hammerspace stomach <slotId>`                | Set which compartment captured players go to         |
| `view`                  | `/hammerspace view [slotId]`                   | View compartment contents                            |
| `release`               | `/hammerspace release [player]`                | Release all or a specific captured player            |
| `escape`                | `/hammerspace escape`                          | Escape from the hammerspace you're trapped in        |
| `allowEscape`           | `/hammerspace allowEscape <true\|false>`       | Toggle whether players can escape your hammerspace   |
| `preventEscapeForTicks` | `/hammerspace preventEscapeForTicks <0-72000>` | Set escape delay in ticks                            |

## Configuration

SizeCraft uses NeoForge's config system with Cloth Config for an in-game GUI.

### Server Config

| Option                        | Default | Description                                  |
| ----------------------------- | ------- | -------------------------------------------- |
| `defaultScale`                | 1.0     | Default scale for new players                |
| `globalMinScale`              | 0.1     | Global minimum scale                         |
| `globalMaxScale`              | 5.0     | Global maximum scale                         |
| `allowSelfResize`             | true    | Whether non-op players can resize themselves |
| `captureEnabled`              | true    | Master toggle for capture mechanic           |
| `captureMinSizeRatio`         | 1.5     | Minimum carrier/target size ratio to capture |
| `captureRequireShift`         | true    | Require sneaking to capture                  |
| `defaultHammerspaceEscapable` | true    | Default escapability of hammerspaces         |
| `defaultEscapeDelayTicks`     | 600     | Default escape delay (600 = 30s)             |
| `maxEscapeDelayTicks`         | 6000    | Max escape delay (6000 = 5min)               |
| `forceHammerspaceEscapable`   | false   | Force all hammerspaces to be escapable       |

### Client Config

| Option                   | Default | Description                             |
| ------------------------ | ------- | --------------------------------------- |
| `animationDurationTicks` | 20      | Resize animation duration (0 = instant) |
| `enableParticles`        | true    | Show sparkle particles on resize        |

## Building from Source

```sh
git clone https://github.com/dotBeeps/sizecraft.git
cd sizecraft
./gradlew build
```

The built JAR will be in `build/libs/`.

## License

All Rights Reserved.
