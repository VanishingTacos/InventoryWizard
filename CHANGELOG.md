# Changelog

All notable changes to InventoryWizard. The release workflow copies the section for the version in
`gradle.properties` into the GitHub release, and from there into Modrinth and Hangar.

Add changes under **Unreleased** as you make them. When you bump the version, rename that heading to
`## [x.y.z] - YYYY-MM-DD` and start a new empty **Unreleased** section above it.

## Unreleased

### Changed
- Redesigned settings screen: options are grouped into Sorting, Feedback, Sorting Enabled For, Sort Partial Stacks In and Other sections on every Minecraft version, and each sort mode explains itself in its tooltip.
- Options that do nothing with the current settings are greyed out: volume and pitch while the sort sound is off, and the partial stack options in Stack-based mode.

### Added
- **Settings:** a magical new look: a shimmering two-tone title with twinkling sparkles, rotating tips, gold section headings, item icons on the buttons, and green/red ON/OFF values.
- **Settings:** a live **sort preview** under Sorting Mode that sorts sample items with your settings and animates them into place when you change modes. From Minecraft 26.1 it appears once you're in a world, since items aren't available on the title screen.
- **Settings:** a sound pitch slider and a **Preview Sound** button.
- **Settings:** **Reset to Defaults** (with a confirmation; locked slots are kept), **Unlock All Slots**, and a shortcut to the key binds.

## [2.3.1] - 2026-10-27

### Added
- The sort buttons, the sort key and `/iwiz` now work on servers running the InventoryWizard Paper plugin, when you also have the client mod. The buttons show up by themselves when the server has the plugin.
- **Paper:** `/iwiz container` sorts the chest, barrel, shulker box or ender chest you have open.
- **Paper:** `/iwiz mode [default|alphabetical|stack-based]` sets your sort mode, or switches to the next one when no mode is given.

## [2.3.0] - 2026-10-25

### Added
- **Scroll to adjust a held stack:** after picking up a stack, scroll up to take one more item from its slot or down to put one back. Works on any server. Turn it off with the **Scroll Held Stack** setting.

### Changed
- Fewer jars: one jar per Minecraft version now runs on every loader. From 26.1 a single jar covers Fabric, Quilt, NeoForge and Forge; before 26.1, NeoForge and Forge share a jar and Fabric/Quilt has its own.

## [2.2.1] - 2026-10-24

### Added
- **Sort buttons** next to the inventory, chest and shulker box titles, plus a button that switches the sort mode. Hide them with the **Sort Buttons** setting.
- **Locked slots** that sorting never moves: **Alt+Click** a hotbar or inventory slot to lock or unlock it.
- **Paper:** `/iwiz lock [slot|list|clear]` locks slots on servers.

## [2.1.1] - 2026-10-23

No changes to the mod or plugin. Releases are now also uploaded to Modrinth and Hangar.

## [2.1.0] - 2026-10-23

### Added
- Supports Minecraft 1.21 through 26.3 on every loader, not just 26.3.
- The Paper plugin is back, updated to 26.3 and tested on Spigot too.

### Fixed
- Shift+Right-click now works normally on multiplayer servers; the mod no longer swallows the click, so the server (or the Paper plugin) gets it.
- Deepslate and redstone ores sort with the other ores, and redstone, glowstone and lodestone no longer sort as stone.
- Alphabetical order is stable for items with the same name from different mods.
- Clicking to sort a container that can't be sorted now says why nothing happened.
- **Paper:** `/iwiz regen-credentials` no longer locks the plugin out of its own database after a restart.
- **Paper:** if the database can't start, preferences are kept in memory instead of failing on every click.
- **Paper:** only the clicks that sort are cancelled, and other plugins' menus are left alone.
- **Paper:** turning partial stacks off no longer drops items.
- **Paper:** double-clicking the hotbar with an item on the cursor collects matching items like vanilla instead of sorting.
- **Paper:** rate limiting uses a 1 second cooldown and a real per-minute window.
- **Paper:** `/iwiz` only needs `inventorywizard.use`; each subcommand checks its own permission.

## [2.0.2] - 2026-10-20

- 🧣 Add support for Quilt Loader, Forge, NeoForge and update dependencies for compatibility

## [2.0.1] - 2026-10-20

- ⌚ Added an update checker
- ⚙️ Added ability to adjust settings via [Mod Settings](https://modrinth.com/mod/mod-settings)

## [2.0.0] - 2026-10-17

- 🧵 Ported to Fabric for Minecraft 26.3 as a client-side, singleplayer mod
- ⌨️ Sort and Cycle Sorting Mode key bindings replace double-click hotbar sorting
- 🗑️ Removed multiplayer features - permissions, rate limiting, admin commands and the H2 database (the sort mode is now stored in the config)
- 🐛 Partial-stack options now take effect - they were never applied in the plugin, and leftover partial stacks are now kept instead of being dropped

## [1.2.0] - 2026-06-05

- 🔧 Update project dependencies, Java 26 compatibility
- 🔧 Add Option to Configure Rate-Limiting on a Per-Player Basis [nickcisne](https://github.com/nickcisne)
- 🔧 Add Ability to Sort Other Containers [nickcisne](https://github.com/nickcisne)

## [1.1.1] - 2025-07-27

- 🛡️ **Rate Limiting Added** - Prevents excessive sorting to avoid server lag and abuse (configurable per player)
- 🗃️ **H2 Database** - Improved performance and security for inventory data storage
- 🐛 **Bug Fixes** - Fixed rare issues with inventory sync and sorting edge cases
- 📚 **Documentation Improvements** - Expanded README and in-game help for new features

## [1.1.0] - 2025-07-24

- ✨ **New Stack-based Sorting Mode** - Group items by type, then sort by stack size (full stacks first)
  - 🔄 **Preserved Individual Stacks** - Partial stacks maintain their original sizes without combining
- 📦 **Alphabetical Material Grouping** - Items grouped alphabetically by material name within stack-based mode
- 🎯 **Enhanced Sorting Logic** - Improved stack size sorting (largest to smallest within each material type)
- 🧙‍♂️ **Mode Cycling** - Easily switch between Default, Alphabetical, and Stack-based sorting modes

## [1.0.1] - 2025-07-20

- 🔧 **Fixed control scheme** - Removed middle click, improved Shift+Right Click logic
- 🎯 **Enhanced hotbar sorting** - Priority-based permission system for combined sorting
- 🛠️ **Code optimization** - Eliminated duplicate event handlers and improved reliability
- 📝 **Updated documentation** - Clearer instructions and permission requirements

## [1.0.0] - 2025-07-20

- ✨ Initial release
- 🧙‍♂️ Smart inventory sorting with type-based categorization
- 🎯 Hotbar optimization for PvP/survival gameplay
- 📦 Chest organization support
- 🖱️ Multiple control methods (click-based and commands)
- 🔊 Audio feedback and magical messages
- ⚙️ Comprehensive configuration options
