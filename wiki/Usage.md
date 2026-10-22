# Usage

## Controls
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
| **Cycle Sorting Mode key** (unbound by default) | Anywhere | Cycle sorting modes |

Both keys can be changed under **Options → Controls → Key Binds → InventoryWizard**. The sort key is ignored while you're typing in a text box (recipe book search, anvil name, ...). The creative inventory isn't sorted.

## Cycling Sorting Modes
Cycle between **Default**, **Alphabetical** and **Stack-based** mode with Shift+Right Click on hotbar slot 4, the Cycle Sorting Mode key, or `/iwiz mode`. Your chosen mode is saved in the config and remembered for future sorting. See [Sorting Modes](Sorting-Modes) for what each one does.

## Commands
| Command | Description |
|---------|-------------|
| `/iwiz` | Sort inventory (alias `/inventorywizard`) |
| `/iwiz hotbar` | Sort hotbar only |
| `/iwiz inventory` | Sort inventory only |
| `/iwiz all` | Sort inventory + hotbar |
| `/iwiz mode` | Cycle the sorting mode |
