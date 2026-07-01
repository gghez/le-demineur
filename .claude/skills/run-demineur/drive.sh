#!/usr/bin/env bash
#
# drive.sh — launch and drive the Démineur Android app on an emulator.
#
# The harness for the run-demineur skill. Every step (SDK lookup, AVD creation,
# headless boot, install, launch, screenshot, tap) is a subcommand so an agent
# can reach the running app programmatically. No machine-specific paths: the SDK
# is resolved from the environment / local.properties / the conventional default.
#
# Usage:
#   drive.sh all              # headless: boot + install + launch + screenshot
#   drive.sh window           # VISIBLE window (for a human to watch): boot + install + launch
#   drive.sh boot             # boot the AVD headless (or reuse a running one)
#   drive.sh install          # ./gradlew :app:installDebug
#   drive.sh launch           # start MainActivity
#   drive.sh shot [name]      # screencap -> $SHOT_DIR/<name>.png (default: screen)
#   drive.sh tap <x> <y>      # inject a tap (drive the UI)
#   drive.sh stop             # kill the running emulator
#   drive.sh sdk              # print the resolved SDK path and exit
#
# Env overrides: DEMINEUR_AVD, DEMINEUR_SHOT_DIR, DEMINEUR_SYSIMG, DEMINEUR_DEVICE,
#                DEMINEUR_HEADLESS (1 = no window [default], 0 = visible window)
set -euo pipefail

SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SKILL_DIR/../../.." && pwd)"

PACKAGE="fr.ghez.demineur"
ACTIVITY="$PACKAGE/.MainActivity"
AVD="${DEMINEUR_AVD:-demineur}"
SYSIMG="${DEMINEUR_SYSIMG:-system-images;android-35;google_apis;x86_64}"
DEVICE="${DEMINEUR_DEVICE:-pixel_6}"
SHOT_DIR="${DEMINEUR_SHOT_DIR:-$REPO_ROOT/build/emulator-shots}"
HEADLESS="${DEMINEUR_HEADLESS:-1}"

# --- Resolve the Android SDK without hardcoding any absolute path ----------
resolve_sdk() {
  if [ -n "${ANDROID_HOME:-}" ]; then echo "$ANDROID_HOME"; return; fi
  if [ -n "${ANDROID_SDK_ROOT:-}" ]; then echo "$ANDROID_SDK_ROOT"; return; fi
  if [ -f "$REPO_ROOT/local.properties" ]; then
    local dir; dir="$(sed -n 's/^sdk\.dir=//p' "$REPO_ROOT/local.properties" | head -1)"
    if [ -n "$dir" ]; then echo "$dir"; return; fi
  fi
  echo "$HOME/Android/Sdk"
}

ANDROID_HOME="$(resolve_sdk)"
ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_HOME ANDROID_SDK_ROOT
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

die() { echo "drive.sh: $*" >&2; exit 1; }

require_sdk() {
  [ -x "$ANDROID_HOME/platform-tools/adb" ] || die "adb not found under $ANDROID_HOME (set ANDROID_HOME or sdk.dir in local.properties)"
  [ -x "$ANDROID_HOME/emulator/emulator" ] || die "emulator not found under $ANDROID_HOME (install the 'emulator' SDK package)"
}

# First already-booted emulator serial, empty if none.
running_serial() { adb devices | awk '/^emulator-[0-9]+\tdevice$/{print $1; exit}'; }

ensure_avd() {
  if emulator -list-avds | grep -qx "$AVD"; then return; fi
  echo "drive.sh: creating AVD '$AVD' from $SYSIMG ..."
  if [ ! -d "$ANDROID_HOME/${SYSIMG//;//}" ]; then
    die "system image '$SYSIMG' not installed. Install it with:
    sdkmanager \"$SYSIMG\""
  fi
  echo "no" | avdmanager create avd -n "$AVD" -k "$SYSIMG" -d "$DEVICE" --force
}

wait_boot() {
  echo "drive.sh: waiting for device..."
  timeout 180 adb wait-for-device || die "device never registered"
  echo "drive.sh: waiting for boot_completed..."
  local i
  for i in $(seq 1 60); do
    [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && { echo "drive.sh: boot complete"; return; }
    sleep 5
  done
  die "boot did not complete in time"
}

cmd_boot() {
  require_sdk
  local serial; serial="$(running_serial)"
  if [ -n "$serial" ]; then echo "drive.sh: reusing running emulator $serial"; return; fi
  ensure_avd
  mkdir -p "$SHOT_DIR"
  # swiftshader (software) is the reliable GPU under WSL/CI. Detached so the
  # script returns; logs go to the shot dir.
  local winflags=""
  if [ "$HEADLESS" = "1" ]; then
    winflags="-no-window -no-boot-anim"
    echo "drive.sh: booting '$AVD' headless (software GPU)..."
  else
    echo "drive.sh: booting '$AVD' with a VISIBLE window (needs a display; WSLg on Windows)..."
  fi
  # shellcheck disable=SC2086  # winflags is an intentional word-split flag list
  nohup emulator -avd "$AVD" $winflags -no-audio \
    -gpu swiftshader_indirect -no-snapshot >"$SHOT_DIR/emulator.log" 2>&1 &
  wait_boot
}

cmd_install() {
  require_sdk
  ( cd "$REPO_ROOT" && ./gradlew :app:installDebug --console=plain )
}

cmd_launch() {
  require_sdk
  adb shell am start -n "$ACTIVITY" >/dev/null
  sleep 3
  if adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
    echo "drive.sh: $PACKAGE running (pid $(adb shell pidof "$PACKAGE" | tr -d '\r'))"
  else
    die "$PACKAGE did not start"
  fi
}

cmd_shot() {
  require_sdk
  local name="${1:-screen}"
  mkdir -p "$SHOT_DIR"
  adb exec-out screencap -p > "$SHOT_DIR/$name.png"
  echo "drive.sh: wrote $SHOT_DIR/$name.png"
}

cmd_tap() {
  require_sdk
  [ $# -eq 2 ] || die "tap needs <x> <y>"
  adb shell input tap "$1" "$2"
}

cmd_stop() {
  require_sdk
  local serial; serial="$(running_serial)"
  [ -n "$serial" ] && { adb -s "$serial" emu kill; echo "drive.sh: killed $serial"; } || echo "drive.sh: no emulator running"
}

case "${1:-all}" in
  sdk)     echo "$ANDROID_HOME" ;;
  boot)    cmd_boot ;;
  install) cmd_install ;;
  launch)  cmd_launch ;;
  shot)    shift; cmd_shot "${1:-screen}" ;;
  tap)     shift; cmd_tap "$@" ;;
  stop)    cmd_stop ;;
  all)     cmd_boot; cmd_install; cmd_launch; cmd_shot home ;;
  window)  HEADLESS=0; cmd_boot; cmd_install; cmd_launch
           echo "drive.sh: look at your desktop — the emulator window is showing the app" ;;
  help|-h|--help) sed -n '2,27p' "${BASH_SOURCE[0]}" ;;
  *)       die "unknown command '$1' (try: all window boot install launch shot tap stop sdk)" ;;
esac
