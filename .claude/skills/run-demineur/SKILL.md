---
name: run-demineur
description: Run, launch, build, install, or screenshot the Démineur Android app on an emulator. Use when asked to run/start the app, boot the Android emulator, take a screenshot of the game, or visually verify a UI change on Android.
allowed-tools: Bash, Read
---

# Run the Démineur Android app

Compose/Kotlin Android game (package `fr.ghez.demineur`, `minSdk 24`, `target 35`).
There is no desktop entry point — it runs on an Android emulator (or device) and is
driven through **[`drive.sh`](drive.sh)**, a wrapper over `emulator` / `gradlew` /
`adb`. The driver boots a headless AVD, installs the debug APK, launches the app, and
screenshots it — so you can see and poke the UI without a physical phone.

All paths below are relative to the repo root (the directory holding `gradlew`).
The driver is at `.claude/skills/run-demineur/drive.sh`; it resolves the Android SDK
itself (`ANDROID_HOME` → `ANDROID_SDK_ROOT` → `sdk.dir` in `local.properties` →
`$HOME/Android/Sdk`), so you do not need to export anything.

## Prerequisites

Android command-line SDK with the emulator, platform-tools, and one x86_64 system
image. JDK 17 (for AGP 8.7). If the system image is missing, `drive.sh` prints this
line — run it once:

```bash
sdkmanager "platform-tools" "emulator" "system-images;android-35;google_apis;x86_64"
```

`gradlew` downloads Gradle 8.10.2 on first use; no manual Gradle install needed.

## Run (agent path) — use this

One command does boot + install + launch + screenshot:

```bash
.claude/skills/run-demineur/drive.sh all
```

It reuses a running emulator if there is one, else creates the `demineur` AVD (from the
system image above) and cold-boots it headless. The home screenshot lands at
`build/emulator-shots/home.png` — **open it** to confirm the Win95 chrome rendered.

Drive individual steps or the UI:

```bash
.claude/skills/run-demineur/drive.sh boot          # boot AVD headless / reuse running one
.claude/skills/run-demineur/drive.sh install       # ./gradlew :app:installDebug
.claude/skills/run-demineur/drive.sh launch        # start MainActivity, assert it runs
.claude/skills/run-demineur/drive.sh shot help     # -> build/emulator-shots/help.png
.claude/skills/run-demineur/drive.sh tap 210 246   # inject a tap (drive the UI)
.claude/skills/run-demineur/drive.sh stop          # kill the emulator when done
```

Example — open the "Comment jouer" dialog and capture it (tap coords are for the
`demineur` AVD, a Pixel 6 profile at 1080×2400; re-measure for other AVDs):

```bash
.claude/skills/run-demineur/drive.sh tap 210 246 && sleep 1 && .claude/skills/run-demineur/drive.sh shot help
```

## Run (human path)

Open the project in Android Studio and Run, or `./gradlew :app:installDebug` against a
plugged-in device, then launch from the launcher. Useless headless — use the driver.

## Test

```bash
./gradlew :app:testDebugUnitTest --console=plain    # GameEngine unit tests
```

## Gotchas

- **Headless boot needs software GPU.** `-gpu swiftshader_indirect -no-window` is the
  reliable combo under WSL/CI; the default `-gpu host` has no display and hangs. The
  driver already passes these.
- **Cold boot ~40s.** The driver polls `sys.boot_completed`; be patient on first boot.
- **`google_apis` image, not `google_apis_playstore`.** No Play Store, so Google Play
  Games sign-in no-ops — but the app is built to run fine offline (leaderboard calls are
  guarded), so the game itself is fully testable.
- **Tap coordinates are per-AVD.** They depend on the device profile's resolution. The
  `demineur` AVD is 1080×2400; other AVDs differ. Screenshot first, measure, then tap.
- **Screenshots go to `build/emulator-shots/`** (gitignored). The emulator log is
  `build/emulator-shots/emulator.log` if a boot fails.

## Troubleshooting

- `No connected devices!` on install → the emulator isn't up. Run `drive.sh boot` first
  (or just `drive.sh all`).
- `device offline` / adb stuck → `adb kill-server && adb start-server`, then re-boot.
- Testing on a **physical phone under WSL** instead of the emulator: the phone must be
  attached to WSL from Windows via `usbipd attach --wsl --busid <id>`; a phone only
  listed under usbipd *Persisted* (not *Connected*) is physically unplugged.
- `system image ... not installed` → run the `sdkmanager` line under Prerequisites.
