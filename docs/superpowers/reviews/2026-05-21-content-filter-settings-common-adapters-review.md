# Content Filter Settings Common Adapters Boundary Review

Date: 2026-05-21

## Input

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/ContentFilterSettingsScreen.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform/SettingsStore.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform/UserMessageSink.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/filter/ContentFilterMaintenance.kt`
- Android/JVM actual implementations for the three adapters.
- Low-risk reuse sites: `NotificationScreenData`, `HotListScreen`, `SearchScreen`.

## Conclusion

`ContentFilterSettingsScreen` belongs in `shared/commonMain`. Its UI state, setting keys, labels, validation, route navigation, and stats presentation are shared UI semantics. Android-only work has been reduced to small adapter capabilities:

- `SettingsStore`: preference read/write.
- `UserMessageSink`: short/long user-visible messages.
- `ContentFilterMaintenance`: content-filter stats, cleanup, reset.

These names are now generic enough for reuse outside the settings page. They should remain shared API surfaces while their actual storage, notification, and database builders stay platform-specific.

## Evidence

- Boundary grep for `ContentFilterSettingsScreen.kt` and the common adapter packages shows no Android runtime imports, `LocalContext`, `Context`, `SharedPreferences`, `Toast`, `PREFERENCE_NAME`, `MainActivity`, `AccountData`, or content-detail fetch calls.
- `expect fun ContentFilterSettingsScreen` and `actual fun ContentFilterSettingsScreen` are gone; the settings page body is no longer whole-page `expect/actual`.
- `NotificationScreenData.android.kt`, `HotListScreen.kt`, and `SearchScreen.kt` now use `UserMessageSink` instead of direct low-risk `Toast.makeText` calls; `HotListScreen.kt` and `SearchScreen.kt` also use `SettingsStore` for low-risk settings reads touched by this slice.
- `:shared:compileKotlinJvm` and `:desktopApp:compileKotlin` pass after the adapter split.

## Platform Boundaries

Shared:

- Setting key access contract and typed `Boolean`/`String`/`Int` operations.
- User-message intent and short/long duration semantics.
- Content-filter maintenance operation contract and stats data shape.
- `ContentFilterSettingsScreen` UI body.

Platform:

- Android `SharedPreferences` and JVM file-backed settings storage.
- Android `Toast` and JVM no-op message sink.
- Android/JVM Room database access through platform database builders; shared `ContentFilterMaintenance` owns the stats/cleanup/reset rules over the common DAO.

## Risks

- `ContentFilterMaintenance` now centralizes stats/cleanup/reset logic in common code and takes a shared `ContentFilterDao`; Android and JVM actuals only provide the platform database instance. This removes the duplicate Android DAO rules that the subagent flagged.
- `SettingsStore` is generic but currently only supports primitive preference types needed by migrated UI. Add operations only when a real shared caller needs them.
- Many direct Android `Toast.makeText` and `getSharedPreferences` calls remain in Android-only pages and ViewModels. They should be replaced opportunistically when those files are migrated or when replacement is low-risk within the current slice.

## Reuse Decision

Low-risk reuse completed:

- Notification screen messages.
- Hot list error messages.
- Search error messages.
- Hot list refresh-FAB setting.
- Search hot-search and refresh-FAB settings.

Deferred reuse:

- `HomeScreen`, `FollowScreen`, `ArticleScreen`, `AppearanceSettingsScreen`, blocklist pages, and feed/comment ViewModels still contain broader Android-only state, Activity/WebView/variant dependencies, or larger migration debt. Replacing all calls there in this slice would expand the blast radius without moving the main KMP boundary materially.

## Verification

Passed:

```bash
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet :shared:compileKotlinJvm :desktopApp:compileKotlin
./gradlew :shared:jvmTest
git diff --check
```

Expected failure:

```bash
./gradlew :shared:compileAndroidMain
```

This still fails on pre-existing migration debt in `AccountSettingScreen`, `ArticleScreen`, feed ViewModels, app-only `BuildConfig`/`MainActivity`/`R`, and `PaginationViewModel` boundaries. No failure points to the new `shared.platform` or `shared.filter` adapter files.

## Subagent

An independent `gpt-5.5` / `xhigh` boundary review was requested for this slice and completed before final decision. The subagent rejected committing the slice as-is. Follow-up status:

- Fixed: `ContentFilterMaintenance` stats/cleanup/reset logic moved to common; platform actuals only provide DAO/database access.
- Fixed: JVM `ContentFilterMaintenance` now uses the existing KMP Room database instead of no-op actions.
- Fixed: JVM `SettingsStore` persists to `~/.zhihu-plus/settings.properties` instead of a composition-local memory map.
- Fixed: `SettingsStore` is reused for low-risk preference reads in already-touched `HotListScreen.kt` and `SearchScreen.kt`.

Process rule captured after this review: once a subagent is started, the main agent must wait for all still-live subagents to complete, or explicitly close them and record why, before committing or declaring the slice complete.
