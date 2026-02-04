---
name: launch-on-device
description: Build, install, and launch the Zhihu++ Android app on a connected device using ADB. Includes comprehensive troubleshooting for common issues like missing devices, installation failures, signature mismatches, and app crashes. Use when deploying debug builds to physical devices or emulators.
license: CC BY-NC-SA 4.0
---

# Launch on Device Skill

## Overview
This skill documents how to build, install, and launch the Zhihu++ Android app on a connected device using ADB (Android Debug Bridge).

## Prerequisites
- ADB installed and available in PATH
- Android device connected via USB or wireless ADB
- USB debugging enabled on device
- Project built successfully

## Quick Start

```bash
# 1. Build the lite debug APK
./gradlew assembleLiteDebug

# 2. Install and launch on device
adb install -r ./app/build/outputs/apk/lite/debug/app-lite-debug.apk
adb shell am start -n com.github.zly2006.zhplus.lite/com.github.zly2006.zhihu.MainActivity
```

## Detailed Workflow

### Step 1: Check ADB Connection

```bash
# Check if adb is available
which adb

# Check adb version
adb --version

# List connected devices
adb devices
```

Expected output:
```
List of devices attached
<device-id>    device
```

### Step 2: Build the APK

```bash
cd /path/to/Zhihu
./gradlew assembleLiteDebug --quiet
```

The APK will be generated at: `./app/build/outputs/apk/lite/debug/app-lite-debug.apk`

### Step 3: Install APK to Device

```bash
# Install with -r flag to replace existing installation
adb install -r ./app/build/outputs/apk/lite/debug/app-lite-debug.apk
```

Expected output: `Success`

### Step 4: Launch the App

```bash
# Launch the main activity
adb shell am start -n com.github.zly2006.zhplus.lite/com.github.zly2006.zhihu.MainActivity
```

Expected output: `Starting: Intent { cmp=com.github.zly2006.zhplus.lite/com.github.zly2006.zhihu.MainActivity }`

## App Variants

The project has two build variants:

1. **Lite Version** (recommended for development)
   - Package: `com.github.zly2006.zhplus.lite`
   - APK: `./app/build/outputs/apk/lite/debug/app-lite-debug.apk`
   - Main Activity: `com.github.zly2006.zhihu.MainActivity`

2. **Full Version**
   - Package: `com.github.zly2006.zhplus`
   - APK: `./app/build/outputs/apk/full/debug/app-full-debug.apk`
   - Main Activity: `com.github.zly2006.zhihu.MainActivity`

## Common Issues and Solutions

### Issue 1: ADB Server Not Running

**Symptom:**
```
error: could not connect to daemon
```

**Solution:**
```bash
# Kill and restart adb server
adb kill-server
adb start-server
adb devices
```

### Issue 2: No Devices Connected

**Symptom:**
```
List of devices attached
(empty list)
```

**Solutions:**

a) **USB Connection Issues:**
```bash
# Check USB cable and connection
# Replug the device
# Check device is in USB debugging mode
# Accept USB debugging prompt on device
```

b) **Unauthorized Device:**
```bash
# Check device screen for authorization dialog
# Accept "Allow USB debugging" prompt
# Check again
adb devices
```

c) **Wireless ADB Connection:**
```bash
# If using wireless ADB, reconnect
adb connect <device-ip>:5555
```

### Issue 3: APK Not Found

**Symptom:**
```
adb: failed to stat ./app/build/outputs/apk/lite/debug/app-lite-debug.apk: No such file or directory
```

**Solution:**
```bash
# Build the APK first
./gradlew assembleLiteDebug

# Verify APK exists
ls -lh ./app/build/outputs/apk/lite/debug/app-lite-debug.apk

# If still not found, clean and rebuild
./gradlew clean assembleLiteDebug
```

### Issue 4: Installation Failed (INSTALL_FAILED_UPDATE_INCOMPATIBLE)

**Symptom:**
```
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.github.zly2006.zhplus.lite signatures do not match newer version]
```

**Solution:**
```bash
# Uninstall existing app first
adb uninstall com.github.zly2006.zhplus.lite

# Then install again
adb install ./app/build/outputs/apk/lite/debug/app-lite-debug.apk
```

### Issue 5: Installation Failed (Insufficient Storage)

**Symptom:**
```
Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]
```

**Solution:**
```bash
# Check device storage
adb shell df -h

# Free up space on device or uninstall unused apps
adb shell pm list packages | grep -i <package-pattern>
adb uninstall <package-name>
```

### Issue 6: Multiple Devices Connected

**Symptom:**
```
error: more than one device/emulator
```

**Solution:**
```bash
# List devices to get device ID
adb devices

# Use -s flag to specify target device
adb -s <device-id> install -r ./app/build/outputs/apk/lite/debug/app-lite-debug.apk
adb -s <device-id> shell am start -n com.github.zly2006.zhplus.lite/com.github.zly2006.zhihu.MainActivity
```

### Issue 7: App Crashes on Launch

**Symptom:**
App installs but crashes immediately after launch.

**Solution:**
```bash
# Check logcat for crash details
adb logcat | grep -i "AndroidRuntime\|FATAL\|zhplus"

# Or filter by app package
adb logcat --pid=$(adb shell pidof -s com.github.zly2006.zhplus.lite)

# Clear app data and try again
adb shell pm clear com.github.zly2006.zhplus.lite
adb shell am start -n com.github.zly2006.zhplus.lite/com.github.zly2006.zhihu.MainActivity
```

### Issue 8: Activity Not Found

**Symptom:**
```
Error: Activity class {...} does not exist.
```

**Solution:**
```bash
# Verify package is installed
adb shell pm list packages | grep zhplus

# Check main activity from manifest
adb shell dumpsys package com.github.zly2006.zhplus.lite | grep -A 1 "android.intent.action.MAIN:"

# Use correct activity name
adb shell am start -n com.github.zly2006.zhplus.lite/com.github.zly2006.zhihu.MainActivity
```

## Useful ADB Commands

### App Management
```bash
# List installed packages
adb shell pm list packages | grep zhplus

# Get app installation path
adb shell pm path com.github.zly2006.zhplus.lite

# Clear app data
adb shell pm clear com.github.zly2006.zhplus.lite

# Uninstall app
adb uninstall com.github.zly2006.zhplus.lite

# Force stop app
adb shell am force-stop com.github.zly2006.zhplus.lite

# Get app info
adb shell dumpsys package com.github.zly2006.zhplus.lite
```

### Device Information
```bash
# Get device properties
adb shell getprop ro.build.version.release  # Android version
adb shell getprop ro.product.model          # Device model
adb shell getprop ro.product.manufacturer   # Manufacturer

# Check storage space
adb shell df -h

# Get device battery status
adb shell dumpsys battery

# Take screenshot
adb exec-out screencap -p > screenshot.png
```

### Debugging
```bash
# View real-time logs
adb logcat

# Filter logs by tag
adb logcat -s TAG_NAME

# Filter logs by package
adb logcat --pid=$(adb shell pidof -s com.github.zly2006.zhplus.lite)

# Save logs to file
adb logcat -d > logcat.txt

# Clear log buffer
adb logcat -c
```

## Complete One-Line Command

For quick deployment after code changes:

```bash
./gradlew assembleLiteDebug && adb install -r ./app/build/outputs/apk/lite/debug/app-lite-debug.apk && adb shell am start -n com.github.zly2006.zhplus.lite/com.github.zly2006.zhihu.MainActivity
```

## Advanced: Automatic Build and Deploy Script

Create a script `deploy.sh`:

```bash
#!/bin/bash

set -e  # Exit on error

echo "🔨 Building lite debug APK..."
./gradlew assembleLiteDebug --quiet

APK_PATH="./app/build/outputs/apk/lite/debug/app-lite-debug.apk"
PACKAGE_NAME="com.github.zly2006.zhplus.lite"
ACTIVITY="com.github.zly2006.zhihu.MainActivity"

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
```

Make it executable:
```bash
chmod +x deploy.sh
./deploy.sh
```

## Troubleshooting Checklist

When things don't work, check these in order:

1. ✅ Is ADB installed? → `which adb`
2. ✅ Is ADB server running? → `adb devices`
3. ✅ Is device connected? → Should show `device` status
4. ✅ Is USB debugging enabled on device?
5. ✅ Is USB debugging authorized? → Accept prompt on device
6. ✅ Is APK built? → Check `./app/build/outputs/apk/lite/debug/`
7. ✅ Is package name correct? → `com.github.zly2006.zhplus.lite`
8. ✅ Is activity name correct? → `com.github.zly2006.zhihu.MainActivity`
9. ✅ Check logs for errors → `adb logcat`

## References

- [ADB Documentation](https://developer.android.com/tools/adb)
- [Android Build Types](https://developer.android.com/studio/build/build-variants)
- Project build config: `./app/build.gradle.kts`
- App manifest: `./app/src/main/AndroidManifest.xml`

---

**Last Updated:** 2026-02-03  
**Tested with:** ADB 1.0.41, Android SDK Platform Tools 36.0.2
