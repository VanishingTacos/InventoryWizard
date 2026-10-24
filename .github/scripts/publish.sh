#!/usr/bin/env bash
# Uploads the jars in $DIST to the platforms named as arguments (curseforge, modrinth, hangar).
#   CurseForge: the Fabric, NeoForge and Forge jars
#   Modrinth:   every jar, Paper included
#   Hangar:     the Paper jar
# Mod jars are InventoryWizard-<loader>-<version>+<mc>.jar; the Minecraft versions each one supports
# come from publish_game_versions in props/<mc>.properties. The Paper jar supports all of them.
#
# Env: VERSION, CHANGELOG_FILE, DIST (default dist), DRY_RUN=1 to print the uploads without sending them,
# LOADERS to upload only some jars (e.g. "neoforge forge paper"; default all),
# and CURSEFORGE_TOKEN / MODRINTH_TOKEN / HANGAR_API_KEY for the platforms being uploaded to.
# A platform that fails doesn't stop the others; the script exits non-zero at the end.
set -euo pipefail

CURSEFORGE_PROJECT=1752626
MODRINTH_PROJECT=cU0ynk3p
MODRINTH_FABRIC_API=P7dR8mSH
HANGAR_PROJECT=InventoryWizard
USER_AGENT="VanishingTacos/InventoryWizard (release workflow)"

: "${VERSION:?VERSION is required}"
: "${CHANGELOG_FILE:?CHANGELOG_FILE is required}"
DIST=${DIST:-dist}
DRY_RUN=${DRY_RUN:-}

if [ $# -eq 0 ]; then
	echo "usage: $0 curseforge|modrinth|hangar..." >&2
	exit 2
fi
for platform in "$@"; do
	case $platform in
		curseforge | modrinth | hangar) ;;
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

# Sets loader, mc and game_versions (JSON) for a jar
read_jar() {
	local name
	name=$(basename "$1" .jar)
	name=${name#InventoryWizard-}
	loader=${name%%-*}
	if [ "$loader" = paper ]; then
		mc=
		game_versions=$(all_game_versions)
	else
		mc=${name#*+}
		game_versions=$(game_versions_for "$mc")
	fi
}

mapfile -t jars < <(ls "$DIST"/InventoryWizard-*.jar | sort -V)
if [ -n "${LOADERS:-}" ]; then
	selected=()
	for jar in "${jars[@]}"; do
		read_jar "$jar"
		if [[ " $LOADERS " == *" $loader "* ]]; then
			selected+=("$jar")
		fi
	done
	jars=("${selected[@]}")
fi
if [ ${#jars[@]} -eq 0 ]; then
	echo "no jars in $DIST for loaders: ${LOADERS:-all}" >&2
	exit 1
fi

# --- Modrinth ---------------------------------------------------------------------------------------

publish_modrinth() {
	local existing
	existing=$(curl_api "https://api.modrinth.com/v2/project/$MODRINTH_PROJECT/version")

	local jar
	for jar in "${jars[@]}"; do
		read_jar "$jar"
		local number loaders deps title
		if [ "$loader" = paper ]; then
			number=$VERSION
			loaders='["paper","spigot"]'
			title="InventoryWizard $VERSION (Paper)"
		else
			number="$VERSION+$mc"
			title="InventoryWizard $VERSION ($(display_loader "$loader") $mc)"
			if [ "$loader" = fabric ]; then
				loaders='["fabric","quilt"]'
			else
				loaders="[\"$loader\"]"
			fi
		fi
		if [ "$loader" = fabric ]; then
			deps="[{\"project_id\":\"$MODRINTH_FABRIC_API\",\"dependency_type\":\"required\"}]"
		else
			deps='[]'
		fi

		# A re-run must not upload the same jar twice
		if jq -e --arg n "$number" --arg l "$loader" \
			'any(.[]; .version_number == $n and any(.loaders[]; . == $l))' <<<"$existing" >/dev/null; then
			echo "Modrinth: $title already exists, skipping"
			continue
		fi

		jq -n \
			--arg project "$MODRINTH_PROJECT" \
			--arg name "$title" \
			--arg number "$number" \
			--rawfile changelog "$CHANGELOG_FILE" \
			--argjson loaders "$loaders" \
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

# --- CurseForge -------------------------------------------------------------------------------------

publish_curseforge() {
	local api=https://minecraft.curseforge.com/api
	# Saved to files: the version list is too big to pass to jq as an argument
	if [ -z "$DRY_RUN" ]; then
		curl_api -H "X-Api-Token: $CURSEFORGE_TOKEN" "$api/game/version-types" >"$work/cf-types.json"
		curl_api -H "X-Api-Token: $CURSEFORGE_TOKEN" "$api/game/versions" >"$work/cf-versions.json"
	fi

	# Names -> CurseForge game version ids. Minecraft versions are looked up only in the minecraft-* types,
	# since names such as "1.21" also exist for Bukkit; loaders under modloader, Client under environment.
	cf_ids() {
		jq -n -c \
			--slurpfile types "$work/cf-types.json" \
			--slurpfile versions "$work/cf-versions.json" \
			--argjson mc "$1" \
			--argjson other "$2" '
			def type_ids(f): [$types[0][] | select(.slug | f) | .id];
			def find($name; $tids):
				[$versions[0][] | select(.name == $name and (.gameVersionTypeID as $t | any($tids[]; . == $t)))][0].id
				// error("CurseForge has no game version \($name)");
			type_ids(startswith("minecraft-")) as $mct
			| type_ids(. == "modloader" or . == "environment") as $ot
			| [($mc[] | find(.; $mct)), ($other[] | find(.; $ot))]'
	}

	local jar
	for jar in "${jars[@]}"; do
		read_jar "$jar"
		[ "$loader" = paper ] && continue

		local title other relations ids
		title="InventoryWizard $VERSION ($(display_loader "$loader") $mc)"
		case $loader in
			fabric) other='["Fabric","Quilt","Client"]' ;;
			neoforge) other='["NeoForge","Client"]' ;;
			forge) other='["Forge","Client"]' ;;
		esac
		if [ "$loader" = fabric ]; then
			relations='{"projects":[{"slug":"fabric-api","type":"requiredDependency"}]}'
		else
			# CurseForge rejects an empty projects list, so leave relations out
			relations=null
		fi

		echo "CurseForge: $title -> $(jq -c -n --argjson a "$game_versions" --argjson b "$other" '$a + $b')"
		if [ -n "$DRY_RUN" ]; then
			continue
		fi
		ids=$(cf_ids "$game_versions" "$other")

		jq -n \
			--arg name "$title" \
			--rawfile changelog "$CHANGELOG_FILE" \
			--argjson ids "$ids" \
			--argjson relations "$relations" \
			'{changelog: $changelog, changelogType: "markdown", displayName: $name,
			  gameVersions: $ids, releaseType: "release"}
			 + if $relations then {relations: $relations} else {} end' >"$work/curseforge.json"

		curl_api -X POST "$api/projects/$CURSEFORGE_PROJECT/upload-file" \
			-H "X-Api-Token: $CURSEFORGE_TOKEN" \
			-F "metadata=<$work/curseforge.json;type=application/json" \
			-F "file=@$jar;type=application/java-archive" >/dev/null
	done
}

# --- Hangar -----------------------------------------------------------------------------------------

publish_hangar() {
	local api=https://hangar.papermc.io/api/v1
	local jar=
	for candidate in "${jars[@]}"; do
		read_jar "$candidate"
		if [ "$loader" = paper ]; then
			jar=$candidate
			break
		fi
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
