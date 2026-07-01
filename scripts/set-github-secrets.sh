#!/usr/bin/env bash
# Push every GitHub Actions secret that .github/workflows/release.yml consumes.
# Auth is keyless (Workload Identity Federation): there is NO service-account JSON
# key secret — only the WIF provider resource name + the SA email to impersonate.
# No secret value is ever printed. No secret is stored in the repo.
#
# Prereqs: gh authenticated with repo scope; the upload keystore present; the
# leaderboard ids recorded in .store-passwd (see create-leaderboards.sh); WIF set up
# (see scripts/setup-wif.sh) with WIF_PROVIDER recorded in .store-passwd.
#
# Env (from .store-passwd or the environment):
#   KEYSTORE_FILE, STORE_PASSWORD, KEYSTORE_ALIAS, KEY_PASSWORD   (signing)
#   WIF_PROVIDER           - WIF provider resource (from scripts/setup-wif.sh)
#   SERVICE_ACCOUNT_EMAIL  - the play-publisher SA to impersonate
#   PLAY_GAMES_APP_ID, LEADERBOARD_BEGINNER, LEADERBOARD_INTERMEDIATE, LEADERBOARD_EXPERT
#   REPO   target repo (default gghez/le-demineur)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
REPO="${REPO:-gghez/le-demineur}"
: "${KEYSTORE_FILE:?}" "${STORE_PASSWORD:?}" "${KEYSTORE_ALIAS:?}" "${KEY_PASSWORD:?}"
: "${WIF_PROVIDER:?run scripts/setup-wif.sh and record WIF_PROVIDER in .store-passwd}"
: "${SERVICE_ACCOUNT_EMAIL:?set SERVICE_ACCOUNT_EMAIL (the play-publisher SA)}"
: "${PLAY_GAMES_APP_ID:?}" "${LEADERBOARD_BEGINNER:?}" "${LEADERBOARD_INTERMEDIATE:?}" "${LEADERBOARD_EXPERT:?}"
[ -f "$KEYSTORE_FILE" ] || { echo "keystore not found: $KEYSTORE_FILE"; exit 1; }

set_secret() { # name  value
  printf '%s' "$2" | gh secret set "$1" -R "$REPO" >/dev/null && echo "set $1"
}

base64 -w0 "$KEYSTORE_FILE" | gh secret set UPLOAD_KEYSTORE_BASE64 -R "$REPO" >/dev/null && echo "set UPLOAD_KEYSTORE_BASE64"
set_secret RELEASE_STORE_PASSWORD  "$STORE_PASSWORD"
set_secret RELEASE_KEY_ALIAS       "$KEYSTORE_ALIAS"
set_secret RELEASE_KEY_PASSWORD    "$KEY_PASSWORD"
set_secret WIF_PROVIDER            "$WIF_PROVIDER"
set_secret WIF_SERVICE_ACCOUNT     "$SERVICE_ACCOUNT_EMAIL"
set_secret PLAY_GAMES_APP_ID       "$PLAY_GAMES_APP_ID"
set_secret LEADERBOARD_BEGINNER      "$LEADERBOARD_BEGINNER"
set_secret LEADERBOARD_INTERMEDIATE  "$LEADERBOARD_INTERMEDIATE"
set_secret LEADERBOARD_EXPERT        "$LEADERBOARD_EXPERT"

echo
echo "Secrets on $REPO:"; gh secret list -R "$REPO" | awk '{print "  "$1}'
