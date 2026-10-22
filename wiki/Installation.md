# Installation

## Fabric / Quilt
1. **Install** [Fabric Loader](https://fabricmc.net/use/) 0.19.3+ (or [Quilt Loader](https://quiltmc.org/en/install/)) for Minecraft 26.3
2. **Download** [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3 and the latest `InventoryWizard-fabric-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases)
3. **Place** both JARs in your `.minecraft/mods` folder
4. **Configure** the mod in-game with [Mod Settings](https://modrinth.com/mod/mod-settings) or [Mod Menu](https://modrinth.com/mod/modmenu), or by editing `config/inventorywizard.json` (optional)

## NeoForge
1. **Install** [NeoForge](https://neoforged.net/) 26.3.0.95+ for Minecraft 26.3
2. **Download** the latest `InventoryWizard-neoforge-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases) and place it in your `.minecraft/mods` folder
3. **Configure** the mod in-game from **Mods → InventoryWizard → Config**, or by editing `config/inventorywizard.json` (optional)

## Forge
1. **Install** [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html) 66.0.13+ for Minecraft 26.3
2. **Download** the latest `InventoryWizard-forge-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases) and place it in your `.minecraft/mods` folder
3. **Configure** the mod in-game from **Mods → InventoryWizard → Config**, or by editing `config/inventorywizard.json` (optional)

## Requirements
- **Minecraft**: 26.3
- **Fabric Loader**: 0.19.3 or newer, or **Quilt Loader** 0.30.1 or newer, with **Fabric API** 0.163.0+26.3 or newer (on Quilt too, since Quilted Fabric API is retired)
- **NeoForge**: 26.3.0.95 or newer
- **Forge**: 66.0.13 or newer
- **Java**: 25 or higher

## Singleplayer only
InventoryWizard is a client-side mod that sorts on your world's built-in server, so it works in **singleplayer worlds**, including when you're the host of a world **opened to LAN**. It does nothing on multiplayer servers or when joining someone else's LAN world - vanilla clicks behave normally there.

Running a server? Use the [Paper Plugin](Paper-Plugin) instead.
