# Configuration

## In-game settings
Install [Mod Settings](https://modrinth.com/mod/mod-settings) to change settings from the **pause menu** (the **Mod Settings...** button, or press `F6`) - pick **InventoryWizard**. [Mod Menu](https://modrinth.com/mod/modmenu) works too. The screen covers the sorting mode, chat messages, sound on/off and volume, the update check, which containers can be sorted, and partial stack placement. On NeoForge and Forge, open it from **Mods → InventoryWizard → Config**. Changes apply immediately and are saved when you press **Done**.

The sound id/pitch and hotbar priorities can only be changed in the JSON file.

## Config file
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

### Update Check
- With `checkForUpdates` on, the mod asks [Modrinth](https://modrinth.com/mod/inventorywizard) at startup for the newest release for your loader and Minecraft version. If it is newer than yours, you get one chat message with a download link when you join a world.
- Set it to `false` to turn off the request entirely (takes effect after a restart).

### Partial Stack Sorting Behavior
- `allowPartialStacksChest` and `allowPartialStacksInventory` control where leftover partial stacks go in containers and the player inventory.
- **If set to `true`**: Each partial stack is placed right after the full stacks of the same item.
- **If set to `false`**: Full stacks are sorted first and all leftover partial stacks are placed together at the end. No items are ever removed.
