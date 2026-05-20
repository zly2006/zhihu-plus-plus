# KMP Migration Status

## Completed

- Project has `shared`, `desktopApp`, Android `app`, and Android-only `sentence_embeddings`.
- `desktopApp` is a shallow demo1-style launcher.
- QR login core and QR UI are shared via `SharedQrLoginPane`.
- Android login uses shared QR login; Android WebView stays in app.
- JVM login uses shared QR login and stores cookies under `~/.zhihu-plus-plus/account.json`.
- KMP Room is used for content filter and local content databases.
- 共享导航语义应由 `shared/commonMain` 拥有；`NavDestination`、`LocalNavigator.kt`、`AnswerNavigator.kt` 已迁回 shared。`AnswerNavigator` 的 Android 数据访问通过 `AndroidAnswerNavigatorRepository` 留在 app 适配层。
- Bottom navigation preference keys and normalization rules are shared in `shared/commonMain`; Android preference screens and `ZhihuMain` adapters reuse that common rule set.
- Account session data and JSON persistence rules have a shared repository; JVM desktop storage is now a thin file-path adapter over that repository.
- Shared has feed data models, notification/daily/hot-list/read-history clients, display formatting, ZSE signing, and local recommendation scoring helpers.

## Do Not Redo

- Do not keep navigation semantics or the main navigation shell Android-only. Move shared route/destination semantics plus `ZhihuMain.kt`, `LocalNavigator.kt`, and `AnswerNavigator.kt` toward `shared/commonMain`; keep only Android runtime side effects (`Context`, `Intent`, WebView, APK/update/install semantics, platform-only callbacks) in app.
- Use `org.jetbrains.androidx.navigation:navigation-compose` as the preferred KMP navigation runtime. The current Android module already depends on `org.jetbrains.androidx.navigation:navigation-compose:2.9.2`; continue by moving that dependency to shared/commonMain and validating JVM/desktop compilation before introducing any custom route adapter.
- Do not assume desktop needs a separate route model. Desktop should reuse the shared Android UI/navigation semantics for this migration; only introduce a thin runtime adapter if the current Navigation Compose dependency cannot compile for JVM/desktop.
- Do not recreate `Time.android.kt` / `Time.jvm.kt`; use `Clock.System`.
- Do not put APK/lite/full/update/install semantics into `shared`.
- Do not make `desktopApp` contain QR login, cookie persistence, networking, or main UI state.

## Remaining Work

- Move `ZhihuMain.kt` back to common code after separating Android-only runtime effects.
- Finish wiring Android `AccountData` onto the shared account session repository while preserving its current public API.
- Move pagination and feed loading state into shared with platform effect adapters.
- Replace Android duplicate feed display mapping with shared `Feed.toDisplayItem`.
- Add a shared hot-list/home shell that Android and desktop can both invoke.
- Move local recommendation orchestration behind platform adapters.
- Move pure HTML/text parsing only after replacing Jsoup with Ksoup-compatible shared code.
- Run Android AVD and JVM QR login runtime validation before final completion.
