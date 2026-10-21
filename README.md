# 🧙‍♂️ InventoryWizard

**The magical sorting assistant for your Minecraft world!** ✨

InventoryWizard is a client-side mod for Fabric, Quilt, NeoForge and Forge that brings intelligent, one-click inventory organization to your singleplayer worlds. With intuitive controls and smart categorization, managing your items has never been easier!

> Running a server? There's also a **PaperMC plugin** that sorts for every player, no client mod needed - see [PaperMC Server Plugin](#-papermc-server-plugin).

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-26.3-green.svg)
![Fabric](https://img.shields.io/badge/loader-Fabric-orange.svg)
![Quilt](https://img.shields.io/badge/loader-Quilt-purple.svg)
![NeoForge](https://img.shields.io/badge/loader-NeoForge-red.svg)
![Forge](https://img.shields.io/badge/loader-Forge-lightgrey.svg)
![Paper](https://img.shields.io/badge/server-Paper-blue.svg)

## 🌟 Features

### ✨ **Intelligent Sorting**
- **Smart categorization** - Items are grouped logically (weapons, tools, food, blocks, etc.) using item tags and components, so modded items are sorted too
- **Hotbar optimization** - Weapons and tools prioritized for survival gameplay
- **Stack consolidation** - Automatically combines partial stacks
- **Configurable partial stack sorting** - Choose whether leftover partial stacks are sorted in or placed at the end (see config)
- **Component preservation** - Enchanted, named or otherwise different items are never merged together

### 🖱️ **Multiple Control Methods**
- **Sort key** (default `R`) - Sort the slot you're hovering, the open container, or your whole inventory
- **Shift+Right Click** - Sort inventories, containers, and hotbar
- **Commands** - `/iwiz` client command
- **Cycle Sorting Mode key** (unbound by default) or **Shift+Right Click in Hotbar Slot 4** - Cycle through sorting modes

### 🎒 **Comprehensive Coverage**
- **Player Inventory** - Organize your main storage
- **Hotbar** - Optimized for quick access to combat/survival items
- **Containers** - Chests (incl. double, trapped, chest minecarts and chest boats), shulker boxes, barrels and ender chests
- **All-in-one** - Sort everything simultaneously

## 📥 Installation

### Fabric / Quilt
1. **Install** [Fabric Loader](https://fabricmc.net/use/) 0.19.3+ (or [Quilt Loader](https://quiltmc.org/en/install/)) for Minecraft 26.3
2. **Download** [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3 and the latest `InventoryWizard-fabric-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases)
3. **Place** both JARs in your `.minecraft/mods` folder
4. **Configure** the mod in-game with [Mod Settings](https://modrinth.com/mod/mod-settings) or [Mod Menu](https://modrinth.com/mod/modmenu), or by editing `config/inventorywizard.json` (optional)

### NeoForge
1. **Install** [NeoForge](https://neoforged.net/) 26.3.0.95+ for Minecraft 26.3
2. **Download** the latest `InventoryWizard-neoforge-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases) and place it in your `.minecraft/mods` folder
3. **Configure** the mod in-game from **Mods → InventoryWizard → Config**, or by editing `config/inventorywizard.json` (optional)

### Forge
1. **Install** [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html) 66.0.13+ for Minecraft 26.3
2. **Download** the latest `InventoryWizard-forge-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases) and place it in your `.minecraft/mods` folder
3. **Configure** the mod in-game from **Mods → InventoryWizard → Config**, or by editing `config/inventorywizard.json` (optional)

### Requirements
- **Minecraft**: 26.3
- **Fabric Loader**: 0.19.3 or newer, or **Quilt Loader** 0.30.1 or newer, with **Fabric API** 0.163.0+26.3 or newer (on Quilt too, since Quilted Fabric API is retired)
- **NeoForge**: 26.3.0.95 or newer
- **Forge**: 66.0.13 or newer
- **Java**: 25 or higher

### Singleplayer only
InventoryWizard is a client-side mod that sorts on your world's built-in server, so it works in **singleplayer worlds**, including when you're the host of a world **opened to LAN**. It does nothing on multiplayer servers or when joining someone else's LAN world - vanilla clicks behave normally there.

## 🎮 Usage Guide

### **Control Scheme Overview**
| Action | Where | Result |
|--------|-------|--------|
| **Sort key** (`R`) | Hovering a hotbar slot | Sort inventory + hotbar |
| **Sort key** (`R`) | Hovering the main inventory | Sort main inventory |
| **Sort key** (`R`) | Hovering a container slot, or nothing with a container open | Sort the container |
| **Sort key** (`R`) | No screen open | Sort inventory + hotbar |
| **Shift+Right Click** | Hotbar (slots 0-8) | Sort inventory + hotbar |
| **Shift+Right Click** | Hotbar Slot 4 | Cycle sorting modes |
| **Shift+Right Click** | Main Inventory (slots 9-35) | Sort main inventory |
| **Shift+Right Click** | Container slot | Sort container contents |
| **Cycle Sorting Mode key** | Anywhere | Cycle sorting modes |

Both keys can be changed under **Options → Controls → Key Binds → InventoryWizard**. The sort key is ignored while you're typing in a text box (recipe book search, anvil name, ...). The creative inventory isn't sorted.

### **Cycling Sorting Modes**
- **Default Mode**: Smart categorization by item type
- **Alphabetical Mode**: Sort items alphabetically by ID
- **Stack-based Mode**: Group by item type, then sort by stack size (largest first)
- Your chosen mode is saved in the config and remembered for future sorting!

## 🧙‍♂️ Commands

| Command | Description |
|---------|-------------|
| `/iwiz` | Sort inventory (alias `/inventorywizard`) |
| `/iwiz hotbar` | Sort hotbar only |
| `/iwiz inventory` | Sort inventory only |
| `/iwiz all` | Sort inventory + hotbar |
| `/iwiz mode` | Cycle the sorting mode |

## ⚙️ Configuration

### In-game settings
Install [Mod Settings](https://modrinth.com/mod/mod-settings) to change settings from the **pause menu** (the **Mod Settings...** button, or press `F6`) - pick **InventoryWizard**. [Mod Menu](https://modrinth.com/mod/modmenu) works too. The screen covers the sorting mode, chat messages, sound on/off and volume, the update check, which containers can be sorted, and partial stack placement. On NeoForge and Forge, open it from **Mods → InventoryWizard → Config**. Changes apply immediately and are saved when you press **Done**.

The sound id/pitch and hotbar priorities can only be changed in the JSON file.

### Config file
The mod generates `config/inventorywizard.json`:

```json
{
  "sortMode": "DEFAULT",
  "playSound": true,
  "sound": {
    "id": "minecraft:ui.button.click",
    "volume": 0.5,
    "pitch": 1.2
  },
  "showMessages": true,
  "features": {
    "chestSorting": true,
    "shulkerSorting": true,
    "barrelSorting": true,
    "enderChestSorting": true,
    "inventorySorting": true,
    "hotbarSorting": true
  },
  "allowPartialStacksChest": true,
  "allowPartialStacksInventory": true,
  "hotbarPriorities": {
    "weapons": 100,
    "tools": 200,
    "food": 300,
    "buildingBlocks": 400,
    "utilityItems": 500,
    "other": 999
  },
  "checkForUpdates": true
}
```

#### Update Check
- With `checkForUpdates` on, the mod asks [Modrinth](https://modrinth.com/mod/inventorywizard) at startup for the newest release for your loader and Minecraft version. If it is newer than yours, you get one chat message with a download link when you join a world.
- Set it to `false` to turn off the request entirely (takes effect after a restart).

#### Partial Stack Sorting Behavior
- `allowPartialStacksChest` and `allowPartialStacksInventory` control where leftover partial stacks go in containers and the player inventory.
- **If set to `true`**: Each partial stack is placed right after the full stacks of the same item.
- **If set to `false`**: Full stacks are sorted first and all leftover partial stacks are placed together at the end. No items are ever removed.

## 📋 Sorting Modes

### **Default Mode** (Smart Categorization)
Items are sorted by logical categories:

1. **Stone Types** - Stone, granite, cobblestone, deepslate, etc.
2. **Earth Types** - Dirt, sand, gravel, clay, concrete, etc.
3. **Wood Types** - Logs, planks, leaves, saplings
4. **Ores & Metals** - Ores, iron blocks, diamond blocks, etc.
5. **Other Blocks** - Remaining building materials
6. **Tools** - Pickaxes, axes, shovels, shears, etc.
7. **Weapons** - Swords, spears, bows, arrows, tridents, maces
8. **Armor** - Helmets, chestplates, boots, shields, elytra
9. **Food** - All edible items
10. **Redstone** - Redstone components and mechanisms
11. **Transportation** - Boats, minecarts, saddles
12. **Decoration** - Flowers, carpets, banners, etc.
13. **Miscellaneous** - Everything else

### **Alphabetical Mode**
Items are sorted alphabetically by their item ID (A-Z).

### **Stack-based Mode**
Items are grouped by type first, then sorted by stack size within each type. Stacks keep their original sizes.

### **Hotbar Sorting Order** (Optimized for Survival)
1. **Weapons** - Combat items for quick access
2. **Tools** - Essential survival tools
3. **Food** - Health restoration
4. **Building Blocks** - Cobblestone, dirt for quick building
5. **Utility Items** - Ender pearls, buckets, torches
6. **Other Items** - Miscellaneous

## 🖥️ PaperMC Server Plugin

The `paper/` subproject is a server-side plugin: players sort with Shift+Right Click and `/iwiz` without installing anything. It's a separate codebase from the mod (Bukkit API instead of vanilla classes), with its own permissions and config.

### Installation
1. **Download** the latest `InventoryWizard-paper-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases)
2. **Place** it in your server's `plugins` folder and restart
3. **Configure** it in `plugins/InventoryWizard/config.yml` (optional)

Requires **Paper 26.3** (or a compatible fork) and **Java 25+**. Player sorting modes are stored in an embedded H2 database (`plugins/InventoryWizard/player_data.mv.db`); no setup needed.

### Controls
| Action | Location | Permission | Result |
|--------|----------|------------|--------|
| **Shift+Right Click** | Hotbar (slots 0-8) | `inventorywizard.all` | Sort inventory + hotbar |
| **Shift+Right Click** | Hotbar (slots 0-8) | `inventorywizard.hotbar` | Sort hotbar only |
| **Shift+Right Click** | Hotbar Slot 4 | `inventorywizard.use` | Cycle sorting modes |
| **Shift+Right Click** | Main Inventory (slots 9-35) | `inventorywizard.inventory` | Sort main inventory |
| **Shift+Right Click** | Chest, barrel, shulker box, ender chest | `inventorywizard.chest` / `.barrel` / `.shulker` / `.enderchest` | Sort the container |
| **Double Click** | Hotbar (slots 0-8) | `inventorywizard.hotbar` | Sort hotbar only |

### Commands
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/iwiz` | `/inventorywizard`, `/sort` | Base command | `inventorywizard.inventory` |
| `/iwiz hotbar` | `/iwiz hb` | Sort hotbar only | `inventorywizard.hotbar` |
| `/iwiz inventory` | `/iwiz inv` | Sort inventory only | `inventorywizard.inventory` |
| `/iwiz all` | `/iwiz both` | Sort everything | `inventorywizard.all` |

### Permissions
Every permission defaults to `true` except `inventorywizard.admin` (op only), which allows regenerating the database credentials.

`inventorywizard.use`, `.chest`, `.shulker`, `.barrel`, `.enderchest`, `.inventory`, `.hotbar`, `.all`, `.admin`

## 🔧 Building from Source

```bash
# Clone the repository
git clone https://github.com/VanishingTacos/inventorywizard.git
cd inventorywizard

# Build with Gradle (needs a Java 25 JDK)
./gradlew build

# The JARs will be in fabric/build/libs/InventoryWizard-fabric-x.x.x.jar,
# neoforge/build/libs/InventoryWizard-neoforge-x.x.x.jar,
# forge/build/libs/InventoryWizard-forge-x.x.x.jar
# and paper/build/libs/InventoryWizard-paper-x.x.x.jar

# Launch a development client
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
./gradlew :forge:runClient

# Run the client gametests: each starts a fresh game, sorts with simulated
# clicks and key presses, checks the results and exits with pass/fail
./gradlew :fabric:runClientGameTest
./gradlew :fabric:runClientGameTest -Ploader=quilt
./gradlew :neoforge:runClientGameTest
./gradlew :forge:runClientGameTest

# Start a Paper 26.3 test server with the plugin installed (join it at localhost)
./gradlew :paper:runServer

# Run the Paper gametests: starts a fresh server, sorts real chests, barrels,
# shulker boxes and ender chests, checks plugin setup, then stops with pass/fail
./gradlew :paper:runPaperTest
```

### Project Layout
- `common/` - the sorting logic, config, settings screen and mixins, written against vanilla Minecraft only
  - `common/src/gametest/` - the client gametests for NeoForge and Forge, which have no client gametest API of their own; they are only loaded by `runClientGameTest`, never shipped in a jar
- `fabric/` - Fabric/Quilt entrypoint, Mod Menu integration and client gametests
- `neoforge/` - NeoForge entrypoint
- `forge/` - Forge entrypoint
- `paper/` - the PaperMC server plugin, a standalone project on the Paper API (it doesn't use `common/`)
  - `paper/src/gametest/` - a test plugin that `runPaperTest` loads next to InventoryWizard; never shipped

Anything that needs the mod loader goes through `com.inventorywizard.platform.Platform`, which each loader implements.

### Development Requirements
- Java 25+
- Gradle 9.6+ (wrapper included)
- Fabric Loom 1.18, Fabric API 0.163.0+26.3
- ModDevGradle 2.0, NeoForge 26.3.0.95
- ForgeGradle 7, Forge 26.3-66.0.13
- Paper API 26.3, Shadow 9, run-paper 3

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests, report bugs, or suggest features.

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature-name`
3. **Commit** your changes: `git commit -am 'Add some feature'`
4. **Push** to the branch: `git push origin feature-name`
5. **Submit** a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **FabricMC Team** for Fabric Loader, Loom and Fabric API
- **NeoForged Team** for NeoForge and ModDevGradle
- **MinecraftForge Team** for Forge and ForgeGradle
- **PaperMC Team** for the server software the original plugin was built on
- **Minecraft Community** for inspiration and feedback
- **Mod and plugin developers** who paved the way

---

## ☕ Support InventoryWizard

If you enjoy using InventoryWizard and want to support its development, you can buy me a coffee!

<a href="https://buymeacoffee.com/vanishingtacos" target="_blank">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" width="150" >
</a>


**Made with** ❤️ **and** ✨ **for the Minecraft community**

*Transform your chaotic inventory into organized perfection with InventoryWizard!* 🧙‍♂️

---

<sub>🤖 **AI disclosure:** Parts of this project, including code, tests and documentation, were written with the help of AI tools. All AI-assisted changes are reviewed by the maintainer.</sub>
