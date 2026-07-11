# Changelog

## 1.0.0

Initial release.

- Ships drawn as native, live-moving map icons on Xaero's Minimap and fullscreen
  World Map — the same rendering path as Xaero's own radar. Map views only: no
  waypoint-list entries, no in-world beacons, nothing written to waypoint files.
- Last-known / parked memory: when a ship unloads or you step off it, a gold
  hollow-center icon stays at its last position (persists across relogs).
- Connected sub-levels (bearing craft, trailers) merge into one icon with a stable
  id, labelled with the ship's real name (or `Ship-xxxx` until you name it).
- Optional server side: install on a server (single-player counts) for a shared,
  persistent, whole-server radar — see far-away and other players' ships, with
  server-synced names. Falls back to a local scan when the server doesn't have it;
  never required on either side.
- Commands: `/aeroradar list | name | pin | unpin | park | forget`.
- Configurable via `config/aeroradar-client.toml` or the in-game config screen.
