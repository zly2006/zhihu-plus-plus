# KMP Migration Status

## Completed

- Project has `shared`, `desktopApp`, Android `app`, and Android-only `sentence_embeddings`.
- `desktopApp` is a shallow demo1-style launcher.
- QR login core and QR UI are shared via `SharedQrLoginPane`.
- Android login uses shared QR login; Android WebView stays in app.
- JVM login uses shared QR login and stores cookies under `~/.zhihu-plus-plus/account.json`.
- KMP Room is used for content filter and local content databases.
- 共享导航语义应由 `shared/commonMain` 拥有；`NavDestination`、`LocalNavigator.kt`、`AnswerNavigator.kt` 已迁回 shared。`AnswerNavigator` 的 Android 数据访问通过 `AndroidAnswerNavigatorRepository` 留在 app 适配层。
- `ZhihuMain.kt` 主导航壳已通过 `git mv` 迁入 `shared/commonMain`；但 route 注册仍需按 `master` 的大函数形状收回 shared。Android 只应保留具体页面实现、`MainActivity`、偏好读取、ViewModel 创建等运行时副作用 adapter。
- `ThemeManager` / `ZhihuTheme` 的主题状态和 Material3 主题壳已迁入 `shared/commonMain`；Android 持久化、system dark、dynamic color 和 system bar 副作用留在 `shared/androidMain` adapter。
- Bottom navigation preference keys and normalization rules are shared in `shared/commonMain`; Android preference screens and `ZhihuMain` adapters reuse that common rule set.
- Account session data and JSON persistence rules have a shared repository; Android and JVM desktop storage are thin file-path adapters over that repository.
- Feed display mapping is shared via `Feed.toDisplayItem`; Android feed view models only pass platform preferences into the shared mapper.
- Generic paging state uses shared `ZhihuPaging`; `PaginationViewModel` is still Android-side only because signed fetch, login-expired handling, Toast/Dialog/clipboard, Activity navigation, preferences, and lifecycle effects have not yet been split into small platform adapters. The target is to move `PaginationViewModel` itself to `shared/commonMain`, not to replace it with a separate `ZhihuPageLoader`.
- `ContentFilterManager` and `ContentFilterExtensions` are core filtering features and must move to `shared/commonMain`; current Android `Context`/preferences/Room/log/Toast dependencies are adapter seams, not ownership. Do not replace or bypass the filtering logic while migrating settings pages or feed view models.
- `DailyScreen`、`CollectionScreen`、`NotificationScreen`、`OpenSourceLicensesScreen` 页面主体已迁入 `shared/commonMain`；Android 仅保留必要 data/provider/runtime adapter。`SentenceSimilarityTestScreen` 不是普通 shared 页面，full/lite 变体实现必须留在 Android app variant。
- Shared has feed data models, notification/daily/hot-list/read-history clients, display formatting, ZSE signing, and local recommendation scoring helpers.

## Do Not Redo

- Do not keep navigation semantics or the main navigation shell Android-only. Move shared route/destination semantics plus `ZhihuMain.kt`, `LocalNavigator.kt`, and `AnswerNavigator.kt` toward `shared/commonMain`; keep only Android runtime side effects (`Context`, `Intent`, WebView, APK/update/install semantics, platform-only callbacks) in app.
- Do not move `ZhihuMain` route registration into an Android-only helper such as `androidZhihuMainRouteContent`. Match `master`: the shared `ZhihuMain` big function owns `NavHost` and all `composable<...>` route registrations; platforms inject page implementations and runtime side effects only.
- Use `org.jetbrains.androidx.navigation:navigation-compose` as the preferred KMP navigation runtime. The current Android module already depends on `org.jetbrains.androidx.navigation:navigation-compose:2.9.2`; continue by moving that dependency to shared/commonMain and validating JVM/desktop compilation before introducing any custom route adapter.
- Do not assume desktop needs a separate route model. Desktop should reuse the shared Android UI/navigation semantics for this migration; only introduce a thin runtime adapter if the current Navigation Compose dependency cannot compile for JVM/desktop.
- Do not recreate `Time.android.kt` / `Time.jvm.kt`; use `Clock.System`.
- Do not put APK/lite/full/update/install semantics into `shared`.
- Do not make `desktopApp` contain QR login, cookie persistence, networking, or main UI state.
- Do not recreate the removed `.codex/hooks.json`; that deletion is intentional for this worktree.
- Do not introduce `ZhihuPageLoader` or another one-off loader layer as a substitute for migrating `PaginationViewModel`. Split side effects into small cross-platform interfaces/adapters and move the ViewModel.
- Do not move theme mode/custom color/background color state back to app just because Android currently owns persistence or dynamic color. Only platform environment and side effects stay in adapters.
- Do not treat `ContentFilterManager` or `ContentFilterExtensions` as Android-only just because they currently read Android preferences, Room builders, or `Context`; move the filtering orchestration and strategy to shared and keep only platform access in adapters.

## Remaining Work

- Continue shrinking the Android `ZhihuMain` adapter only where it removes real platform side effects; do not rewrite the shared main shell.
- Split account fetch/token refresh orchestration from Android `AccountData`.
- Move `PaginationViewModel` into shared after splitting Android side effects into small cross-platform interfaces/adapters.
- Move `ContentFilterManager` and `ContentFilterExtensions` into shared after splitting preferences, database builder, logging/toast, and content-detail fetch side effects into adapters.
- Add a shared hot-list/home shell that Android and desktop can both invoke.
- Move local recommendation orchestration behind platform adapters.
- Move pure HTML/text parsing only after replacing Jsoup with Ksoup-compatible shared code.
- Run Android AVD and JVM QR login runtime validation before final completion.
