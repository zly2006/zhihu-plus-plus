#!/bin/bash

set -e  # Exit on error

echo "🔨 Building lite debug APK..."
./gradlew assembleLiteDebug --quiet

APK_PATH="./app/build/outputs/apk/lite/debug/app-lite-debug.apk"
PACKAGE_NAME="com.chloemlla.zhplus.lite"
ACTIVITY="com.chloemlla.zhplus.MainActivity"

# Check if APK was built
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi

echo "📱 Checking device connection..."
DEVICE_COUNT=$(adb devices | grep -w "device" | wc -l)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "❌ No devices connected"
    echo "Please connect a device and enable USB debugging"
    exit 1
fi

if [ "$DEVICE_COUNT" -gt 1 ]; then
    echo "⚠️  Multiple devices connected:"
    adb devices
    echo "Please specify device with: adb -s <device-id>"
    exit 1
fi

echo "📦 Installing APK..."
if adb install -r "$APK_PATH" 2>&1 | grep -q "Success"; then
    echo "✅ Installation successful"
else
    echo "⚠️  Installation failed, trying to uninstall first..."
    adb uninstall "$PACKAGE_NAME" 2>/dev/null || true
    if adb install "$APK_PATH" 2>&1 | grep -q "Success"; then
        echo "✅ Installation successful after uninstall"
    else
        echo "❌ Installation failed"
        exit 1
    fi
fi

echo "🚀 Launching app..."
adb shell am start -n "$PACKAGE_NAME/$ACTIVITY"

echo "✨ Done! App should be launching on your device."
