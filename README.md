# 🧙‍♂️ InventoryWizard

**The magical sorting assistant for your Minecraft world!** ✨

InventoryWizard is a Fabric mod that brings intelligent, one-click inventory organization to your singleplayer worlds. With intuitive controls and smart categorization, managing your items has never been easier!

> Looking for the PaperMC server plugin? It lives on the [`main` branch](https://github.com/VanishingTacos/InventoryWizard/tree/main).

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-26.3-green.svg)
![Fabric](https://img.shields.io/badge/loader-Fabric-orange.svg)

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

1. **Install** [Fabric Loader](https://fabricmc.net/use/) 0.19.5+ for Minecraft 26.3
2. **Download** [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3 and the latest `InventoryWizard-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases)
3. **Place** both JARs in your `.minecraft/mods` folder
4. **Configure** the mod using `config/inventorywizard.json` (optional)

### Requirements
- **Minecraft**: 26.3
- **Fabric Loader**: 0.19.5 or newer
- **Fabric API**: 0.163.0+26.3 or newer
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
  }
}
```

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

## 🔧 Building from Source

```bash
# Clone the repository and switch to the Fabric branch
git clone https://github.com/VanishingTacos/inventorywizard.git
cd inventorywizard
git checkout fabric

# Build with Gradle (needs a Java 25 JDK)
./gradlew build

# The JAR will be in build/libs/InventoryWizard-2.0.0.jar

# Launch a development client
./gradlew runClient
```

### Development Requirements
- Java 25+
- Gradle 9.6+ (wrapper included)
- Fabric Loom 1.18, Fabric API 0.163.0+26.3

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests, report bugs, or suggest features.

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature-name`
3. **Commit** your changes: `git commit -am 'Add some feature'`
4. **Push** to the branch: `git push origin feature-name`
5. **Submit** a pull request


## 📜 Changelog

# Version 2.0.0 (Fabric)
- 🧵 **Ported to Fabric** for Minecraft 26.3 as a client-side, singleplayer mod
- ⌨️ **Sort and Cycle Sorting Mode key bindings** replace double-click hotbar sorting
- 🗑️ **Removed multiplayer features** - permissions, rate limiting, admin commands and the H2 database (the sort mode is now stored in the config)
- 🐛 **Partial-stack options now take effect** - they were never applied in the plugin, and leftover partial stacks are now kept instead of being dropped

# Version 1.2.0
- 🔧 Update project dependencies, Java 26 compatibility
- 🔧Add Option to Configure Rate-Limiting on a Per-Player Basis [nickcisne](https://github.com/nickcisne)
- 🔧Add Ability to Sort Other Containers [nickcisne](https://github.com/nickcisne)


# Version 1.1.1
- 🛡️ **Rate Limiting Added** - Prevents excessive sorting to avoid server lag and abuse (configurable per player)
- 🗃️ **H2 Database** - Improved performance and secuirty for inventory data storage
- 🐛 **Bug Fixes** - Fixed rare issues with inventory sync and sorting edge cases
- 📚 **Documentation Improvements** - Expanded README and in-game help for new features


### Version 1.1.0
- ✨ **New Stack-based Sorting Mode** - Group items by type, then sort by stack size (full stacks first)
  - 🔄 **Preserved Individual Stacks** - Partial stacks maintain their original sizes without combining
- 📦 **Alphabetical Material Grouping** - Items grouped alphabetically by material name within stack-based mode
- 🎯 **Enhanced Sorting Logic** - Improved stack size sorting (largest to smallest within each material type)
- 🧙‍♂️ **Mode Cycling** - Easily switch between Default, Alphabetical, and Stack-based sorting modes

### Version 1.0.1
- 🔧 **Fixed control scheme** - Removed middle click, improved Shift+Right Click logic
- 🎯 **Enhanced hotbar sorting** - Priority-based permission system for combined sorting
- 🛠️ **Code optimization** - Eliminated duplicate event handlers and improved reliability
- 📝 **Updated documentation** - Clearer instructions and permission requirements

### Version 1.0.0
- ✨ Initial release
- 🧙‍♂️ Smart inventory sorting with type-based categorization
- 🎯 Hotbar optimization for PvP/survival gameplay
- 📦 Chest organization support
- 🖱️ Multiple control methods (click-based and commands)
- 🔊 Audio feedback and magical messages
- ⚙️ Comprehensive configuration options

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **FabricMC Team** for Fabric Loader, Loom and Fabric API
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
