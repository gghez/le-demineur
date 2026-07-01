#!/usr/bin/env bash
# Upload the signed AAB and create a release on a testing track (default: internal)
# via the Publisher API, then commit. The app stays private to the track's testers —
# nothing public. No secrets here. This is the canonical upload path: a single REST
# call sequence, no Gradle Play Publisher, no Fastlane.
#
# Why REST and not GPP/Fastlane: see docs/agent-references/deployment.md. The token
# rule (owner vs CI/WIF) lives in scripts/lib/play-api.sh.
#
# Env (from .store-passwd or environment):
#   GCP_PROJECT, PACKAGE_NAME (default fr.ghez.demineur)
#   TRACK     - internal | alpha | beta | production (default internal)
#   AAB_PATH  - path to the signed .aab (default: release bundle output)
# Note: production on a never-published ("draft") app rejects 'completed' releases
# ("Only releases with status draft may be created on draft app") — do the first
# production publish via the Console; testing tracks accept 'completed'.
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/play-api.sh"
play_load_env
play_api_init
ROOT="$PLAY_REPO_ROOT"
PKG="${PACKAGE_NAME:-fr.ghez.demineur}"
TRACK="${TRACK:-internal}"
AAB="${AAB_PATH:-$ROOT/app/build/outputs/bundle/release/app-release.aab}"
BASE="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PKG"
UP="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PKG"
H=("${PLAY_AUTH[@]}")

[ -f "$AAB" ] || (cd "$ROOT" && ./gradlew bundleRelease)

EID=$(curl -fsS "${H[@]}" -X POST "$BASE/edits" | jq -r .id)
echo "edit $EID"
VC=$(curl -fsS "${H[@]}" -H "Content-Type: application/octet-stream" --data-binary @"$AAB" \
  -X POST "$UP/edits/$EID/bundles?uploadType=media" | jq -r '.versionCode')
echo "uploaded AAB versionCode=$VC"
curl -fsS "${H[@]}" -H "Content-Type: application/json" -X PUT "$BASE/edits/$EID/tracks/$TRACK" \
  -d "{\"track\":\"$TRACK\",\"releases\":[{\"status\":\"completed\",\"versionCodes\":[\"$VC\"]}]}" >/dev/null
echo "assigned vc=$VC to '$TRACK' (completed)"
curl -fsS "${H[@]}" -X POST "$BASE/edits/$EID:commit" | jq -r '"committed edit " + .id'
echo "Done. Version $VC released on '$TRACK'. Add testers in the Console if not set."
