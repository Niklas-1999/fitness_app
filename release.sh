#!/usr/bin/env bash
# Build the signed release APK, verify it (signature, permissions, no Google/network) and install it.
# Usage: ./release.sh            build + verify + install on the USB phone
#        ./release.sh build      build + verify only
set -e
cd "$(dirname "$0")"
TOOLS="${ANDROID_TOOLS:-$HOME/android-tools}"
export JAVA_HOME="${JAVA_HOME_21:-$TOOLS/jdk21}" ANDROID_HOME="$TOOLS/sdk"
BT="$ANDROID_HOME/build-tools/35.0.0"
[ -f keystore.properties ] || { echo "keystore.properties missing (see HANDOFF.md: release signing)"; exit 1; }
./gradlew assembleRelease -q --console=plain 2>&1 | grep -v '^WARNING' || true
APK=app/build/outputs/apk/release/app-release.apk
"$BT/apksigner" verify --print-certs "$APK" | grep -E "SHA-256|Signer #1 certificate DN"
echo "--- permissions"; "$BT/aapt" dump permissions "$APK" | grep uses-permission
if "$BT/aapt" dump permissions "$APK" | grep -q "android.permission.INTERNET"; then echo "FAIL: INTERNET permission present"; exit 1; fi
if unzip -p "$APK" 'classes*.dex' | grep -a -q -E "com/google/firebase|com/google/android/gms"; then echo "FAIL: Google services classes"; exit 1; fi
echo "--- OK: no INTERNET, no Google/Firebase. Size: $(du -h "$APK" | cut -f1)"
cp "$APK" "IRONCORE-$(grep versionName app/build.gradle.kts | head -1 | grep -o '[0-9.]*').apk"
[ "$1" = "build" ] && exit 0
"$ANDROID_HOME/platform-tools/adb" install -r "$APK"
