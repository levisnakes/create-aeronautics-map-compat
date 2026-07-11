# Create Aeronautics Map Compat

**Put your Create: Aeronautics ships on Xaero's map — and never lose your airship again.**

No mod puts your ship (or other players' ships) on Xaero's Minimap / World Map, and
nothing remembers where you parked. This fixes that. Ships are drawn **directly on
the map** as native, live-moving icons — the same way Xaero renders its own radar —
so *"where's my airship?"* finally has an answer.

Client-side is all you need. Add it to the server (or play single-player) for a
shared, whole-server ship radar.

---

## Features

### 🛰️ Native map icons — not waypoints
Every ship appears as a colored diamond icon on Xaero's **Minimap** and the
fullscreen **World Map**, moving in real time, with its name labelled on the world
map. It's a pure map visual, exactly like Xaero's entity radar:

- ❌ no entries in your waypoint list
- ❌ no beacon beams or floating labels in the world
- ❌ nothing ever written to your Xaero waypoint files
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
- **[Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap)** — *optional*, needed
  to see the icons. **Xaero's World Map** is supported on top of it.

## Environment

- **Client:** required.
- **Server:** optional — adds the shared, persistent whole-server radar. The mod is
  perfectly happy client-only.

---

*Not affiliated with Create: Aeronautics or Xaero. Ship data comes from Aeronautics'
Sable engine; icons are drawn through Xaero's map element API.*
