# Create Aeronautics Map Compat

**Put your Create: Aeronautics ships on the map — Xaero's *and* JourneyMap — and
never lose your airship again.**

No mod puts your ship (or other players' ships) on Xaero's Minimap / World Map or
JourneyMap, and nothing remembers where you parked. This fixes that. Ships are drawn
**directly on the map** as native, live-moving icons — the same way the map mods
render their own radar — so *"where's my airship?"* finally has an answer.

Client-side is all you need. Add it to the server (or play single-player) for a
shared, whole-server ship radar.

---

## Features

### 🛰️ Native map icons — not waypoints
Every ship appears as a colored diamond icon, moving in real time, on whichever map
mod you use:

- **Xaero's Minimap** + fullscreen **World Map** (via the map element API — the same
  path as Xaero's entity radar)
- **JourneyMap** — minimap, fullscreen map and webmap (via JourneyMap's official
  plugin API, as marker overlays)

It's a pure map visual:

- ❌ no entries in your waypoint list
- ❌ no beacon beams or floating labels in the world
- ❌ nothing ever written to your map mod's files
- ✅ icons exist **only in map views**

### ⚓ Never lose a ship
When a ship unloads or you step off it, a **gold icon with a hollow center** stays
at its last-known position. Fly 10,000 blocks away, log out, come back next week —
the icon's still there.

### 🏷️ Real ship names
Icons use the ship's actual name (world-map label). Unnamed ships get a stable
`Ship-xxxx` id until you name one with `/aeroradar name <name>`. Pin your ride with
`/aeroradar pin` to paint it green.

### 🧩 Smart clustering
Multi-part vehicles (bearing craft, trailers, connected sub-levels) merge into a
**single** icon instead of spamming the map.

### 🌐 Optional server side — shared radar
Install it on the server (**single-player counts**, via the integrated server) and:

- The server tracks **every** ship in each dimension and syncs it to all players —
  so you see **far-away ships and other players' ships** the client could never
  detect on its own.
- Positions and names are **persisted server-side** and **shared** — name a ship and
  everyone sees it.

If the server doesn't have the mod, the client silently falls back to scanning the
ships around you. The network channels are optional, so modded and vanilla clients
mix freely — nobody gets kicked. `/aeroradar list` shows `Server radar: ON/OFF`.

---

## Commands

| Command | Action |
|---|---|
| `/aeroradar list` | All tracked ships + server-radar status |
| `/aeroradar name <name>` | Name the ship you're on (shared if the server supports it) |
| `/aeroradar pin [name]` | Paint a ship green on the map |
| `/aeroradar unpin` | Clear the mark |
| `/aeroradar park` | Save the current ship's position now |
| `/aeroradar forget [name]` | Drop a saved position |

Icon colours: **aqua** = loaded ship · **gold, hollow center** = last-known/parked ·
**green** = pinned or the ship you're standing on.

---

## Requirements

- **NeoForge 1.21.1** (21.1.0+)
- **[Create: Aeronautics](https://modrinth.com/mod/create-aeronautics)** + its Sable
  engine — this is where ships come from (required).
- A map mod to see the icons — any of these, all *optional*:
  - **[Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap)** (+ **Xaero's
    World Map** on top of it)
  - **[JourneyMap](https://modrinth.com/mod/journeymap)**

## Environment

- **Client:** required.
- **Server:** optional — adds the shared, persistent whole-server radar. The mod is
  perfectly happy client-only.

---

*Not affiliated with Create: Aeronautics, Xaero or JourneyMap. Ship data comes from
Aeronautics' Sable engine; icons are drawn through Xaero's map element API and
JourneyMap's plugin API.*
