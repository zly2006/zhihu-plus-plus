# HotList Common UI Boundary Review

Date: 2026-05-22

Reviewer: gpt-5.5 xhigh subagent `Galileo`

## Inputs

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/HotListScreen.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/HotListViewModel.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PlatformScreenEntrypoints.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PlatformScreenEntrypoints.jvm.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt`

Expected target: move the HotList screen body to `shared/commonMain` so Android and desktop reuse the same UI, while keeping Android `Context`, block-list writes, platform storage, and other runtime side effects behind small adapters.

## Verdict

`HotListScreen` is split ownership: the visible UI structure, list, refresh button, test tags, `FeedCard` composition, and hot-list thumbnail mapping belong in `shared/commonMain`; the current `LocalContext`, `refresh/loadMore(context)`, and block-user dialog side effects are platform adapter concerns.

`HotListViewModel` is also split ownership: the hot-list URL, guest access, pagination state, feed display mapping, and author/avatar clearing belong in shared. It cannot be moved as a single file until the shared portion of `BaseFeedViewModel` is available without Android `Context`.

`BlockUserConfirmDialog` is split ownership: Material dialog UI can be common, while block-list persistence and user messages need platform/shared service adapters. HotList currently does not pass block-user callbacks into `FeedCard`, so this dialog path is not user-triggerable in the current HotList screen and should not be newly activated during migration.

No HotList UI behavior is JVM-only. Android-only concerns are `Context`, `Intent`, Toast, SharedPreferences access, Android block-list facades, and WebView-related capabilities. HotList itself has no WebView dependency.

## Dependencies

Common-capable dependencies already available in `shared/commonMain` include Compose foundation/material3/runtime/ui, material icons, Navigation Compose, Lifecycle ViewModel Compose, Ktor, serialization, Room KMP, and the hot-list data/client models.

Dependencies that must remain platform-side or be replaced are Android `Context`, `Intent`, `Uri`, Toast, Android preference APIs, Android resources referenced by nested feed UI, and Jsoup-backed HTML parsing. Common replacements should prefer existing `SettingsStore`, `UserMessageSink`, `LocalUriHandler`, variant capability adapters, and Ksoup/common parsing.

## Minimal Plan

1. Split the common state/algorithm portion of `BaseFeedViewModel` away from Android `Context` and preference reads.
2. Move `HotListViewModel` to common after it can use shared pagination/display settings.
3. Move simple list/refresh components to common, changing pull-to-refresh to accept common refresh callbacks or common pagination environment rather than taking `LocalContext`.
4. Replace or defer platform-sensitive `FeedCard` and block-user side effects without changing current HotList visible behavior.
5. Move `HotListScreen` to common and remove the HotList page-level `expect/actual` entrypoint.

## Verification

Required after implementation:

```bash
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin
./gradlew :shared:jvmTest
rg -n "android\\.|Context|Intent|WebView|Toast|LocalContext|SharedPreferences|MODE_PRIVATE|androidx\\.webkit" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed -g '*.kt'
rg -n "expect fun HotListScreen|actual fun HotListScreen|UnsupportedDesktopScreen" shared/src -g '*.kt'
rg -n "WebView|android\\.webkit|androidx\\.webkit" shared/src/commonMain/kotlin shared/src/jvmMain/kotlin desktopApp/src
```

AVD instrumentation tests for `HotListScreen` should be rerun when this slice reaches Android runtime validation, because existing tests seed the activity-scoped `HotListViewModel`.

## Risks

- `FeedCard` contains deeper platform and parsing concerns than the screen itself.
- Moving the ViewModel can affect instrumentation tests if the `viewModel()` owner changes.
- Block user behavior must not be accidentally activated or changed while removing the dead HotList dialog path.
- Desktop must remain WebView-free.
