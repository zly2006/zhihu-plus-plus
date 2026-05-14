# Launch on Device - Quick Reference

This skill provides tools and documentation for deploying the Zhihu++ Android app to physical devices or emulators.

## Files

- **SKILL.md** - Complete documentation with troubleshooting guide
- **deploy.sh** - Automated deployment script

## Quick Usage

### Option 1: Use the Deploy Script (Recommended)

```bash
./github/skills/launch-on-device/deploy.sh
```

This script will:
1. Build the lite debug APK
2. Check device connection
3. Install the APK (handling conflicts automatically)
4. Launch the app

### Option 2: Manual Commands

```bash
# Build
./gradlew assembleLiteDebug

# Install and launch
adb install -r ./app/build/outputs/apk/lite/debug/app-lite-debug.apk
adb shell am start -n com.github.zly2006.zhplus.lite/com.github.zly2006.zhihu.MainActivity
```

## Common Issues

- **No device found**: Check USB connection and enable USB debugging
- **Installation failed**: Run `adb uninstall com.github.zly2006.zhplus.lite` first
- **App crashes**: Check logs with `adb logcat`

See SKILL.md for comprehensive troubleshooting guide.
