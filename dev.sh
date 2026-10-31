#!/usr/bin/env bash
# Build the debug app, install it on the connected phone and (re)launch it.
# Usage: ./dev.sh          build + install + launch
#        ./dev.sh mirror   also open the scrcpy mirror window
set -e
cd "$(dirname "$0")"
TOOLS="${ANDROID_TOOLS:-$HOME/android-tools}"
export JAVA_HOME="${JAVA_HOME_21:-$TOOLS/jdk21}"
export ANDROID_HOME="$TOOLS/sdk"
ADB="$ANDROID_HOME/platform-tools/adb"
PKG="dev.niklas.ironcore.debug"

./gradlew installDebug -q --console=plain 2>&1 | grep -v '^WARNING' || true
"$ADB" shell am force-stop "$PKG"
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1

if [ "$1" = "mirror" ] && ! pgrep -x scrcpy >/dev/null; then
  ADB="$ADB" nohup "$TOOLS/scrcpy/scrcpy" --stay-awake --window-title "Fitness RPG (phone)" >/dev/null 2>&1 &
fi
