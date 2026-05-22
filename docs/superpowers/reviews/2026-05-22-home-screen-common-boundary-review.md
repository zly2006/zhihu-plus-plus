# HomeScreen Common Migration Boundary Review

## Input

- Target: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreen.kt`
- Expected destination: `shared/src/commonMain`
- Review agent: `019e4d8b-3b13-7013-8691-90ea9ba559b5`
- Model: `gpt-5.5`, reasoning effort `xhigh`
- Scope: migrate the Home tab UI body without changing Android UI, keep desktop/JVM free of Android and WebView dependencies.

## Ownership Verdict

- `HomeScreen` UI structure, top app bar, announcements, feed list, refresh behavior, reselect handling and navigation semantics are shared.
- `HomeFeedViewModel` and `HomeFeedInteractionViewModel` are shared and should remain the main shared web-feed path.
- Android/Mixed/Local home feed ViewModels are still split ownership. For this slice they should stay in `androidMain` behind a small ViewModel-selection adapter.
- Account session display and login state are shared semantics, but Android `AccountData`, `LoginActivity`, file paths and token refresh side effects remain platform adapter work.
- Update checking and APK install/download semantics stay Android-only; common UI should receive a small announcement model or no announcement.
- Login launch, QQ URL launch, clipboard copy, debuggable flag, first install time, Toast and local recommendation engine access are platform side effects.
- `fetchZhihuUnreadNotificationCount` is common-safe when called with a shared `PaginationEnvironment` HTTP client and signed-request hook.

## Recommended Minimal Split

- Replace direct `SharedPreferences` reads/writes with `SettingsStore`.
- Replace `Toast.makeText` with `UserMessageSink`.
- Add a small home runtime adapter for account summary/login request, install age, update announcement, external URL opening, debug-data copy, ViewModel selection, and local recommendation feedback/open tracking.
- Reuse `FeedBlockActions` for user/topic blocking.
- Add keyword block action support or a separate minimal keyword-block adapter before moving `BlockByKeywordsDialog` usage into common.
- Do not move `LocalRecommendationEngine`, APK update/install, WebView, Android `Context`, `Intent`, `ClipData`, `ApplicationInfo`, `AccountData`, or `UpdateManager` into common.

## Risk

- Medium-high. The UI body is ready to share, but HomeScreen mixes several side effects. Keep this slice focused on adapters and avoid changing text, layout, announcement visibility rules or recommendation-mode semantics.
- Existing `HomeScreenInstrumentedTest` is important because it seeds a `HomeFeedViewModel`; migration must preserve the common WEB path owner behavior.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
rg -n "android\\.|LocalContext|Context|Intent|Toast|ApplicationInfo|ClipData|AccountData|UpdateManager|signFetchRequest|LoginActivity|luoTianYiUrlLauncher|clipboardManager" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreen.kt
rg -n "actual fun HomeScreen|expect fun HomeScreen|UnsupportedDesktopScreen" shared/src -g '*.kt'
rg -n "WebView|android\\.webkit|androidx\\.webkit" desktopApp/src shared/src/jvmMain shared/src/commonMain -g '*.kt'
```
