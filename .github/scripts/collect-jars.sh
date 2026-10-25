#!/usr/bin/env bash
# Copies the jars players install for $VERSION from versions/ into $DIST (default dist):
#   versions/<mc>-merged/InventoryWizard-<loaders>-<version>+<mc>.jar, which runs on NeoForge and Forge,
#   and on Fabric too from 26.1; older Minecraft versions also get versions/<mc>-fabric/InventoryWizard-fabric-...
#   versions/<api>-paper/InventoryWizard-paper-<version>.jar (not the unshaded -plain jar)
# No -sources jars. Fails if any of them is missing, so run ./gradlew build first.
set -euo pipefail

: "${VERSION:?VERSION is required}"
DIST=${DIST:-dist}

mkdir -p "$DIST"
cp versions/*-merged/InventoryWizard-*-"$VERSION"+*.jar "$DIST"/
rm -f "$DIST"/*-sources.jar
for jar in "$DIST"/InventoryWizard-neoforge_forge-"$VERSION"+*.jar; do
	mc=${jar##*+}
	mc=${mc%.jar}
	cp "versions/$mc-fabric/InventoryWizard-fabric-$VERSION+$mc.jar" "$DIST"/
done
cp versions/*-paper/InventoryWizard-paper-"$VERSION".jar "$DIST"/
ls -l "$DIST"
