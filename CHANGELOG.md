# Changelog

## 1.0.0

Initial release.

- Ships drawn as native, live-moving map icons on Xaero's Minimap, Xaero's
  fullscreen World Map and JourneyMap (minimap, fullscreen and webmap). Map views
  only: no waypoint-list entries, no in-world beacons, nothing written to the map
  mods' files.
- Last-known / parked memory: when a ship unloads or you step off it, a gold
  hollow-center icon stays at its last position (persists across relogs).
- Connected sub-levels (bearing craft, trailers) merge into one icon with a stable
  id, labelled with the ship's real name (or `Ship-xxxx` until you name it). Names
  render at full size on a dark backing plate, on both Xaero's world map and
  JourneyMap, so they stay readable over any terrain.
- Optional server side: install on a server (single-player counts) for a shared,
  persistent, whole-server radar that shows far-away and other players' ships, with
  server-synced names. Falls back to a local scan when the server doesn't have it,
  and is never required on either side.
- Commands: `/aeroradar list | name | pin | unpin | park | forget`.
- Configurable via `config/aeroradar-client.toml` or the in-game config screen.
