#!/usr/bin/env bash
# Generate Play Store phone screenshots by driving the app on the emulator via the
# run-demineur skill (.claude/skills/run-demineur/drive.sh) — no separate emulator
# harness is duplicated here, since that skill already boots/installs/launches/shoots
# the app. This script only adds the "walk through a couple of screens and file the
# shots under the Play listing" part. No secrets.
#
# Output: app/src/main/play/listings/fr-FR/graphics/phone-screenshots/{1,2}.png
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRIVE="$ROOT/.claude/skills/run-demineur/drive.sh"
OUT="$ROOT/app/src/main/play/listings/fr-FR/graphics/phone-screenshots"
mkdir -p "$OUT"

"$DRIVE" all   # boot (headless) + installDebug + launch + shot "home"

# The "?" help menu is a small, documented target near (200, 180) on the demineur
# AVD (Pixel 6, 1080x2400); the exact y drifts a few px with the status-bar inset,
# per the skill's own notes — screenshot first if you retarget this on another AVD.
"$DRIVE" tap 200 180
sleep 1
"$DRIVE" shot help

cp "$ROOT/build/emulator-shots/home.png" "$OUT/1.png"
cp "$ROOT/build/emulator-shots/help.png" "$OUT/2.png"
echo "Done. Screenshots in $OUT"
