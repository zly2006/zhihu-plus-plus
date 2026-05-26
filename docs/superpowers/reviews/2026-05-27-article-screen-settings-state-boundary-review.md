# Article Screen Settings State Boundary Review

Date: 2026-05-27
Reviewer: gpt-5.5 xhigh subagent `019e65ee-41a6-7391-8317-be98d69790c6`

## Scope

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreenSettingsState.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreenSettingsState.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreenSettingsState.jvm.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform/SettingsStore.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/shared/platform/SettingsStore.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/platform/SettingsStore.jvm.kt`
- Callers in `ArticleScreen.kt` and `AppearanceSettingsScreen.kt`
- Master `ArticleScreen.kt` preference reads and `OnSharedPreferenceChangeListener` behavior

## Conclusion

`ArticleScreenSettingsState` is split ownership.

Shared ownership:

- Setting keys, default values, Compose state fields, and `saveAnswerDoubleTapAction()`.
- The read order and key update behavior copied from master.
- The `DisposableEffect` lifecycle that subscribes and unsubscribes to key changes.

Platform ownership:

- Android `SharedPreferences` persistence and listener registration.
- JVM desktop properties-file persistence. JVM currently has no external setting-change notifications, so a no-op observer preserves current behavior.

The safe migration path is to add optional key observation to `SettingsStore`, then make `rememberArticleScreenSettingsState()` common. Directly moving the JVM actual to common would drop Android's master listener behavior and is not acceptable.

## Required Compatibility

The common provider must preserve these key/default pairs:

- `titleAutoHide = false`
- `autoHideArticleBottomBar = false`
- `answerSwitchMode = "vertical"`
- `pinAnswerDate = false`
- `duo3_article_actions = false`
- `buttonSkipAnswer = true`
- `autoHideSkipAnswerButton = true`
- `answerDoubleTapAction = AnswerDoubleTapAction.Ask`
- `webviewRender = false`

It must preserve function names `ArticleScreenSettingsState`, `rememberArticleScreenSettingsState`, and `saveAnswerDoubleTapAction`.

## Minimum Implementation

1. Add a no-op-by-default observer capability to `SettingsStore`.
2. Implement Android observation with `SharedPreferences.OnSharedPreferenceChangeListener`.
3. Keep JVM observation no-op.
4. Convert `rememberArticleScreenSettingsState()` from `expect` to a common `@Composable`.
5. Delete Android/JVM actual files.

Do not change `ArticleScreen` UI, article settings UI, answer switch UI, WebView/Markdown behavior, or the preference keys.

## Validation

```bash
rg -n "expect fun rememberArticleScreenSettingsState|actual fun rememberArticleScreenSettingsState" shared/src
rg -n "SharedPreferences|LocalContext|Context|getSharedPreferences" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreenSettingsState.kt shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform/SettingsStore.kt
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
