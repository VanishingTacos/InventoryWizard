# Paper Plugin

The `paper/` subproject is a server-side plugin: players sort with Shift+Right Click and `/iwiz` without installing anything. It's a separate codebase from the mod (Bukkit API instead of vanilla classes), with its own permissions and config.

## Installation
1. **Download** the latest `InventoryWizard-paper-x.x.x.jar` from the [releases page](https://github.com/VanishingTacos/inventorywizard/releases)
2. **Place** it in your server's `plugins` folder and restart
3. **Configure** it in `plugins/InventoryWizard/config.yml` (optional)

Requires **Paper 26.3** or **Spigot 26.3** (or a compatible fork) and **Java 25+**; the same jar runs on both. Player sorting modes are stored in an embedded H2 database (`plugins/InventoryWizard/player_data.mv.db`); no setup needed.

## Controls
| Action | Location | Permission | Result |
|--------|----------|------------|--------|
| **Shift+Right Click** | Hotbar (slots 0-8) | `inventorywizard.all` | Sort inventory + hotbar |
| **Shift+Right Click** | Hotbar (slots 0-8) | `inventorywizard.hotbar` | Sort hotbar only |
| **Shift+Right Click** | Hotbar Slot 4 | `inventorywizard.use` | Cycle sorting modes |
| **Shift+Right Click** | Main Inventory (slots 9-35) | `inventorywizard.inventory` | Sort main inventory |
| **Shift+Right Click** | Chest, barrel, shulker box, ender chest | `inventorywizard.chest` / `.barrel` / `.shulker` / `.enderchest` | Sort the container |
| **Double Click** | Empty hotbar slot, nothing on the cursor | `inventorywizard.hotbar` | Sort hotbar only |

## Commands
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/iwiz` | `/inventorywizard`, `/sort` | Sort inventory (same as `/iwiz inventory`) | `inventorywizard.use` to run the command; each subcommand checks its own permission |
| `/iwiz hotbar` | `/iwiz hb` | Sort hotbar only | `inventorywizard.hotbar` |
| `/iwiz inventory` | `/iwiz inv` | Sort inventory only | `inventorywizard.inventory` |
| `/iwiz all` | `/iwiz both` | Sort everything | `inventorywizard.all` |

## Permissions
Every permission defaults to `true` except `inventorywizard.admin` (op only), which allows regenerating the database credentials.

`inventorywizard.use`, `.chest`, `.shulker`, `.barrel`, `.enderchest`, `.inventory`, `.hotbar`, `.all`, `.admin`
