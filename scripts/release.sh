#!/usr/bin/env bash
# Single CLI entry point for a release. It only wraps the canonical, working paths —
# Gradle for the build, the REST scripts for everything that talks to Play. There is
# no Gradle Play Publisher and no Fastlane here (see docs/agent-references/deployment.md
# for why this shape was chosen).
#
# Requires local.properties wired for signing (see docs/agent-references/deployment.md).
# The publish/listing steps need an owner or CI/WIF token — scripts/lib/play-api.sh.
#
# Usage:
#   scripts/release.sh build      # build the signed AAB only (default)
#   scripts/release.sh publish    # build + upload AAB to the internal track (REST)
#   scripts/release.sh listing    # push store listing text + graphics (REST)
#
# For production once the app has been published once: TRACK=production scripts/publish-internal.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
case "${1:-build}" in
  build)   ./gradlew bundleRelease ;;
  publish) ./gradlew bundleRelease && scripts/publish-internal.sh ;;
  listing) scripts/upload-listing.sh ;;
  *) echo "usage: $0 [build|publish|listing]" >&2; exit 2 ;;
esac
