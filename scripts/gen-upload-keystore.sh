#!/usr/bin/env bash
# Generate the release upload keystore with strong random passwords, store the
# credentials in .store-passwd (git-ignored) and wire them into local.properties.
# Refuses to overwrite an existing keystore. No secrets in this file.
#
# Env (optional):
#   KEYSTORE_FILE  - target .jks path (default: $HOME/keystores/le-demineur-upload.jks)
#   KEYSTORE_ALIAS - key alias (default: demineur)
#   DNAME          - certificate DN (default: CN=gghez, O=gghez, C=FR)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KS="${KEYSTORE_FILE:-$HOME/keystores/le-demineur-upload.jks}"
ALIAS="${KEYSTORE_ALIAS:-demineur}"
DNAME="${DNAME:-CN=gghez, O=gghez, C=FR}"

if [ -f "$KS" ]; then
  echo "Keystore already exists at $KS — refusing to overwrite." >&2
  exit 1
fi
mkdir -p "$(dirname "$KS")"; chmod 700 "$(dirname "$KS")"
STORE_PW="$(openssl rand -hex 24)"
KEY_PW="$(openssl rand -hex 24)"

keytool -genkeypair -v -keystore "$KS" -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 -storetype JKS \
  -dname "$DNAME" -storepass "$STORE_PW" -keypass "$KEY_PW"

umask 077
cat > "$ROOT/.store-passwd" <<EOF
# Démineur UPLOAD keystore credentials — NEVER commit or share. Back up the .jks securely.
# With Play App Signing this is only the upload key; Google holds the final app-signing key.
KEYSTORE_FILE=$KS
KEYSTORE_ALIAS=$ALIAS
STORE_PASSWORD=$STORE_PW
KEY_PASSWORD=$KEY_PW
EOF
chmod 600 "$ROOT/.store-passwd"

# Wire local.properties (idempotent: drop any previous RELEASE_* lines first)
LP="$ROOT/local.properties"; touch "$LP"
grep -v '^RELEASE_' "$LP" > "$LP.tmp" || true; mv "$LP.tmp" "$LP"
cat >> "$LP" <<EOF
RELEASE_STORE_FILE=$KS
RELEASE_STORE_PASSWORD=$STORE_PW
RELEASE_KEY_ALIAS=$ALIAS
RELEASE_KEY_PASSWORD=$KEY_PW
EOF
echo "Keystore created at $KS; credentials in .store-passwd; local.properties wired."
echo "BACK UP both $KS and .store-passwd in a safe place."
