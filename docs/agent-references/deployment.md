# Deployment reference — how Démineur ships to Google Play

> Living record of the deployment setup. **Keep it current** whenever you change
> anything deployment-related (see the maintenance rule in the root `CLAUDE.md`).
> **Never put secrets here** — no keystore passwords, no service account key
> contents, no absolute machine paths.
>
> **No time-variable values.** Do not record point-in-time identifiers that change
> with every release — specific release tags (`vX.Y.Z`), versionCodes, edit/run ids,
> "last verified" snapshots. Describe the *mechanism* (the tag→versionCode formula,
> the track, the auth flow), never a snapshot that goes stale and misleads the next
> release.
>
> Step-by-step CLI procedures live in the GitHub issues labelled `deployment`. This
> file records the mechanism, not what has or hasn't been run yet.
>
> This pipeline is ported from the sibling `game-2048` project's verified setup
> (WIF + plain REST scripts, no Fastlane, no Gradle Play Publisher) — see that
> project's `docs/agent-references/play-publishing-research.md` for the comparison
> of approaches and the 2026 best-practice rationale behind this shape.

## App identity

- **Display name / package:** `Démineur` / `fr.ghez.demineur` — both already set,
  no rename needed.
- Version: `versionCode` / `versionName` in `app/build.gradle.kts`. On CI they are
  derived from the pushed git tag (`vX.Y.Z` → name `X.Y.Z`, code `X*10000+Y*100+Z`)
  via the `VERSION_NAME` / `VERSION_CODE` env vars; local builds fall back to `1`/`1.0`.

## Wired in the repo

- **Release signing:** `signingConfigs.release` in `app/build.gradle.kts`,
  credentials read from `local.properties` (`RELEASE_STORE_FILE`,
  `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
  **Releases are CI-only (tag-driven).** The release workflow always writes these
  signing props (plus `VERSION_CODE`/`VERSION_NAME`) into `local.properties` before
  building. If a release task is requested **without** those props, the build
  **fails fast** with `GradleException("Release signing not configured: …")`
  instead of silently producing an unsigned AAB. The guard is gated on the resolved
  task graph (`gradle.taskGraph.whenReady`, matching `assemble*/bundle*/package*Release`),
  so `assembleDebug`, `testDebugUnitTest`, and any other non-release task are never
  affected. `*.jks` is git-ignored. Generate the upload keystore with
  `scripts/gen-upload-keystore.sh` (RSA 2048, ~27-year validity); its credentials
  live in `.store-passwd` at the repo root (git-ignored) and are mirrored into
  `local.properties`. Verify a signed AAB with `jarsigner -verify` after
  `./gradlew bundleRelease`.
- **Upload & listing via the REST Publisher API:** no Gradle Play Publisher, no
  Fastlane. The canonical paths are `scripts/publish-internal.sh` (AAB → track) and
  `scripts/upload-listing.sh` (store listing), both plain REST. The token rule lives
  once in `scripts/lib/play-api.sh`.
- **Store graphics:** `scripts/gen-store-assets.sh` (ImageMagick) produces the
  Play-Store-only PNG assets (512 icon, 1024x500 feature graphic) in the app's
  existing Win95 grey/mine palette. The app's actual launcher icon stays the
  hand-drawn vector at `app/src/main/res/drawable/ic_launcher.xml` — nothing to
  regenerate there.
- **R8 minification + resource shrinking:** the `release` build type sets
  `isMinifyEnabled = true` and `isShrinkResources = true`. Keep rules live in
  `app/proguard-rules.pro`.
- **Privacy policy:** `docs/privacy.md`, hosted on GitHub Pages via
  `scripts/enable-github-pages.sh`.

## Cloud / external resources (to be created per `scripts/README.md`)

- **GitHub Pages:** `/docs` on `main` → privacy policy at
  `https://gghez.github.io/le-demineur/privacy`.
- **GCP project + `play-publisher` service account:** dedicated to Play publishing,
  Android Publisher API enabled, id/email recorded in `.store-passwd` (git-ignored).
  A Play Games Services project (`le-demineur`, SA
  `pgs-publisher@le-demineur.iam.gserviceaccount.com`) already exists for
  leaderboards — decide whether the publishing SA reuses that project or a
  dedicated one, matching the "dedicated project per app" pattern from `game-2048`.
  **Play Console access: per-app only** (`scripts/grant-publisher-sa.sh`, default;
  `--account-wide` is the opt-in broad grant, avoid it).
- **Google Play Console:** account + app entry (manual, no API — see below).

## Remaining manual / web steps

These cannot be scripted from here (interactive secrets or web-only consoles):

1. **Back up the upload keystore** — the `.jks` and `.store-passwd` exist locally
   only. Copy them to a safe place; losing the upload key means requesting a reset
   from Google (possible thanks to Play App Signing).
2. **Questionnaires:** content rating (IARC) and Data safety — declare *no data
   collected, no tracking* (Play Games sign-in is opt-in and covered by Google's own
   privacy policy — see `docs/privacy.md`).
3. **Paste the privacy policy URL** into the store listing.
4. **Screenshots:** `scripts/gen-screenshots.sh` drives the `run-demineur` skill's
   emulator to capture them.
5. **Leaderboards — three boards, one per ranked difficulty** (`Difficulty.isRanked`):
   `leaderboard_beginner` / `_intermediate` / `_expert`, all **Time**-formatted,
   smaller-is-better; scores submitted in **milliseconds** (`seconds * 1000`).
   Custom games are never ranked (variable board size). Created via the Games
   Configuration API (`scripts/create-leaderboards.sh`, owner ADC token +
   `x-goog-user-project`). The numeric App ID and the three leaderboard ids go in
   `.store-passwd` and as GitHub Actions secrets (`PLAY_GAMES_APP_ID`,
   `LEADERBOARD_BEGINNER`/`_INTERMEDIATE`/`_EXPERT`); locally in `local.properties`
   (`playGamesAppId`, `leaderboardBeginner`, `leaderboardIntermediate`,
   `leaderboardExpert`) → injected as string resources. The `games.APP_ID` manifest
   meta-data stays commented in the repo (an empty id crashes the Play Games v2 SDK
   at init); CI uncomments it automatically. `PlayGamesLeaderboard` only submits to
   a difficulty's board once its id resValue is non-blank — otherwise that call is
   silently skipped, so the app always builds/runs without any of this configured.

   **Still web-only for sign-in to work at runtime** (no public API for OAuth
   Android clients): in Play Console → Play Games Services → Setup and management →
   Configuration: (a) configure the **OAuth consent screen** (GCP), (b) **Create
   credentials** → Android, bound to the package + the **Play App-signing SHA-1**
   (Test and release → App integrity), and (c) **Review and publish** the Games
   Services project.

## Reproducible scripts

See `scripts/README.md` for the ordered table and prerequisites. Store listing text
+ graphics are versioned under `app/src/main/play/` (`fr-FR` only, matching the
app's single-locale string set). Scripts read sensitive values from `.store-passwd`
(git-ignored) — never hard-coded.

**Still web-only (no API):** content rating (IARC), Ads, App access, Target
audience, app category — these App-content declarations must be filled in the
Console.

## Committing edits via the API — two gotchas (learned from game-2048)

- **Do not set `changesNotSentForReview`** on `:commit` if the account rejects it
  (400 `"must not be set"`) — Gradle Play Publisher set it by default, which is part
  of why this project uses plain REST scripts instead. Confirm the account's actual
  behavior on the first real listing push before assuming either way.
- **Use an owner `androidpublisher` token** to `:commit` — the service account can
  stage edits but its `:commit` typically returns `403` on first submissions (the
  ADC login in `scripts/README.md`).

**AAB release — internal track.** AABs are uploaded and released `completed` on the
`internal` track via `scripts/publish-internal.sh`. **Production rejects `completed`
releases on a never-published ("draft") app** — the first real publish goes through
a testing track (or the first production publish is done in the Console). Internal
testers are added by email list in the Console (*Test → Internal testing →
Testers*); the API only binds Google Groups.

## Release commands (CLI)

```bash
scripts/release.sh build         # build the signed AAB
scripts/publish-internal.sh      # upload AAB + release on the internal track (API)
TRACK=production scripts/publish-internal.sh   # once the app has been published once
```

## Continuous deployment — tag → internal track

`.github/workflows/release.yml` runs on a pushed semver tag (`vX.Y.Z`): it derives
the version from the tag, restores the upload keystore, writes `local.properties`
(signing + leaderboard ids), enables the Play Games meta-data, builds a signed AAB,
**authenticates keyless via Workload Identity Federation** (`google-github-actions/auth`
exchanges the runner's GitHub OIDC token for a ~1h GCP token minted with the
`androidpublisher` scope) and reuses `scripts/publish-internal.sh` to release on the
**internal** track. No long-lived service-account key is stored anywhere.
First production publish stays manual; promote internal → production in the Console.

```bash
git tag vX.Y.Z && git push origin vX.Y.Z   # triggers the release workflow
```

**Required GitHub repo secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `UPLOAD_KEYSTORE_BASE64` | `base64 -w0 <upload.jks>` |
| `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD` | from `.store-passwd` |
| `WIF_PROVIDER` | WIF provider resource name (output of `scripts/setup-wif.sh`) |
| `WIF_SERVICE_ACCOUNT` | the `play-publisher` SA email to impersonate |
| `PLAY_GAMES_APP_ID` | numeric Play Games App ID |
| `LEADERBOARD_BEGINNER` / `LEADERBOARD_INTERMEDIATE` / `LEADERBOARD_EXPERT` | the three board ids |

There is **no `PLAY_SERVICE_ACCOUNT_JSON` secret** — WIF replaces the long-lived key.

### Keyless auth setup (WIF)

`scripts/setup-wif.sh` provisions, in the publishing GCP project, the pool + OIDC
provider and the IAM binding that let GitHub Actions authenticate without a stored
key. Two properties are load-bearing:

- The provider carries the **mandatory attribute condition**
  `assertion.repository == 'gghez/le-demineur'`. GitHub shares one OIDC issuer
  across every repo, so without this condition any repo's workflow could
  impersonate the SA.
- The SA is bound via `roles/iam.workloadIdentityUser` scoped to the repository
  attribute (least privilege) — the federated principal mints the token, not the SA
  itself.

The script prints `WIF_PROVIDER` / `WIF_SERVICE_ACCOUNT`; record `WIF_PROVIDER` in
`.store-passwd` and push both with `scripts/set-github-secrets.sh`.
