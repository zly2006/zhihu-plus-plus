# SettingsStore Float Capability Review

Date: 2026-05-21

## Input

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform/SettingsStore.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/shared/platform/SettingsStore.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/platform/SettingsStore.jvm.kt`
- current Android-only filter reads in `ContentFilterExtensions.getNLPSimilarityThreshold`

## Conclusion

`nlpSimilarityThreshold` is a cross-platform filter setting, not an Android-only value. `SettingsStore` already abstracts Boolean, String, and Int settings for shared UI, but it lacked Float support. That omission forced `ContentFilterExtensions` and NLP settings to keep reading Android `SharedPreferences` directly.

## Implementation

- Added `getFloat` and `putFloat` to shared `SettingsStore`.
- Android implementation delegates to `SharedPreferences.getFloat` / `putFloat`.
- JVM implementation stores Float values in the existing properties file.

## Remaining Work

This slice only adds the missing primitive capability. The next filter migration slice should build a shared filter-settings snapshot and move `enableNLPBlocking`, `nlpSimilarityThreshold`, and related keys out of direct Android preference reads.

## Verification

```bash
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabaseTest
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
git diff --check
```
