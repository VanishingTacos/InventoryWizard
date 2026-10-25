#!/usr/bin/env bash
# Prints what a release would upload to Modrinth and CurseForge, without uploading anything.
# Collects the jars for the version in gradle.properties the way the release workflow does
# (.github/scripts/collect-jars.sh), then runs .github/scripts/publish.sh with DRY_RUN=1.
#
#   .github/scripts/dry-run.sh [--build] [modrinth] [curseforge] [hangar]
#
# Platforms default to modrinth and curseforge. --build runs ./gradlew build first.
# LOADERS limits the uploads as in publish.sh, e.g. LOADERS="fabric" .github/scripts/dry-run.sh modrinth
# The Modrinth and CurseForge dry runs still read which files the projects already have (from Modrinth's
# public API and CFWidget), to show which uploads they would skip; Hangar's sends no requests. Needs jq.
set -euo pipefail

cd "$(dirname "$0")/../.."

build=
platforms=()
for arg in "$@"; do
	case $arg in
		--build) build=1 ;;
		*) platforms+=("$arg") ;;
	esac
done
if [ ${#platforms[@]} -eq 0 ]; then
	platforms=(modrinth curseforge)
fi

if ! command -v jq >/dev/null; then
	echo "jq is required: https://jqlang.org/download/ (Windows: winget install jqlang.jq)" >&2
	exit 1
fi

if [ -n "$build" ]; then
	./gradlew build
fi

VERSION=$(grep -E '^version=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')
export VERSION

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

echo "Collecting the $VERSION jars"
DIST="$work/dist" bash .github/scripts/collect-jars.sh >/dev/null
echo "Dry run of InventoryWizard $VERSION ($(ls "$work/dist" | wc -l | tr -d ' ') jars)"
echo

echo "(dry run: release notes go here)" >"$work/changelog.md"
DRY_RUN=1 DIST="$work/dist" CHANGELOG_FILE="$work/changelog.md" bash .github/scripts/publish.sh "${platforms[@]}"
