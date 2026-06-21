# Play Games Services setup

How to wire the app to Google Play Games leaderboards. The game runs fine without any of this
(scores stay local); these steps enable the global leaderboard.

## Already automated (via gcloud)

| Item | Value |
| --- | --- |
| GCP project id | `le-demineur` |
| GCP project number | `511893538007` |
| Enabled APIs | `games`, `gamesconfiguration`, `gamesmanagement` |
| Service account (optional, for scripting leaderboards) | `pgs-publisher@le-demineur.iam.gserviceaccount.com` |
| Debug signing SHA-1 (this machine's `~/.android/debug.keystore`) | `0E:D2:D2:6A:0E:4B:6C:9E:D0:EA:CF:02:CB:19:5E:E7:40:F4:FA:1A` |
| Application id | `fr.ghez.demineur` |

## ⚠️ Timeline — expect several days

Standing up Play Games Services is **not instant**:

- **Developer identity verification** (Play Console account) can take **several days** to clear.
  Until it does, parts of the console (publishing, some Play Games config) stay locked.
- Play Games sign-in only works once: (a) the OAuth credential is created and **approved**, and
  (b) the app has at least an **internal test track / testers list**, with your account added as a
  tester. Newly created credentials can take a while to propagate.
- Leaderboards may need the Play Games Services project to be **published** (not just drafted)
  before scores appear for testers.

**Meanwhile, develop and test locally.** The app is fully playable now: best times use the local
`ScoreRepository` and every Play Games call no-ops while the ids are placeholders. Wire the real
ids in only once verification clears — no code blocked in the interim.

## Remaining steps (Play Console — web)

1. **Play Console** → create the app (type: Game, free).
2. **Play Games Services → Setup**: create a new Play Games Services project and link it to the
   existing Google Cloud project `le-demineur`. Accept the terms.
3. **Credentials → Android**: create an OAuth client with package `fr.ghez.demineur` and the debug
   SHA-1 above. (Add the release/upload key SHA-1 later, before publishing.)
4. **Leaderboards**: create three, all *Time*-formatted and *Smaller is better*:
   - Beginner
   - Intermediate
   - Expert

   Either create them by hand in the console (simplest for three), or script them with the
   `gamesConfiguration` API using the service account above.
5. Add yourself as a **tester** so sign-in works before the game is published.

## Wiring the ids into the app

Replace the placeholders in `app/src/main/res/values/games-ids.xml`:

- `game_services_project_id` — the numeric Play Games Services project id (Play Console → Play
  Games Services → Configuration). Used by the `APP_ID` manifest meta-data.
- `leaderboard_beginner` / `_intermediate` / `_expert` — the leaderboard ids (format `CgkI...EAIQAQ`).

Until replaced, `PlayGamesLeaderboard` detects the `REPLACE_*` placeholders and no-ops, so nothing
crashes. Difficulty → leaderboard mapping lives in `PlayGamesLeaderboard.leaderboardIdFor`.

## Notes

- Custom games are intentionally **not** ranked (variable board size); their best times are kept
  locally only.
- Scores are submitted in milliseconds (`seconds * 1000`) against the time-based leaderboards.
- minSdk 24, compileSdk/targetSdk 35.
