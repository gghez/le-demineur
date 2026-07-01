#!/usr/bin/env bash
# Create the three Démineur leaderboards on Play Games Services via the Games
# Configuration API (gamesconfiguration.googleapis.com). Idempotent: a board whose
# name already exists is skipped. No secrets here.
#
# Scope of automation: the API supports leaderboard creation, so this is scripted.
# The REST of Play Games Services setup has NO web-free path and stays manual in the
# Play Console: OAuth consent screen + an Android credential bound to the signing
# SHA-1 + "Review and publish". (The IAP OAuth Admin API that once created consent
# screens is deprecated and never worked for personal Google accounts anyway.)
#
# Auth: the Games Configuration API requires an OWNER on the games project; the
# publisher service account is NOT granted Games Services access, so use the owner's
# Application Default Credentials:
#   gcloud auth application-default login
# The API also needs a quota project header (GCP_PROJECT) because ADC bills the call.
#
# Env (from .store-passwd or the environment):
#   PLAY_GAMES_APP_ID  numeric Play Games application id (= the games project id /
#                      the value behind the games.APP_ID manifest meta-data)
#   GCP_PROJECT        the publishing GCP project, used as the API quota project
#   ACCESS_TOKEN       optional; defaults to the owner ADC token
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
APPID="${PLAY_GAMES_APP_ID:?set PLAY_GAMES_APP_ID (numeric games app id)}"
QP="${GCP_PROJECT:?set GCP_PROJECT (used as API quota project)}"
TOKEN="${ACCESS_TOKEN:-$(gcloud auth application-default print-access-token)}"
BASE="https://www.googleapis.com/games/v1configuration/applications/$APPID/leaderboards"
# Read headers (auth + quota) and write headers (also Content-Type).
RH=(-H "Authorization: Bearer $TOKEN" -H "x-goog-user-project: $QP")
WH=("${RH[@]}" -H "Content-Type: application/json")

# Enable the API (idempotent / no-op if already on).
gcloud services enable gamesconfiguration.googleapis.com --project="$QP" >/dev/null

# Note: if fr-FR is not an enabled project language, store the label under the
# project default locale (en-US) instead — the console shows it to every player
# regardless of the label's own locale.
create_board() { # name  sortRank
  local name="$1" rank="$2"
  if curl -fsS "${RH[@]}" "$BASE" \
       | jq -e --arg n "$name" '.items[]?.draft.name.translations[]? | select(.value==$n)' \
       >/dev/null 2>&1; then
    echo "exists:  $name"; return
  fi
  local body id
  body=$(jq -n --arg n "$name" --argjson r "$rank" \
    '{scoreOrder:"SMALLER_IS_BETTER", draft:{sortRank:$r, name:{translations:[{locale:"en-US",value:$n}]}, scoreFormat:{numberFormatType:"TIME_DURATION"}}}')
  id=$(curl -fsS "${WH[@]}" -X POST "$BASE" -d "$body" | jq -r '.id')
  echo "created: $name -> $id"
}

# Times are submitted in milliseconds (seconds * 1000); smaller is better. Custom
# games are never ranked (variable board size) and have no leaderboard.
create_board "Meilleurs temps — Débutant"      1
create_board "Meilleurs temps — Intermédiaire" 2
create_board "Meilleurs temps — Expert"        3

echo
echo "Board ids (set these as GitHub secrets / local.properties via set-github-secrets.sh):"
curl -fsS "${RH[@]}" "$BASE" \
  | jq -r '.items[] | "  \(.draft.name.translations[0].value)\t\(.id)\t\(.scoreOrder)\t\(.draft.scoreFormat.numberFormatType)"'
