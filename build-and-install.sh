#!/usr/bin/env bash
# Build Kick TV APK and optionally install it to the TV via ADB.
set -euo pipefail

cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-/home/kszunio/tools/jdk17}"
export ANDROID_HOME="${ANDROID_HOME:-/home/kszunio/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "==> Building Kick TV APK..."
./gradlew assembleDebug --console=plain

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
echo "==> APK successfully generated at: $APK_PATH"

if [ -n "${TV_HOST:-}" ]; then
  echo "==> TV_HOST detected: $TV_HOST"
  echo "==> Connecting via ADB..."
  adb connect "$TV_HOST:5555" || true
  echo "==> Installing APK to TV..."
  adb -s "$TV_HOST:5555" install -r "$APK_PATH"
  echo "==> Launching Kick TV on TV..."
  adb -s "$TV_HOST:5555" shell am start -n com.personal.kicktv/.MainActivity
  echo "==> Installed & Launched!"
elif adb devices | grep -w "device" >/dev/null; then
  echo "==> Connected ADB device found. Installing..."
  adb install -r "$APK_PATH"
  echo "==> Launching Kick TV..."
  adb shell am start -n com.personal.kicktv/.MainActivity
  echo "==> Done!"
else
  echo ""
  echo "================================================================="
  echo "APK ready to install: $(pwd)/$APK_PATH"
  echo "To install directly to TV over network:"
  echo "  TV_HOST=<TV_IP_ADDRESS> ./build-and-install.sh"
  echo "Or via ADB manually:"
  echo "  adb connect <TV_IP_ADDRESS>:5555"
  echo "  adb install -r $APK_PATH"
  echo "================================================================="
fi
