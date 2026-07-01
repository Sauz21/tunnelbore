# Tunnel Bore

A Fabric mod that turns tedious digging into a fast, controlled **bore**. Mark a flat area, then mine a single block. The whole layer goes at once and the selection advances forward, so you can carve clean tunnels and big excavations in seconds.

Built by [Auz Tech Labs](https://auztechlabs.com).

## What it does

- **Mark a cross-section** of blocks (a floor patch, a wall, up to a chunk). Hold left-click and sweep to paint large areas fast.
- **Mine one marked block** and the entire layer bores at once. Drops go straight to your inventory, and your pickaxe takes the correct durability.
- **The selection advances** into the face you broke, so holding left-click digs straight down or tunnels forward layer by layer at normal mining speed.
- **Full vanilla rules**: correct pickaxe tier, Fortune/Silk Touch, Unbreaking, and it stops safely at bedrock or before your pickaxe would break.

## Controls

| Key | Action |
|-----|--------|
| **V** | Toggle Mark Mode |
| **Left-click** (Mark Mode on) | Mark blocks; hold and sweep to paint an area |
| **Right-click** (Mark Mode on) | Unmark a block |
| **Left-click** (Mark Mode off) | Mine a marked block → bore the whole layer and advance |
| **Delete** | Clear the selection |

All keys are rebindable under **Options → Controls → Tunnel Bore**.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder.
3. Download the Tunnel Bore jar for your version from [Releases](https://github.com/Sauz21/tunnelbore/releases) and drop it in `mods` too.

Works in single-player and on LAN worlds you host.

### Supported versions

- **Minecraft 1.21.4**: [download](https://github.com/Sauz21/tunnelbore/releases/tag/v0.7.0-1.21.4)
- **Minecraft 1.21**: [download](https://github.com/Sauz21/tunnelbore/releases/tag/v0.7.0)

## Building from source

Requires **JDK 21**.

```sh
./gradlew build
```

The mod jar lands in `build/libs/` (use the one without `-sources`).

## License

Released under [CC0-1.0](LICENSE).
