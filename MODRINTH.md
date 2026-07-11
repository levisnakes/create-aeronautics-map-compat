# Create Aeronautics Map Compat

Puts your Create: Aeronautics ships on the map — Xaero's, JourneyMap, or both —
so "where's my airship" stops being a question.

Nothing else shows ships (yours or anyone else's) on a minimap or world map, and
nothing remembers where you parked. This mod does both. Ships show up as live,
moving icons drawn straight onto the map, the same way Xaero's radar or a
JourneyMap marker overlay works.

Client-side is all you need. Add it to the server too (or just play single-player)
and everyone gets a shared radar of every ship in the world.

---

## What it does

**Icons, not waypoints.** Every ship is a small colored diamond that moves in real
time on whichever map mod you run:

- Xaero's Minimap and World Map, through the map element API — the same code path
  Xaero uses for its own entity radar
- JourneyMap, through its plugin API, as marker overlays

Nothing gets added to your waypoint list. Nothing renders in the world. Nothing is
written to either map mod's save files. The icons only exist while you're looking
at a map.

**Never lose a ship.** Step off one, or let it unload, and a gold icon with a
hollow center marks the last place it was. Fly off, log out, come back a week
later — it's still there.

**Ships get real names.** The world map label uses the ship's actual name. If it
doesn't have one yet, you get a stable `Ship-xxxx` id until you set one with
`/aeroradar name <name>`. Pin a ship (`/aeroradar pin`) and it turns green.

**Multi-part vehicles merge into one icon.** Bearing craft, trailers, anything
built from several connected sub-levels — they collapse into a single marker
instead of covering your map in dots.

**Server side is optional, and it's a real upgrade if you use it.** Install it on
the server — single-player counts, since that runs its own integrated server —
and the server starts tracking every ship in every dimension, not just the ones
near you. That means far-away ships and other players' ships show up too, and
ship positions/names get saved server-side and shared, so a name you set is a
name everyone sees. Without a server install, the client just scans nearby ships
on its own — nothing breaks either way. The network side is registered optional,
so a modded client can join a vanilla server and vice versa without anyone getting
kicked. `/aeroradar list` tells you which mode you're in.

---

## Commands

| Command | Action |
|---|---|
| `/aeroradar list` | All tracked ships, plus whether server radar is on |
| `/aeroradar name <name>` | Name the ship you're on (shared if the server has it too) |
| `/aeroradar pin [name]` | Turn a ship's icon green |
| `/aeroradar unpin` | Undo that |
| `/aeroradar park` | Save the current ship's position right now |
| `/aeroradar forget [name]` | Drop a saved position |

Icon colors: aqua is a loaded ship, gold with a hollow center is last-known/parked,
green is pinned or the one you're standing on.

---

## What you need

- NeoForge 1.21.1 (21.1.0+)
- [Create: Aeronautics](https://modrinth.com/mod/create-aeronautics) — required,
  since ships come from its Sable engine
- A map mod, optional but you'll want one to see anything:
  - [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap), plus Xaero's
    World Map if you use it
  - [JourneyMap](https://modrinth.com/mod/journeymap)

Client: required. Server: optional, adds the shared radar.

---

Not affiliated with Create: Aeronautics, Xaero, or JourneyMap. Ship data comes
from Aeronautics' Sable engine; icons are drawn through Xaero's map element API
and JourneyMap's plugin API.
