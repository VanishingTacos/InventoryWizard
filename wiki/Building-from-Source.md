# Building from Source

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

## Project Layout
- `common/` - the sorting logic, config, settings screen and mixins, written against vanilla Minecraft only
  - `common/src/gametest/` - the client gametests for NeoForge and Forge, which have no client gametest API of their own; they are only loaded by `runClientGameTest`, never shipped in a jar
- `fabric/` - Fabric/Quilt entrypoint, Mod Menu integration and client gametests
- `neoforge/` - NeoForge entrypoint
- `forge/` - Forge entrypoint
- `paper/` - the PaperMC server plugin, a standalone project on the Paper API (it doesn't use `common/`)
  - `paper/src/gametest/` - a test plugin that `runPaperTest` loads next to InventoryWizard; never shipped

Anything that needs the mod loader goes through `com.inventorywizard.platform.Platform`, which each loader implements.

## Development Requirements
- Java 25+
- Gradle 9.6+ (wrapper included)
- Fabric Loom 1.18, Fabric API 0.163.0+26.3
- ModDevGradle 2.0, NeoForge 26.3.0.95
- ForgeGradle 7, Forge 26.3-66.0.13
- Paper API 26.3, Shadow 9, run-paper 3

## Contributing
Contributions are welcome! Please feel free to submit pull requests, report bugs, or suggest features.

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature-name`
3. **Commit** your changes: `git commit -am 'Add some feature'`
4. **Push** to the branch: `git push origin feature-name`
5. **Submit** a pull request
