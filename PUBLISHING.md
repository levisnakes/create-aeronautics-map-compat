# Publishing to Modrinth

Everything below is ready. Modrinth uploads need your account login, so do the
actual submit yourself at <https://modrinth.com/> → **Create a project**.

## Files to upload
- **Mod file:** `build/libs/aeroradar-1.0.0.jar`
- **Project icon:** `icon.png` (1024×1024, in the project root)

## Project settings
| Field | Value |
|---|---|
| **Name** | Create Aeronautics Map Compat |
| **Slug / URL** | `create-aeronautics-map-compat` (already set as `displayURL` in the jar) |
| **Summary** | Puts Create: Aeronautics ships on Xaero's Minimap & World Map with live markers, and remembers where you parked. Optional shared server-side radar. |
| **Description** | Paste the contents of `MODRINTH.md` |
| **Categories** | Utility, Transportation, Management |
| **Environments** | Client: **Required** · Server: **Optional** |
| **License** | MIT |
| **Project type** | Mod |

Source/issue links are optional (no public repo yet) — leave blank or add later.

## First version
| Field | Value |
|---|---|
| **Version number** | 1.0.0 |
| **Channel** | Release |
| **Loaders** | NeoForge |
| **Game versions** | 1.21.1 |
| **Changelog** | Paste the `1.0.0` section from `CHANGELOG.md` |
| **File** | `aeroradar-1.0.0.jar` |

### Version dependencies (add on the version page)
- **Create: Aeronautics** (`create-aeronautics`) — **Required**
- **Xaero's Minimap** (`xaeros-minimap`) — **Optional**

> Sable ships inside Create: Aeronautics, so it doesn't need a separate dependency.

## Notes
- The mod's network channels are registered as **optional**, so a client with this
  mod can join servers that don't have it, and a server with it won't reject vanilla
  clients — no `displayTest` juggling needed.
- To cut download size later, you can point `logoFile` at a 256×256 copy of the
  icon; 1024×1024 is fine for release.
- Prefer CLI publishing? The community `modrinth` CLI or the Labrinth `POST
  /v2/version` API both work with a personal access token from your Modrinth
  account settings; the metadata above maps 1:1 to their fields.
