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
- `PaginationViewModel.kt` 本体已通过 `git mv` 迁入 `shared/commonMain`，当前 common API 依赖 `PaginationEnvironment` 而不是 Android `Context`。Android 侧仍有临时 `Context -> PaginationEnvironment` 薄适配，feed/comment/list 子类还在 Android source set；下一步继续把子类的状态和纯分页流程迁入 shared，并把登录过期 Dialog、Toast/clipboard、Activity 导航、偏好读取、history repository 等副作用压缩到平台 adapter。不得用 `ZhihuPageLoader` 替代本体迁移。
- `ContentFilterManager` is in `shared/commonMain`. `ContentFilterExtensions` has moved from app into `shared/androidMain` as the current Android wrapper over shared filter pipelines; its remaining Android `Context`/preferences/Room/log/Toast/detail-fetch dependencies are adapter seams, not ownership. Continue moving the orchestration and strategy into `shared/commonMain` instead of replacing or bypassing filtering logic.
- `ContentFilterSettingsScreen` should not use a mixed page adapter for all platform work. Its common bridge is split into generic `SettingsStore`, `ContentFilterMaintenance`, and `UserMessageSink` capabilities; filter maintenance owns one shared KMP Room-backed implementation instead of copying Android DAO logic in platform actuals.
- Newly extracted generic adapters must be reused in the same migration slice where reasonable. Grep for duplicate `Toast.makeText`, `getSharedPreferences`, and filter maintenance logic; replace low-risk call sites with `UserMessageSink`, `SettingsStore`, and `ContentFilterMaintenance`, or record why a remaining Android-only call site is deliberately left alone.
- `DailyScreen`、`CollectionScreen`、`NotificationScreen`、`OpenSourceLicensesScreen` 页面主体已迁入 `shared/commonMain`；Android 仅保留必要 data/provider/runtime adapter。`SentenceSimilarityTestScreen` 不是普通 shared 页面，full/lite 变体实现必须留在 Android app variant。
- Shared has feed data models, notification/daily/hot-list/read-history clients, display formatting, ZSE signing, and local recommendation scoring helpers.

## Do Not Redo

- Do not leave spawned subagents alive while continuing to final decisions. If a subagent is started, wait for every live subagent to complete; waiting is the only default path. Do not bypass waiting because local judgment seems sufficient, the wait is long, or the pending result seems unlikely to matter. Before committing, declaring completion, stopping the turn, or sending a final response, explicitly account for all subagents started in the turn: every live one must be completed via `wait_agent`. If a subagent times out but is still alive, keep waiting, provide more input, or adjust the task. Closing is not the normal escape hatch; only close one when the user explicitly cancels it, the task is obsolete, or a written note explains why its result can no longer affect current work. Before that, do not commit, declare a slice complete, treat the local decision as final, or continue work that depends on the pending conclusion.
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
- Do not grow a page-specific content-filter-settings aggregate back into a mixed adapter. Keep `SettingsStore`, `ContentFilterMaintenance`, and `UserMessageSink` separate and generically named; page-level adapters must not become a replacement for the shared filtering manager.
- Do not leave newly extracted generic adapters unused outside their first page when adjacent low-risk duplicates exist.

## Remaining Work

- Continue shrinking the Android `ZhihuMain` adapter only where it removes real platform side effects; do not rewrite the shared main shell.
- Split account fetch/token refresh orchestration from Android `AccountData`.
- Continue migrating `PaginationViewModel` subclasses and Android-only pagination call sites into shared after splitting their remaining platform side effects into small adapters.
- Move `ContentFilterManager` and `ContentFilterExtensions` into shared after splitting preferences, database builder, logging/toast, and content-detail fetch side effects into adapters.
- Add a shared hot-list/home shell that Android and desktop can both invoke.
- Move local recommendation orchestration behind platform adapters.
- Move pure HTML/text parsing only after replacing Jsoup with Ksoup-compatible shared code.
- Run Android AVD and JVM QR login runtime validation before final completion.
