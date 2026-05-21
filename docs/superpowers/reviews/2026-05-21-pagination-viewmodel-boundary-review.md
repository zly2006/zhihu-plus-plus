# PaginationViewModel KMP Boundary Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/PaginationViewModel.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/*.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/comment/*.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/za/*.kt`
- Related `AccountData.fetchGet`, `signFetchRequest`, login-expired handling, clipboard/dialog/Toast, and preferences call sites.

Subagent: `019e48fb-91ed-7193-9cf2-0779fe7d2a0f` (`gpt-5.5`, `xhigh`) completed before implementation.

## Conclusion

`PaginationViewModel` should move to `shared/commonMain`. Its paging state, `refresh`/`loadMore`/`fetchFeeds` flow, `processResponse`, raw JSON capture, JSON decoding, and `ZhihuPaging` handling are shared semantics. Android owns only the current runtime environment: `Context`, `MainActivity.httpClient`, `LoginActivity` intent, `AlertDialog`, `Toast`, clipboard, debug curl UI, `BuildConfig.DEBUG`, `context.mainExecutor`, lifecycle-safe dialog checks, and `SharedPreferences`.

Do not replace `PaginationViewModel` with a loader. The migration path is to keep the class body and split its side effects into small adapters before `git mv`.

## Boundaries

Shared:

- paging state: `allData`, `debugData`, `isLoading`, `errorMessage`, `lastPaging`;
- request paging flow: initial URL, next URL, include parameters, load/refresh/end state;
- JSON decode with `ZhihuJson`;
- HTTP status error model and `raiseForStatus`;
- reusable signing via `signZhihuFetchRequest`;
- feed/comment/list state transitions that do not require Android runtime APIs.

Platform adapter:

- Android `Context` and Activity lifecycle;
- `MainActivity.httpClient` and Android `AccountData.httpClient(context)` lookup;
- login-expired UI and `LoginActivity` launch;
- Toast, AlertDialog, clipboard, debug curl copy;
- preference source for guest access/login settings;
- history repository access currently reached by casting `MainActivity`.

Needs splitting:

- `BaseFeedViewModel` display mapping should move toward shared, while `ContentDetailCache`, `BlocklistManager`, and Toast stay adapters.
- `HomeFeedViewModel` filtering/read semantics are shared, but current calls still depend on `ContentFilterExtensions`, signed requests, and Android preferences.
- Comment ViewModels should share state and request construction; Android `htmlEncode` must be replaced with a common helper.
- `SearchViewModel` should replace `java.net.URLEncoder` with Ktor URL parameters.
- `HistoryViewModel` and `OnlineHistoryViewModel` need an injected history repository instead of `MainActivity` casts.

## Dependencies

Common-ready dependencies already exist in `shared`: Compose runtime state, JetBrains Lifecycle/ViewModel, Ktor core/content negotiation, kotlinx serialization, KMP Room, `ZhihuJson`, `SettingsStore`, `UserMessageSink`, shared `Log`, and `signZhihuFetchRequest`.

Android-only dependencies must remain in platform source sets: `androidx.activity`, `androidx.core`, Android dialogs, Toast, clipboard, WebView/browser, and Android Ktor engine/runtime lookup.

## Minimum Migration

1. Move or recreate common HTTP error helpers (`HttpStatusException`, `raiseForStatus`) outside Android UI files.
2. Define a common pagination environment that supplies HTTP fetching/signing, settings, messages, login-expired action, debug error action, and logging.
3. Change `PaginationViewModel` to depend on that environment instead of `Context`/`AccountData`.
4. `git mv` `PaginationViewModel.kt` into `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/`, preserving the existing class body.
5. Migrate subclasses in batches: first feed base and simple feed/list subclasses, then comment, then za/home/filter-heavy classes.

## Verification

```bash
rg -n "android\\.|Context|MainActivity|LoginActivity|Toast|AlertDialog|clipboard|SharedPreferences|PREFERENCE_NAME|signFetchRequest|AccountData" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
rg -n "ZhihuPageLoader" .
./gradlew :shared:jvmTest :desktopApp:compileKotlin :app:compileLiteDebugKotlin :app:testLiteDebugUnitTest
```
