# Démineur

Minesweeper for Android (Windows 95 style), Kotlin + Jetpack Compose, with optional
Google Play Games leaderboards (local fallback when signed out).

## Conventions

- Code, comments, and commit messages in English; in-app strings are French
  (the app currently ships French-only — no other `res/values-<lang>/` folders).
- Domain logic (`game/`) stays free of Play Games / leaderboard concerns; leaderboard
  access goes through `LeaderboardService`, with a `Noop` fallback so the app is
  always fully playable without a Play Console account.
- When you change anything deployment-related (signing, publishing, store assets,
  hosting, cloud resources), update `docs/agent-references/deployment.md` in the
  same change. Never commit secrets to it, and keep it mechanism-only — no
  time-variable values (specific release tags/versionCodes, edit/run ids, "last
  verified" snapshots) that go stale and mislead the next release.

## Where to look

- Build & run: `README.md`.
- Drive the app on an emulator (screenshots, taps, a visible window to watch it):
  the `run-demineur` skill in `.claude/skills/run-demineur/`.
- Deployment state & how the app ships: `docs/agent-references/deployment.md`
  (step-by-step CLI procedures stay in GitHub issues labelled `deployment`).
