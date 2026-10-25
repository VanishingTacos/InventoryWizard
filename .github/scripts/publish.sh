#!/usr/bin/env bash
# Uploads the jars in $DIST to the platforms named as arguments (modrinth, hangar).
#   Modrinth:   every jar, Paper included
#   Hangar:     the Paper jar
# Mod jars are InventoryWizard-<loaders>-<version>+<mc>.jar, <loaders> being the loaders the jar runs on
# joined by _ (e.g. neoforge_forge_fabric). Each jar is one upload tagged with all of its loaders (Fabric
# also as Quilt). The platforms attach dependencies to a whole upload, so one holding Fabric requires
# Fabric API, which NeoForge and Forge users see listed too. The Minecraft versions each jar supports
# come from publish_game_versions in props/<mc>.properties. The Paper jar supports all of them.
#
# Env: VERSION, CHANGELOG_FILE, DIST (default dist), DRY_RUN=1 to print the uploads without sending them,
# LOADERS to upload only some loaders (e.g. "neoforge forge paper"; default all); an upload holding several
# loaders goes ahead when any of them is listed,
# and MODRINTH_TOKEN / HANGAR_API_KEY for the platforms being uploaded to.
# Uploads already on Modrinth are skipped, so a failed run can be re-run.
# A platform that fails doesn't stop the others; the script exits non-zero at the end.
set -euo pipefail

MODRINTH_PROJECT=cU0ynk3p
MODRINTH_FABRIC_API=P7dR8mSH
HANGAR_PROJECT=InventoryWizard
USER_AGENT="VanishingTacos/InventoryWizard (release workflow)"

: "${VERSION:?VERSION is required}"
: "${CHANGELOG_FILE:?CHANGELOG_FILE is required}"
DIST=${DIST:-dist}
DRY_RUN=${DRY_RUN:-}

if [ $# -eq 0 ]; then
	echo "usage: $0 modrinth|hangar..." >&2
	exit 2
fi
for platform in "$@"; do
	case $platform in
		modrinth | hangar) ;;
		*) echo "unknown platform: $platform" >&2; exit 2 ;;
	esac
done

# One process per platform, so set -e still stops a platform at its first failed upload
if [ $# -gt 1 ]; then
	failed=()
	for platform in "$@"; do
		bash "$0" "$platform" || failed+=("$platform")
	done
	if [ ${#failed[@]} -gt 0 ]; then
		echo "::error::Upload failed for: ${failed[*]}"
		exit 1
	fi
	exit 0
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# Prints the response, or on an HTTP error shows the API's message and fails
curl_api() {
	local out=$work/response
	if ! curl -sS --fail-with-body -A "$USER_AGENT" -o "$out" "$@"; then
		echo "::error::$(cat "$out" 2>/dev/null)" >&2
		return 1
	fi
	cat "$out"
}

# props/<mc>.properties publish_game_versions as a JSON array
game_versions_for() {
	grep -E '^publish_game_versions=' "props/$1.properties" | cut -d= -f2 | tr -d '[:space:]' \
		| jq -R 'split(",")'
}

# Every Minecraft version any props file lists, for the Paper jar
all_game_versions() {
	grep -hE '^publish_game_versions=' props/*.properties | cut -d= -f2 | tr -d '\r' | tr ',' '\n' \
		| sort -uV | jq -R . | jq -s .
}

display_loader() {
	case $1 in
		fabric) echo Fabric ;;
		neoforge) echo NeoForge ;;
		forge) echo Forge ;;
		paper) echo Paper ;;
	esac
}

# The loaders of an upload for titles, e.g. NeoForge/Forge
display_loaders() {
	local loader names=()
	for loader in "$@"; do
		names+=("$(display_loader "$loader")")
	done
	(IFS=/; echo "${names[*]}")
}

# The arguments as a JSON array of strings
json_list() {
	printf '%s\n' "$@" | jq -R . | jq -s -c .
}

# Sets jar_loaders (the loaders it runs on, space-separated), mc and game_versions (JSON) for a jar
read_jar() {
	local name
	name=$(basename "$1" .jar)
	name=${name#InventoryWizard-}
	jar_loaders=${name%%-*}
	jar_loaders=${jar_loaders//_/ }
	if [ "$jar_loaders" = paper ]; then
		mc=
		game_versions=$(all_game_versions)
	else
		mc=${name#*+}
		game_versions=$(game_versions_for "$mc")
	fi
}

# One "<jar> <loaders>" line per upload, <loaders> being the jar's loaders joined by _
uploads=()
for jar in $(ls "$DIST"/InventoryWizard-*.jar | sort -V); do
	read_jar "$jar"
	selected=
	for loader in $jar_loaders; do
		if [ -z "${LOADERS:-}" ] || [[ " $LOADERS " == *" $loader "* ]]; then
			selected=1
		fi
	done
	if [ -n "$selected" ]; then
		uploads+=("$jar ${jar_loaders// /_}")
	fi
done
if [ ${#uploads[@]} -eq 0 ]; then
	echo "no jars in $DIST for loaders: ${LOADERS:-all}" >&2
	exit 1
fi

# Sets jar, loaders (array), mc and game_versions for one of the uploads
read_upload() {
	jar=${1% *}
	read_jar "$jar"
	local group=${1##* }
	read -ra loaders <<<"${group//_/ }"
}

# Whether the upload holds this loader
has_loader() {
	[[ " ${loaders[*]} " == *" $1 "* ]]
}

# --- Modrinth ---------------------------------------------------------------------------------------

publish_modrinth() {
	local existing
	existing=$(curl_api "https://api.modrinth.com/v2/project/$MODRINTH_PROJECT/version")

	local upload
	for upload in "${uploads[@]}"; do
		read_upload "$upload"
		local number tags deps title
		if has_loader paper; then
			number=$VERSION
			tags='["paper","spigot"]'
			title="InventoryWizard $VERSION (Paper)"
		else
			number="$VERSION+$mc"
			title="InventoryWizard $VERSION ($(display_loaders "${loaders[@]}") $mc)"
			# Quilt runs Fabric mods
			tags=$(for loader in "${loaders[@]}"; do
				echo "$loader"
				if [ "$loader" = fabric ]; then
					echo quilt
				fi
			done | jq -R . | jq -s -c .)
		fi
		if has_loader fabric; then
			deps="[{\"project_id\":\"$MODRINTH_FABRIC_API\",\"dependency_type\":\"required\"}]"
		else
			deps='[]'
		fi

		# A re-run must not upload the same jar twice. Older releases have one version per loader,
		# so each loader counts as done when any version of this number has it.
		if jq -e --arg n "$number" --argjson ls "$(json_list "${loaders[@]}")" \
			'. as $versions | all($ls[]; . as $l | any($versions[]; .version_number == $n and any(.loaders[]; . == $l)))' \
			<<<"$existing" >/dev/null; then
			echo "Modrinth: $title already exists, skipping"
			continue
		fi

		jq -n \
			--arg project "$MODRINTH_PROJECT" \
			--arg name "$title" \
			--arg number "$number" \
			--rawfile changelog "$CHANGELOG_FILE" \
			--argjson loaders "$tags" \
			--argjson game_versions "$game_versions" \
			--argjson deps "$deps" \
			'{project_id: $project, name: $name, version_number: $number, changelog: $changelog,
			  dependencies: $deps, game_versions: $game_versions, loaders: $loaders,
			  version_type: "release", featured: false, status: "listed",
			  file_parts: ["file"], primary_file: "file"}' >"$work/modrinth.json"

		echo "Modrinth: $title -> $(jq -c '{loaders, game_versions}' "$work/modrinth.json")"
		if [ -z "$DRY_RUN" ]; then
			curl_api -X POST "https://api.modrinth.com/v2/version" \
				-H "Authorization: $MODRINTH_TOKEN" \
				-F "data=<$work/modrinth.json;type=application/json" \
				-F "file=@$jar;type=application/java-archive" >/dev/null
		fi
	done
}

# --- Hangar -----------------------------------------------------------------------------------------

publish_hangar() {
	local api=https://hangar.papermc.io/api/v1
	local jar= upload
	for upload in "${uploads[@]}"; do
		read_upload "$upload"
		if has_loader paper; then
			break
		fi
		jar=
	done
	if [ -z "$jar" ]; then
		echo "Hangar: no Paper jar in $DIST" >&2
		return 1
	fi

	jq -n \
		--arg version "$VERSION" \
		--rawfile description "$CHANGELOG_FILE" \
		--argjson game_versions "$game_versions" \
		'{version: $version, channel: "Release", description: $description,
		  files: [{platforms: ["PAPER"]}],
		  platformDependencies: {PAPER: $game_versions}, pluginDependencies: {}}' >"$work/hangar.json"

	echo "Hangar: $VERSION -> $(jq -c .platformDependencies "$work/hangar.json")"
	if [ -n "$DRY_RUN" ]; then
		return
	fi

	local token
	token=$(curl_api -X POST "$api/authenticate?apiKey=$HANGAR_API_KEY" | jq -r .token)
	curl_api -X POST "$api/projects/$HANGAR_PROJECT/upload" \
		-H "Authorization: HangarAuth $token" \
		-F "versionUpload=<$work/hangar.json;type=application/json" \
		-F "files=@$jar;type=application/java-archive" >/dev/null
}

for platform in "$@"; do
	"publish_$platform"
done
