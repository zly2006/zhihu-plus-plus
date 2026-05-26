# Update Manager Variant Boundary Review

Date: 2026-05-27

## Input

- Target files:
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/updater/UpdateManager.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/shared/platform/VariantCapabilities.android.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform/VariantCapabilities.kt`
- Goal: decide whether `Context.isLiteVariant()` in `UpdateManager` should reuse the same lite/full variant rule as `rememberIsLiteVariant()`.
- Subagent attempt: spawning the required `gpt-5.5 xhigh` boundary reviewer failed with `agent thread limit reached`, so this document records the local review using the same checklist.

## Conclusion

This is an Android release adapter boundary, not common UI logic. The package-name rule may be shared inside `androidMain`, but it should not be moved to `commonMain`.

## Ownership

- Android-only:
  - `Context.packageName`.
  - APK asset selection in `UpdateManager`.
  - The current lite package suffix convention `.lite`.
- Shared:
  - The abstract Compose capability `rememberIsLiteVariant()`, already exposed as an `expect` for common UI that needs variant state.
  - Pure update release parsing and version comparison that already live in common updater helpers.

## Minimal Implementation

Add a small Android source set helper such as `isAndroidLiteVariantPackageName(packageName: String)` in `VariantCapabilities.android.kt`.

Then:

- `rememberIsLiteVariant()` keeps the same public name and delegates to the helper.
- `UpdateManager` keeps `private fun Context.isLiteVariant()` and delegates to the helper.
- `GithubRelease.extractDownloadInfo()` keeps its current order: filter Android APK assets, select the asset by `context.isLiteVariant()`, fallback to first asset, return `DownloadInfo`.
- Do not change APK download/install behavior, release selection, update settings, or UI.

## Risk

Moving the helper to `commonMain` would make Android package suffix semantics look like a cross-platform release rule. That is not accurate for desktop, which does not have Android APK variants. Keeping the helper in `androidMain` avoids that drift while removing the duplicate suffix check.

## Verification

```bash
rg -n "packageName\\.endsWith\\(\"\\.lite\"\\)|isAndroidLiteVariantPackageName|rememberIsLiteVariant|isLiteVariant\\(" shared/src app/src -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileAndroidMain :app:compileLiteDebugKotlin :shared:runKtlintFormatOverAndroidMainSourceSet --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
