# Notification Screen Data Provider Boundary Review

Date: 2026-05-27
Reviewer: gpt-5.5 xhigh subagent `019e65fb-e4e8-71c2-8264-49a2d528e6a7`

## Scope

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreen.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreenData.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreenData.jvm.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/NotificationViewModel.kt`
- Android/JVM `NotificationPaginationEnvironment` implementations
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/notification/*`
- Master `NotificationScreen` and `NotificationViewModel`

## Conclusion

`NotificationScreenData` is split ownership. The screen state and action wiring are shared, while platform files should only provide runtime side effects.

Shared ownership:

- `NotificationScreenData` construction.
- `NotificationViewModel` access, notification filtering, refresh/load-more actions, mark-all-as-read action, debug data JSON encoding, and user-facing messages.
- The existing `NotificationScreen()` UI structure and navigation behavior.

Platform ownership:

- Android `Context`, `ApplicationInfo.FLAG_DEBUGGABLE`, clipboard write, and Android notification pagination environment.
- JVM `DesktopAccountStore`, `HttpClient` lifecycle, AWT clipboard write, signing cookie source, and JVM notification pagination environment.

Directly moving the Android/JVM actual files to common would incorrectly move platform side effects. The safe migration path is to keep a smaller platform runtime and move only the repeated provider wiring to common.

## Required Compatibility

Preserve these key names and flows:

- `NotificationScreen`, `NotificationItemView`, `buildNotificationText`, and `NotificationDebugCopyButton`.
- Initial refresh effect, pull-to-refresh, load-more trigger, mark-all-as-read message, and debug-copy message.
- Item click still marks the notification read before target navigation.
- Shared UI must continue using `LocalNavigator`; do not add a one-off navigation lambda.

This slice must not change list UI, top app bar UI, notification target handling, visible text, or settings navigation.

## Minimum Implementation

1. Replace `expect fun rememberNotificationScreenData()` with a common implementation.
2. Add a small `NotificationScreenRuntime` platform adapter with `NotificationPaginationEnvironment`, debug visibility, and clipboard writing.
3. Keep Android environment creation in Android source set.
4. Keep JVM `DesktopAccountStore`, signed request environment, `HttpClient` disposal, and AWT clipboard in JVM source set.
5. Delete the old duplicated Android/JVM `rememberNotificationScreenData()` actual implementations.

## Validation

```bash
rg -n "android\.|Context|LocalContext|ApplicationInfo|ClipData|Toast|java\.awt|DesktopAccountStore" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreen.kt shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/NotificationViewModel.kt
rg -n "expect fun rememberNotificationScreenData|actual fun rememberNotificationScreenData|NotificationScreenData\(" shared/src -g '*.kt'
rg -n "Scaffold|TopAppBar|LazyColumn|NotificationItemView|NotificationScreen\(" shared/src/androidMain/kotlin shared/src/jvmMain/kotlin -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
