# 🧙‍♂️ InventoryWizard

**One-click inventory sorting for Minecraft.** ✨

A client-side mod for Fabric, Quilt, NeoForge and Forge that sorts your inventory, hotbar and containers in singleplayer, plus a Paper/Spigot plugin that does the same for every player on a server.

![Downloads](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/VanishingTacos/InventoryWizard/badges/downloads.json)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.21--26.3-green.svg)
![Fabric](https://img.shields.io/badge/loader-Fabric-orange.svg)
![Quilt](https://img.shields.io/badge/loader-Quilt-purple.svg)
![NeoForge](https://img.shields.io/badge/loader-NeoForge-red.svg)
![Forge](https://img.shields.io/badge/loader-Forge-lightgrey.svg)
![Paper](https://img.shields.io/badge/server-Paper-blue.svg)
![Spigot](https://img.shields.io/badge/server-Spigot-orange.svg)
[![Discord](https://img.shields.io/badge/discord-join-5865F2.svg?logo=discord&logoColor=white)](https://discord.gg/SeXCPAbu)

## ☕ Support
Need help, found a bug or have an idea? Join the Discord! 

<a href="https://discord.gg/6DS7N3Vmgk" target="_blank">
  <img src="https://pngimg.com/d/discord_PNG19.png" alt="Support Discord" width="150" >
</a>

## Features
- **Smart sorting** by category (blocks, tools, weapons, food, ...), including modded items
- **Survival-friendly hotbar** with weapons and tools first
- **Stacks merged**, but enchanted or named items are never combined
- **Three modes**: Default, Alphabetical and Stack-based
- **Sort buttons** in the inventory, chest and shulker box screens, plus one to switch modes
- **Locked slots** that sorting never moves: **Alt+Click** a hotbar or inventory slot to lock it (`/iwiz lock [slot]` on Paper)
- **Scroll to adjust a held stack**: after picking up a stack, scroll up to take one more item from its slot or down to put one back (mod only, works on any server)
- **Works on** the inventory, hotbar, chests, barrels, shulker boxes and ender chests

## Quick Start
1. Download the jar for your loader and Minecraft version (`InventoryWizard-<loader>-<version>+<minecraft>.jar`) from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases) (Fabric/Quilt also needs [Fabric API](https://modrinth.com/mod/fabric-api))
2. Drop it into `.minecraft/mods` (or `plugins` for Paper/Spigot)
3. Press **`R`** or **Shift+Right Click** in any inventory to sort, or click the **sort button** next to the inventory or chest title

Supports Minecraft 1.21 through 26.3 (Java 21+, or Java 25+ on 26.x). The client mod only sorts in singleplayer and LAN worlds you host; use the Paper plugin for servers.

## 📖 Documentation
Everything else lives in the **[Wiki](https://github.com/VanishingTacos/InventoryWizard/wiki)**:

- [Installation](https://github.com/VanishingTacos/InventoryWizard/wiki/Installation)
- [Usage & Commands](https://github.com/VanishingTacos/InventoryWizard/wiki/Usage)
- [Sorting Modes](https://github.com/VanishingTacos/InventoryWizard/wiki/Sorting-Modes)
- [Configuration](https://github.com/VanishingTacos/InventoryWizard/wiki/Configuration)
- [Paper Plugin](https://github.com/VanishingTacos/InventoryWizard/wiki/Paper-Plugin)
- [Building from Source & Contributing](https://github.com/VanishingTacos/InventoryWizard/wiki/Building-from-Source)

## 📄 License
MIT - see [LICENSE](LICENSE).

If you enjoy InventoryWizard, you can buy me a coffee!

<a href="https://buymeacoffee.com/vanishingtacos" target="_blank">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" width="150" >
</a>

---

<sub>Thanks to the FabricMC, NeoForged, MinecraftForge and PaperMC teams. 🤖 **AI disclosure:** Parts of this project, including code, tests and documentation, were written with the help of AI tools. All AI-assisted changes are reviewed by the maintainer.</sub>
