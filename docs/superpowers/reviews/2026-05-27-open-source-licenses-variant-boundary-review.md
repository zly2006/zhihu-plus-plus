# Open Source Licenses Variant Boundary Review

Date: 2026-05-27
Reviewer: gpt-5.5 xhigh subagent `019e6607-87a7-7600-8cf5-41a8a225d9e0`

## Scope

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/OpenSourceLicensesScreen.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/OpenSourceLicensesRuntime.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/OpenSourceLicensesRuntime.jvm.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform/VariantCapabilities.kt`
- Android/JVM `VariantCapabilities` actual files

## Conclusion

Do not replace common `rememberShowFullVariantLicenses()` usage with `!rememberIsLiteVariant()` as a behavior-preserving migration.

The Android implementation is equivalent because both check whether the package is the lite variant. The JVM implementation is not equivalent: it currently returns `false`, while `!rememberIsLiteVariant()` would return `true` on desktop and display Android full-flavor manual license entries for `sentence_embeddings` and Rust/JNI components that desktop does not include.

## Ownership

Shared ownership:

- `OpenSourceLicensesScreen()` UI structure.
- `OpenSourceLicensesContent()`, `ManualLicenseEntryGroup()`, and `fullVariantManualLibraries` data.
- The display structure for manual license entries.

Platform ownership:

- `rememberOpenSourceLicensesLibraries()`, because Android loads aboutlibraries with `LocalContext` and JVM loads resources/file fallbacks.
- The decision to show Android full-flavor manual license entries, because desktop intentionally does not include those Android full dependencies.

## Required Compatibility

Keep these names and structure unchanged:

- `OpenSourceLicensesScreen`
- `OpenSourceLicensesContent`
- `rememberOpenSourceLicensesLibraries`
- `ManualLicenseEntryGroup`
- `fullVariantManualLibraries`

Keep the existing call order: navigator, URI handler, scroll behavior, manual library decision, `Scaffold`, `LargeTopAppBar`, `OpenSourceLicensesContent`, `LibrariesContainer`, then optional manual license header.

## Decision

Keep the current small `rememberShowFullVariantLicenses()` platform capability because it is more accurate than reusing `rememberIsLiteVariant()` in common and changing desktop behavior.

Follow-up implementation: the Android actual may delegate to `!rememberIsLiteVariant()` because the Android implementation is equivalent to the previous package-name check. The JVM actual must stay `false` so desktop does not display Android full-flavor manual license entries.

## Validation If Revisited

```bash
rg -n "rememberShowFullVariantLicenses|rememberIsLiteVariant|fullVariantManualLibraries" shared/src -g '*.kt'
rg -n "LocalContext|withContext|java.io.File|Thread.currentThread|packageName|IS_LITE|BuildConfig" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
git diff --check
```
