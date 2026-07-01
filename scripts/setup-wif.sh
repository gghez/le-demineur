#!/usr/bin/env bash
# Create Workload Identity Federation (WIF) so GitHub Actions authenticates to
# Google Play KEYLESS — no long-lived service-account JSON key stored anywhere.
# GitHub mints a short-lived OIDC token, exchanged for a ~1h GCP token. Re-runnable
# (skips existing resources). No secrets in this file.
#
# SECURITY — mandatory attribute condition: GitHub shares ONE OIDC issuer across
# every repo on github.com, so the provider MUST restrict to this repo, otherwise
# any repo's workflow could impersonate the SA. This script always sets that
# condition; ALLOWED_REF_PREFIX tightens it further to release tags.
#
# Reads from .store-passwd (git-ignored) or the environment:
#   GCP_PROJECT            - the dedicated Play-publishing project id
#   SERVICE_ACCOUNT_EMAIL  - the play-publisher SA to impersonate (keep least privilege)
#   GITHUB_REPO            - owner/repo allowed to use the pool (default gghez/le-demineur)
#   POOL_ID                - WIF pool id (default github-pool)
#   PROVIDER_ID            - WIF provider id (default github-provider)
#   ALLOWED_REF_PREFIX     - optional: e.g. refs/tags/v to only accept release tags
#
# Requires gcloud authenticated as a project owner/editor of GCP_PROJECT.
# Usage:  scripts/setup-wif.sh
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/play-api.sh"
play_load_env
: "${GCP_PROJECT:?set GCP_PROJECT (dedicated Play-publishing project id)}"
: "${SERVICE_ACCOUNT_EMAIL:?set SERVICE_ACCOUNT_EMAIL (the play-publisher SA)}"
GITHUB_REPO="${GITHUB_REPO:-gghez/le-demineur}"
POOL_ID="${POOL_ID:-github-pool}"
PROVIDER_ID="${PROVIDER_ID:-github-provider}"

# Token exchange (STS) + token minting (IAM Credentials) + pools (IAM) APIs.
gcloud services enable \
  sts.googleapis.com iamcredentials.googleapis.com iam.googleapis.com \
  --project="$GCP_PROJECT"

PROJECT_NUMBER="$(gcloud projects describe "$GCP_PROJECT" --format='value(projectNumber)')"

# 1. Pool
gcloud iam workload-identity-pools describe "$POOL_ID" \
    --project="$GCP_PROJECT" --location=global >/dev/null 2>&1 \
  || gcloud iam workload-identity-pools create "$POOL_ID" \
       --project="$GCP_PROJECT" --location=global \
       --display-name="GitHub Actions pool"

# 2. OIDC provider with the mandatory repo restriction (+ optional ref pin).
COND="assertion.repository == '${GITHUB_REPO}'"
[ -n "${ALLOWED_REF_PREFIX:-}" ] && COND="${COND} && assertion.ref.startsWith('${ALLOWED_REF_PREFIX}')"
if gcloud iam workload-identity-pools providers describe "$PROVIDER_ID" \
     --project="$GCP_PROJECT" --location=global \
     --workload-identity-pool="$POOL_ID" >/dev/null 2>&1; then
  gcloud iam workload-identity-pools providers update-oidc "$PROVIDER_ID" \
    --project="$GCP_PROJECT" --location=global \
    --workload-identity-pool="$POOL_ID" \
    --attribute-condition="$COND"
else
  gcloud iam workload-identity-pools providers create-oidc "$PROVIDER_ID" \
    --project="$GCP_PROJECT" --location=global \
    --workload-identity-pool="$POOL_ID" \
    --display-name="GitHub provider" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
    --attribute-condition="$COND"
fi

# 3. Bind the SA to this repo's federated identity (least privilege: scoped to the
#    repository attribute, so only GITHUB_REPO can impersonate the SA).
MEMBER="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL_ID}/attribute.repository/${GITHUB_REPO}"
gcloud iam service-accounts add-iam-policy-binding "$SERVICE_ACCOUNT_EMAIL" \
  --project="$GCP_PROJECT" \
  --role="roles/iam.workloadIdentityUser" \
  --member="$MEMBER"

PROVIDER_RESOURCE="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL_ID}/providers/${PROVIDER_ID}"
echo
echo "WIF ready. Set these two GitHub secrets (not committed — they carry the"
echo "project number / SA email):"
echo "  WIF_PROVIDER        = ${PROVIDER_RESOURCE}"
echo "  WIF_SERVICE_ACCOUNT = ${SERVICE_ACCOUNT_EMAIL}"
echo
echo "scripts/set-github-secrets.sh pushes them if you record WIF_PROVIDER in .store-passwd."
