#!/usr/bin/env bash
# Enable GitHub Pages from /docs on main (serves docs/privacy.md). Re-runnable.
# Requires the gh CLI authenticated. No secrets.
#
# Env (optional):
#   GH_REPO - owner/repo (default: auto-detected from the current repo's origin)
set -euo pipefail
REPO="${GH_REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
gh api -X POST "repos/$REPO/pages" -f 'source[branch]=main' -f 'source[path]=/docs' 2>/dev/null \
  || gh api -X PUT "repos/$REPO/pages" -f 'source[branch]=main' -f 'source[path]=/docs'
URL="$(gh api "repos/$REPO/pages" -q .html_url)"
echo "GitHub Pages enabled. Base: $URL  | Privacy policy: ${URL%/}/privacy"
