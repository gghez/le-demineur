#!/usr/bin/env bash
# Grant the publishing service account access to the Play Console via the Android
# Publisher API. Re-runnable. No secrets in this file.
#
# DEFAULT = per-app least privilege (one app only). Pass --account-wide to grant
# account-level permissions instead (every app on the developer account) — only do
# this if per-app is impractical; it regresses the project's least-privilege stance.
#
# Needs an OWNER androidpublisher token: Play Console permissions are not GCP IAM,
# and the SA cannot grant itself its first access. The token rule lives in
# scripts/lib/play-api.sh (set ACCESS_TOKEN, or log ADC in once with the scope).
#
# Reads from .store-passwd (git-ignored) or the environment:
#   DEVELOPER_ID           - number in the Console URL .../developers/<DEVELOPER_ID>/...
#   SERVICE_ACCOUNT_EMAIL  - the play-publisher service account email
#   GCP_PROJECT            - project where the Android Publisher API is enabled (quota)
#   PACKAGE_NAME           - app package, per-app mode only (default fr.ghez.demineur)
#
# Usage:
#   scripts/grant-publisher-sa.sh                 # per-app (preferred, least privilege)
#   scripts/grant-publisher-sa.sh --account-wide  # all apps (broad — avoid)
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/play-api.sh"
play_load_env
play_api_init

SCOPE="app"
case "${1:-}" in
  --account-wide) SCOPE="account" ;;
  "")             SCOPE="app" ;;
  *) echo "usage: $0 [--account-wide]" >&2; exit 2 ;;
esac

: "${DEVELOPER_ID:?set DEVELOPER_ID (number in the Play Console URL)}"
: "${SERVICE_ACCOUNT_EMAIL:?set SERVICE_ACCOUNT_EMAIL}"
PACKAGE_NAME="${PACKAGE_NAME:-fr.ghez.demineur}"
API="https://androidpublisher.googleapis.com/androidpublisher/v3"
auth=("${PLAY_AUTH[@]}" -H "Content-Type: application/json")
EMAIL_ENC="${SERVICE_ACCOUNT_EMAIL/@/%40}"

if [ "$SCOPE" = "account" ]; then
  # Account-level permissions apply to EVERY app on the developer account.
  read -r -d '' BODY <<JSON || true
{
  "email": "${SERVICE_ACCOUNT_EMAIL}",
  "developerAccountPermissions": [
    "CAN_SEE_ALL_APPS",
    "CAN_MANAGE_PUBLIC_APKS_GLOBAL",
    "CAN_MANAGE_TRACK_APKS_GLOBAL",
    "CAN_MANAGE_PUBLIC_LISTING_GLOBAL"
  ]
}
JSON
  echo "Granting $SERVICE_ACCOUNT_EMAIL ACCOUNT-WIDE access to developer $DEVELOPER_ID ..."
  curl -fsS -X POST "${auth[@]}" "$API/developers/${DEVELOPER_ID}/users" -d "$BODY"
  echo
  echo "Done. The SA can publish EVERY app on the account (account-wide grant)."
  exit 0
fi

# Per-app least privilege: a user must hold at least one permission, so an app-only
# user is created with the grant INLINE (no developerAccountPermissions -> no access
# to other apps). If the user already exists, add/update the grant for this package.
PERMS='["CAN_MANAGE_PUBLIC_APKS","CAN_MANAGE_TRACK_APKS","CAN_MANAGE_PUBLIC_LISTING"]'
if curl -fsS "${auth[@]}" "$API/developers/${DEVELOPER_ID}/users?pageSize=-1" \
     | jq -e --arg e "$SERVICE_ACCOUNT_EMAIL" '.users[]?|select(.email==$e)' >/dev/null 2>&1; then
  echo "User exists — granting app-level access to ${PACKAGE_NAME}..."
  curl -fsS -X POST "${auth[@]}" \
    "$API/developers/${DEVELOPER_ID}/users/${EMAIL_ENC}/grants" \
    -d "{\"packageName\":\"${PACKAGE_NAME}\",\"appLevelPermissions\":${PERMS}}" \
  || { echo "grant create failed (already granted?) — patching..."; \
       curl -fsS -X PATCH "${auth[@]}" \
         "$API/developers/${DEVELOPER_ID}/users/${EMAIL_ENC}/grants/${PACKAGE_NAME}?updateMask=appLevelPermissions" \
         -d "{\"appLevelPermissions\":${PERMS}}"; }
else
  echo "Creating app-only user with inline grant for ${PACKAGE_NAME}..."
  curl -fsS -X POST "${auth[@]}" "$API/developers/${DEVELOPER_ID}/users" \
    -d "{\"email\":\"${SERVICE_ACCOUNT_EMAIL}\",\"grants\":[{\"packageName\":\"${PACKAGE_NAME}\",\"appLevelPermissions\":${PERMS}}]}"
fi
echo
echo "Done. The SA can publish ${PACKAGE_NAME} only — no access to other apps."
