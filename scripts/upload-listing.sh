#!/usr/bin/env bash
# Upload the store listing (texts + all graphics) to Google Play via the Publisher
# API and commit. NOTE: commits on this account may be sent for review automatically
# (no draft mode observed on game-2048's Play account) — confirm on first run for a
# new app. No secrets here.
#
# Needs an owner androidpublisher token (the service account cannot commit the very
# first submission). See scripts/README.md for the ADC login.
# Reads listing files from app/src/main/play/listings/<LANG>/ and identifiers from
# .store-passwd (git-ignored) or the environment.
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/play-api.sh"
play_load_env
play_api_init
ROOT="$PLAY_REPO_ROOT"
PKG="${PACKAGE_NAME:-fr.ghez.demineur}"
LANG_="${LISTING_LANG:-fr-FR}"
BASE="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PKG"
UP="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PKG"
H=("${PLAY_AUTH[@]}")
LD="$ROOT/app/src/main/play/listings/$LANG_"; G="$LD/graphics"

EID=$(curl -fsS "${H[@]}" -X POST "$BASE/edits" | jq -r .id)
echo "edit $EID"

body=$(jq -n --arg l "$LANG_" \
  --arg t "$(cat "$LD/title.txt")" \
  --arg s "$(cat "$LD/short-description.txt")" \
  --arg f "$(cat "$LD/full-description.txt")" \
  '{language:$l,title:$t,shortDescription:$s,fullDescription:$f}')
curl -fsS "${H[@]}" -H "Content-Type: application/json" -X PUT "$BASE/edits/$EID/listings/$LANG_" -d "$body" >/dev/null
echo "listing texts set"

put_imgs() {  # type  dir
  local type=$1 dir=$2
  [ -d "$dir" ] || return 0
  curl -fsS "${H[@]}" -X DELETE "$BASE/edits/$EID/listings/$LANG_/$type" >/dev/null 2>&1 || true
  for img in "$dir"/*.png; do
    [ -f "$img" ] || continue
    curl -fsS "${H[@]}" -H "Content-Type: image/png" --data-binary @"$img" \
      -X POST "$UP/edits/$EID/listings/$LANG_/$type?uploadType=media" >/dev/null
    echo "  + $type/$(basename "$img")"
  done
}
put_imgs icon "$G/icon"
put_imgs featureGraphic "$G/feature-graphic"
put_imgs phoneScreenshots "$G/phone-screenshots"
put_imgs sevenInchScreenshots "$G/sevenInchScreenshots"
put_imgs tenInchScreenshots "$G/tenInchScreenshots"

echo "committing..."
curl -fsS "${H[@]}" -X POST "$BASE/edits/$EID:commit" | jq -r '.id // .'
echo "done."
