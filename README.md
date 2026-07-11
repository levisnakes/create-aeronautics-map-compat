# Create Aeronautics Map Compat

*Ship radar & Xaero's map integration for Create: Aeronautics (mod id `aeroradar`).*

**"Where's my airship?" — solved.**

A NeoForge 1.21.1 mod that draws Create: Aeronautics ships **directly on Xaero's
Minimap and World Map** as native, live-moving map icons, and remembers each ship's
**last-known position**. Client-side is enough; add it to the server for a shared,
persistent, whole-server radar.

## Features

- **Native map icons, not waypoints** — ships render through Xaero's map element
  API (the same path as Xaero's own entity radar). Icons exist only in map views:
  no waypoint-list entries, no in-world beacons, nothing written to Xaero's files.
- **Live tracking** — every loaded ship is an aqua diamond that follows the ship in
  real time, labelled with the ship's real name on the world map (stable
  `Ship-xxxx` id until you name it).
- **Last-known / parked memory** — when a ship unloads or you step off it, a gold
  hollow-center icon stays at its last position, surviving relogs.
- **Smart clustering** — connected sub-levels (bearing craft, trailers) merge into
  a single icon with a stable id.
- **Name & pin ships** — `/aeroradar name Skybreaker`, then `/aeroradar pin
  Skybreaker` to paint it green.

### Optional server side (not required)

Install it on the server — **single-player counts**, via the integrated server —
for a shared radar:

- The server tracks **every** ship in each dimension and syncs them to all players:
  far-away ships and other players' ships included.
- Positions and names are **persisted server-side** (survive restarts) and
  **shared** — name a ship and everyone sees it.
- Without it, the client falls back to scanning nearby ships. The network channels
  are optional, so modded and vanilla clients mix freely.

`/aeroradar list` shows `Server radar: ON/OFF`.

## Commands

| Command | Action |
|---|---|
| `/aeroradar list` | All tracked ships with distance/direction + server-radar status |
| `/aeroradar name <name>` | Name the ship you're on (shared if the server supports it) |
| `/aeroradar pin [name]` | Paint a ship green on the map |
| `/aeroradar unpin` | Clear the mark |
| `/aeroradar park` | Save the current ship's position now |
| `/aeroradar forget [name]` | Drop a saved position |

Icon colours: **aqua** = loaded · **gold, hollow center** = last-known/parked ·
**green** = pinned or the ship you're standing on.

## Config

`config/aeroradar-client.toml` (or the in-game config screen): live/parked icons
on/off, use server ships on/off, hide the ship you're standing on, local auto-park.

## Requirements

- NeoForge 1.21.1 (21.1.0+)
- **Sable** (the Create: Aeronautics physics engine — where ships come from)
- Xaero's Minimap — *optional* (needed to see the icons); Xaero's World Map
  supported on top of it

## Building

`./gradlew build` → `build/libs/aeroradar-<version>.jar`.

The build compiles against four jars in `libs/` which are **not** included in this
repository (they are other mods' property — All Rights Reserved / their own
licenses). Copy them from a modpack that ships Create: Aeronautics + Xaero's maps
(e.g. from your `mods/` folder):

```
libs/sable-neoforge-1.21.1-1.2.2.jar
libs/sable-companion-common-1.21.1-1.6.0.jar
libs/xaerominimap-neoforge-1.21.1-25.3.13.jar
libs/xaeroworldmap-neoforge-1.21.1-1.40.16.jar
```

Nothing from these jars is bundled into the built mod — they are `compileOnly` and
provided by the player's modpack at runtime.

## License

MIT — see [LICENSE](LICENSE).
