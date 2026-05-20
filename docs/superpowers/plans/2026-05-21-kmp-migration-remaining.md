# KMP Migration Remaining Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the remaining migration from the Android-only Zhihu++ codebase to the current Kotlin Multiplatform shape while preserving Android behavior and making JVM/desktop run through shared logic.

**Architecture:** Keep `desktopApp` as a demo1-style shallow entry point and move reusable state machines, network clients, parsers, display models, navigation semantics, and Compose screens into `shared`. Desktop should reuse the same route model and Android-like UI semantics during this migration; only platform runtime effects (`Context`, `Intent`, WebView, APK variants, file provider, AVD-only validation, JVM file paths, terminal notifications, packaging) stay in platform source sets. If the current Navigation Compose runtime compiles on JVM/desktop, prefer using it in shared; if it does not, keep the shared `NavDestination` model and add the thinnest platform adapter.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor 3.5, Kotlin Serialization, Room KMP, Ksoup, ZXing, Gradle 9.4, ktlint.

---

## Current State Audit

### Completed And Verified Recently

- KMP skeleton exists with `shared`, `desktopApp`, Android `app`, and Android-only `sentence_embeddings`.
- `desktopApp/src/main/kotlin/com/github/zly2006/zhihu/desktop/Main.kt` is shallow and only opens a Compose `Window` with shared JVM UI.
- Common/shared already contains:
  - data models and clients under `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data`
  - QR login state machine and UI in `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/login`
  - KMP Room databases for content filters and local content under `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter` and `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local`
  - pure formatting and policy utilities under `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/util`
- Android login already reuses `SharedQrLoginPane` from `LoginActivity.kt`; Android risk-control WebView remains in app.
- JVM QR login uses shared QR flow and backs up cookies via `DesktopAccountStore`.
- `NavDestination` was temporarily moved back to Android app boundary in `fd313cd`; this corrected platform side-effect leakage but is not the final target. Navigation semantics should be moved back to `shared/commonMain` after Android-only runtime effects are separated.
- Latest verified commands from the previous completed slice:

```bash
./gradlew :shared:ktlintCommonMainSourceSetFormat :shared:ktlintCommonTestSourceSetFormat :app:ktlintMainSourceSetFormat
./gradlew :shared:compileKotlinJvm :shared:jvmTest :desktopApp:compileKotlin assembleLiteDebug :app:testLiteDebugUnitTest
rg -n "android\\.content|android\\.webkit|androidx\\.webkit|\\bIntent\\b|\\bContext\\b|FileProvider|APK|lite|full" shared/src/commonMain/kotlin shared/src/jvmMain/kotlin desktopApp/src -g '*.kt'
rg -n "WebView|android\\.webkit|androidx\\.webkit" desktopApp/src shared/src
git diff --check
```

### Remaining Gaps

- `app/src/main/java/com/github/zly2006/zhihu/data/AccountData.kt` still owns account file persistence, global account state, Android `Context`, shared Ktor config, and token refresh orchestration in one Android object.
- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/PaginationViewModel.kt` is still Android-only and mixes pagination, network fetch, JSON decode, login-expired dialogs, clipboard, Toast, and Android lifecycle.
- Feed display creation is duplicated: shared has `Feed.toDisplayItem()` and Android `BaseFeedViewModel.createDisplayItem()` still reimplements similar logic.
- Desktop/JVM currently proves QR login and hot-list fetch, but it does not yet run the same shared main screen structure as Android.
- Android feed/viewmodel classes still depend on `Context`, `Toast`, `AlertDialog`, and `MainActivity`; these need staged adapter seams. `NavDestination` itself is shared navigation semantics and should not be treated as an Android-only dependency.
- Local recommendation code has KMP Room entities/DAO, but orchestration classes (`LocalRecommendationEngine`, `CrawlingExecutor`, `TaskScheduler`, `UserBehaviorAnalyzer`, `FeedGenerator`, `LocalHomeFeedViewModel`) still live in Android and pass `Context` through business logic.
- Jsoup remains in Android code (`ArticleScreen`, `DailyScreen`, markdown helpers); shared should use Ksoup only when moving pure HTML parsing.
- Full end-state runtime validation is still missing: Android AVD login/cookie/core operations and JVM QR login/cookie/core operations must be executed before claiming completion.

## Parallelization Policy

- Treat each top-level task below as an independent lane after Task 1 is complete.
- If there are two or more independent lanes and the environment supports subagents, dispatch separate subagents for read-only exploration or disjoint file ownership implementation.
- Do not dispatch overlapping write scopes. Example split:
  - Worker A: account/session files
  - Worker B: pagination/feed display files
  - Worker C: local recommendation files
  - Worker D: desktop shared shell files
- If subagent spawning fails due tool configuration, continue inline and record that failure in the final handoff.

## Task 1: Add A Migration Status Ledger

**Files:**
- Create: `docs/kmp-migration-status.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: Create the status ledger**

Create `docs/kmp-migration-status.md` with this content:

```markdown
# KMP Migration Status

## Completed

- Project has `shared`, `desktopApp`, Android `app`, and Android-only `sentence_embeddings`.
- `desktopApp` is a shallow demo1-style launcher.
- QR login core and QR UI are shared via `SharedQrLoginPane`.
- Android login uses shared QR login; Android WebView stays in app.
- JVM login uses shared QR login and stores cookies under `~/.zhihu-plus-plus/account.json`.
- KMP Room is used for content filter and local content databases.
- 共享导航语义应由 `shared/commonMain` 拥有；当前 `NavDestination` 暂在 Android app 侧是待修正状态，不是最终边界。
- Shared has feed data models, notification/daily/hot-list/read-history clients, display formatting, ZSE signing, and local recommendation scoring helpers.

## Do Not Redo

- Do not keep navigation semantics Android-only. Move shared route/destination semantics to `shared/commonMain`; keep only Android runtime side effects (`Context`, `Intent`, WebView, APK/update/install semantics, platform-only callbacks) in app.
- Do not assume desktop needs a separate route model. Desktop should reuse the shared Android UI/navigation semantics for this migration; only introduce a thin runtime adapter if the current Navigation Compose dependency cannot compile for JVM/desktop.
- Do not recreate `Time.android.kt` / `Time.jvm.kt`; use `Clock.System`.
- Do not put APK/lite/full/update/install semantics into `shared`.
- Do not make `desktopApp` contain QR login, cookie persistence, networking, or main UI state.

## Remaining Work

- Move shared navigation semantics / `NavDestination` back to common code after separating Android-only runtime effects.
- Split account/session persistence from Android `AccountData`.
- Move pagination and feed loading state into shared with platform effect adapters.
- Replace Android duplicate feed display mapping with shared `Feed.toDisplayItem`.
- Add a shared hot-list/home shell that Android and desktop can both invoke.
- Move local recommendation orchestration behind platform adapters.
- Move pure HTML/text parsing only after replacing Jsoup with Ksoup-compatible shared code.
- Run Android AVD and JVM QR login runtime validation before final completion.
```

- [ ] **Step 2: Update AGENTS.md to point to the ledger and plan**

Add these bullets under `## KMP 迁移工作约束`:

```markdown
- 继续迁移前必须先查看 `docs/kmp-migration-status.md` 和 `docs/superpowers/plans/2026-05-21-kmp-migration-remaining.md`，确认当前已完成/未完成边界，避免重复迁移同一模块或把已经纠正的平台边界改回去。
- 对两个以上互不重叠的迁移 lane，默认优先并行推进：能用 subagent 时按文件所有权拆分给 subagent；不能用 subagent 时也要按 lane 批量审查、批量验证，避免串行地反复读同一批文件。
```

- [ ] **Step 3: Verify documentation**

Run:

```bash
rg -n "KMP Migration Status|Do Not Redo|2026-05-21-kmp-migration-remaining|subagent|并行" AGENTS.md docs/kmp-migration-status.md docs/superpowers/plans/2026-05-21-kmp-migration-remaining.md
```

Expected: all new anchors are found.

- [ ] **Step 4: Commit**

```bash
git add AGENTS.md docs/kmp-migration-status.md docs/superpowers/plans/2026-05-21-kmp-migration-remaining.md
git commit -m "docs: 记录 KMP 剩余迁移计划"
```

## Task 2: Restore Shared Navigation Semantics

**Files:**
- Move: `app/src/main/java/com/github/zly2006/zhihu/navigation/NavDestination.kt` -> `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation/NavDestination.kt`
- Modify: `app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMain.kt`
- Modify: `app/src/main/java/com/github/zly2006/zhihu/data/HistoryStorage.kt`
- Modify: app files importing `com.github.zly2006.zhihu.navigation.*`
- Test: add or update shared/common tests for route serialization.

- [ ] **Step 1: Audit platform-only content in NavDestination**

Run:

```bash
rg -n "android\\.|Context|Intent|WebView|FileProvider|APK|lite|full|NavHostController|NavBackStackEntry|composable<|hasRoute" app/src/main/java/com/github/zly2006/zhihu/navigation/NavDestination.kt app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMain.kt
```

Expected: `NavDestination.kt` contains only serializable route semantics; Android navigation runtime usage is in `ZhihuMain.kt`.

- [ ] **Step 2: Move the route semantics file**

Use `git mv`:

```bash
mkdir -p shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation
git mv app/src/main/java/com/github/zly2006/zhihu/navigation/NavDestination.kt shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation/NavDestination.kt
```

Do not move Android `NavHostController`, `NavBackStackEntry`, `Intent`, WebView, APK/update, or platform callback semantics with it.

- [ ] **Step 3: Keep Android runtime adapter in app**

Keep Android-only navigation runtime code in app files such as `ZhihuMain.kt`, `LocalNavigator.kt`, and `AnswerNavigator.kt`. If shared route models require a platform action, expose it through a small interface rather than passing Android `Context` into shared.

- [ ] **Step 4: Check whether Navigation Compose runtime can be shared**

Run dependency/compile checks before deciding:

```bash
rg -n "navigation-compose|androidx.navigation" gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts shared/build.gradle.kts desktopApp/build.gradle.kts
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin
```

Expected: if current navigation runtime supports JVM/desktop, plan a follow-up to move the shared NavHost shell. If it does not, keep route semantics in shared and create thin Android/desktop runtime adapters.

- [ ] **Step 5: Verify route boundary**

Run:

```bash
rg -n "android\\.content|android\\.webkit|androidx\\.webkit|\\bIntent\\b|\\bContext\\b|FileProvider|APK|lite|full" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin assembleLiteDebug :app:testLiteDebugUnitTest
```

Expected: no platform runtime leak in shared navigation; builds/tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/github/zly2006/zhihu/navigation shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMain.kt app/src/main/java/com/github/zly2006/zhihu/data/HistoryStorage.kt
git commit -m "refactor: 共享导航语义"
```

## Task 3: Extract Shared Account Session Core

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account/ZhihuAccountSession.kt`
- Create: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account/ZhihuAccountRepository.kt`
- Create: `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/account/ZhihuAccountRepositoryTest.kt`
- Modify: `app/src/main/java/com/github/zly2006/zhihu/data/AccountData.kt`
- Modify: `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopAccountStore.kt`

- [ ] **Step 1: Write common repository tests**

Create `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/account/ZhihuAccountRepositoryTest.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZhihuAccountRepositoryTest {
    @Test
    fun saveAndLoadRoundTripKeepsCookiesAndProfile() {
        val store = InMemoryAccountStore()
        val repository = ZhihuAccountRepository(store)
        val session = ZhihuAccountSession(
            login = true,
            username = "Alice",
            cookies = mutableMapOf("z_c0" to "token", "_xsrf" to "xsrf"),
            userAgent = "ua",
            profile = ZhihuAccountProfileSnapshot(id = "1", name = "Alice", urlToken = "alice"),
        )

        repository.save(session)

        assertEquals(session, repository.load())
        assertTrue(repository.current.login)
        assertEquals("token", repository.current.cookies["z_c0"])
    }

    @Test
    fun deleteResetsToLoggedOutSession() {
        val repository = ZhihuAccountRepository(InMemoryAccountStore())
        repository.save(ZhihuAccountSession(login = true, username = "Alice"))

        repository.delete()

        assertFalse(repository.current.login)
        assertEquals("", repository.current.username)
    }
}

private class InMemoryAccountStore : ZhihuAccountStore {
    private var saved: String? = null

    override fun readAccountJson(): String? = saved

    override fun writeAccountJson(value: String) {
        saved = value
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
./gradlew :shared:jvmTest --tests '*ZhihuAccountRepositoryTest*'
```

Expected: FAIL because `ZhihuAccountRepository` and related types do not exist.

- [ ] **Step 3: Implement common account session types**

Create `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account/ZhihuAccountSession.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.account

import kotlinx.serialization.Serializable

const val DEFAULT_ZHIHU_USER_AGENT =
    "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/540.0 (KHTML, like Gecko) Ubuntu/10.10 Chrome/9.1.0.0 Safari/540.0"

@Serializable
data class ZhihuAccountProfileSnapshot(
    val id: String = "",
    val name: String = "",
    val urlToken: String? = null,
)

@Serializable
data class ZhihuAccountSession(
    val login: Boolean = false,
    val username: String = "",
    val cookies: MutableMap<String, String> = mutableMapOf(),
    val userAgent: String = DEFAULT_ZHIHU_USER_AGENT,
    val profile: ZhihuAccountProfileSnapshot? = null,
)
```

Create `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account/ZhihuAccountRepository.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.account

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ZhihuAccountStore {
    fun readAccountJson(): String?
    fun writeAccountJson(value: String)
}

class ZhihuAccountRepository(
    private val store: ZhihuAccountStore,
) {
    private val state = MutableStateFlow(readStoredSession())
    val session: StateFlow<ZhihuAccountSession> = state
    val current: ZhihuAccountSession get() = state.value

    fun load(): ZhihuAccountSession {
        val loaded = readStoredSession()
        state.value = loaded
        return loaded
    }

    fun save(session: ZhihuAccountSession) {
        state.value = session
        store.writeAccountJson(ZhihuJson.json.encodeToString(session))
    }

    fun delete() {
        save(ZhihuAccountSession())
    }

    private fun readStoredSession(): ZhihuAccountSession {
        val raw = store.readAccountJson() ?: return ZhihuAccountSession()
        return runCatching {
            ZhihuJson.json.decodeFromString<ZhihuAccountSession>(raw)
        }.getOrDefault(ZhihuAccountSession())
    }
}
```

- [ ] **Step 4: Run common test**

Run:

```bash
./gradlew :shared:jvmTest --tests '*ZhihuAccountRepositoryTest*'
```

Expected: PASS.

- [ ] **Step 5: Adapt Android `AccountData` without changing public callers**

Modify `app/src/main/java/com/github/zly2006/zhihu/data/AccountData.kt` by adding an internal file-backed store and mapping helpers. Keep `AccountData.Data` temporarily so existing Android callers compile.

```kotlin
private class AndroidAccountStore(
    private val context: Context,
) : ZhihuAccountStore {
    private val file: File get() = File(context.filesDir, "account.json")

    override fun readAccountJson(): String? =
        file.takeIf { it.exists() }?.readText()

    override fun writeAccountJson(value: String) {
        file.writeText(value)
    }
}

private fun Data.toSession(): ZhihuAccountSession = ZhihuAccountSession(
    login = login,
    username = username,
    cookies = cookies,
    userAgent = userAgent,
    profile = self?.let {
        ZhihuAccountProfileSnapshot(
            id = it.id,
            name = it.name,
            urlToken = it.urlToken,
        )
    },
)

private fun ZhihuAccountSession.toAndroidData(): Data = Data(
    login = login,
    username = username,
    cookies = cookies,
    userAgent = userAgent,
    self = profile?.let {
        Person(
            id = it.id,
            name = it.name,
            urlToken = it.urlToken.orEmpty(),
        )
    },
)
```

If `Person` requires additional constructor parameters, do not fake them. Instead keep `self` in Android `Data` for this task and only use the shared repository for `login`, `username`, `cookies`, and `userAgent`; migrate `self` in a dedicated model-specific task.

- [ ] **Step 6: Adapt JVM `DesktopAccountStore`**

Modify `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopAccountStore.kt` so it stores `ZhihuAccountSession` through `ZhihuAccountRepository`. Keep `DesktopAccountData` as a typealias only if callers still need the old name:

```kotlin
typealias DesktopAccountData = ZhihuAccountSession
```

Implement a JVM store:

```kotlin
private class JvmFileAccountStore(
    private val accountFile: Path,
) : ZhihuAccountStore {
    override fun readAccountJson(): String? =
        if (accountFile.exists()) accountFile.readText() else null

    override fun writeAccountJson(value: String) {
        accountFile.parent.createDirectories()
        accountFile.writeText(value)
    }
}
```

- [ ] **Step 7: Verify**

Run:

```bash
./gradlew :shared:ktlintCommonMainSourceSetFormat :shared:ktlintCommonTestSourceSetFormat :shared:ktlintJvmMainSourceSetFormat
./gradlew :shared:jvmTest :desktopApp:compileKotlin assembleLiteDebug
```

Expected: all pass.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/account app/src/main/java/com/github/zly2006/zhihu/data/AccountData.kt shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopAccountStore.kt
git commit -m "refactor: 抽取共享账号会话仓库"
```

## Task 4: Replace Android Feed Display Duplication With Shared Mapping

**Files:**
- Modify: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/feed/BaseFeedViewModel.kt`
- Modify: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/feed/HotListViewModel.kt`
- Test: existing `:app:testLiteDebugUnitTest` plus shared tests

- [ ] **Step 1: Inspect current duplicate mapping**

Run:

```bash
rg -n "fun Feed\\.toDisplayItem|open fun createDisplayItem|FeedDisplayItem\\(" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/FeedDisplayItem.kt app/src/main/java/com/github/zly2006/zhihu/viewmodel/feed/BaseFeedViewModel.kt
```

Expected: shared `Feed.toDisplayItem()` and Android `BaseFeedViewModel.createDisplayItem()` both construct `FeedDisplayItem`.

- [ ] **Step 2: Replace Android implementation with shared helper**

Modify `BaseFeedViewModel.createDisplayItem` to keep Android preferences only at the call boundary:

```kotlin
open fun createDisplayItem(context: Context, feed: Feed): FeedDisplayItem {
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    return feed.toDisplayItem(
        enableQualityFilter = preferences.getBoolean("enableQualityFilter", true),
        reverseBlock = preferences.getBoolean("reverseBlock", false),
    )
}
```

Remove now-unused imports for feed subclasses that were only needed by the duplicated `when`.

- [ ] **Step 3: Keep HotList behavior**

Keep `HotListViewModel` override:

```kotlin
override fun createDisplayItem(context: Context, feed: Feed): FeedDisplayItem =
    super.createDisplayItem(context, feed).copy(
        authorName = null,
        avatarSrc = null,
    )
```

- [ ] **Step 4: Verify duplicate removal**

Run:

```bash
rg -n "is CommonFeed, is FeedItemIndexGroup, is MomentsFeed, is HotListFeed|QuestionFeedCard ->" app/src/main/java/com/github/zly2006/zhihu/viewmodel/feed/BaseFeedViewModel.kt
```

Expected: no output.

- [ ] **Step 5: Build**

Run:

```bash
./gradlew :app:ktlintMainSourceSetFormat
./gradlew :shared:jvmTest :desktopApp:compileKotlin assembleLiteDebug :app:testLiteDebugUnitTest
```

Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/github/zly2006/zhihu/viewmodel/feed/BaseFeedViewModel.kt app/src/main/java/com/github/zly2006/zhihu/viewmodel/feed/HotListViewModel.kt
git commit -m "refactor: 复用 shared Feed 展示映射"
```

## Task 5: Extract Shared Pagination Loader Core

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/paging/ZhihuPageLoader.kt`
- Create: `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/paging/ZhihuPageLoaderTest.kt`
- Modify: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/PaginationViewModel.kt`
- Modify: feed subclasses only as needed.

- [ ] **Step 1: Write loader tests with Ktor MockEngine**

Create `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/paging/ZhihuPageLoaderTest.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.paging

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ZhihuPageLoaderTest {
    @Serializable
    data class Item(val id: Int, val title: String)

    @Test
    fun loadNextDecodesDataAndPaging() = kotlinx.coroutines.test.runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"data":[{"id":1,"title":"A"}],"paging":{"is_end":false,"next":"https://example.com/next"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(com.github.zly2006.zhihu.shared.data.ZhihuJson.json)
            }
        }
        val loader = ZhihuPageLoader(Item.serializer(), initialUrl = "https://example.com/start")

        val page = loader.loadNext(client)

        assertEquals(listOf(Item(1, "A")), page.items)
        assertFalse(page.paging.isEnd)
        assertEquals("https://example.com/next", page.paging.next)
    }
}
```

- [ ] **Step 2: Run failing test**

```bash
./gradlew :shared:jvmTest --tests '*ZhihuPageLoaderTest*'
```

Expected: FAIL because `ZhihuPageLoader` does not exist.

- [ ] **Step 3: Implement common loader**

Create `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/paging/ZhihuPageLoader.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.paging

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

@Serializable
data class ZhihuPagingState(
    val page: Int = -1,
    val isEnd: Boolean = false,
    val isStart: Boolean = false,
    val previous: String? = null,
    val totals: Int = 0,
    val next: String,
    val prev: String? = null,
)

data class ZhihuLoadedPage<T>(
    val items: List<T>,
    val rawData: JsonArray,
    val paging: ZhihuPagingState,
)

class ZhihuPageLoader<T : Any>(
    private val serializer: KSerializer<T>,
    private val initialUrl: String,
    private val include: String = "data[*].content,excerpt,headline,target.author.badge_v2",
    private val shouldSkipRawItem: (JsonObject) -> Boolean = { false },
) {
    var lastPaging: ZhihuPagingState? = null
        private set

    val isEnd: Boolean get() = lastPaging?.isEnd == true

    fun reset() {
        lastPaging = null
    }

    suspend fun loadNext(client: HttpClient): ZhihuLoadedPage<T> {
        val url = (lastPaging?.next ?: initialUrl).replace("http://", "https://")
        val response = client.get(url) {
            if (include.isNotEmpty()) {
                parameter("include", include)
            }
        }.body<JsonObject>()
        val data = response["data"]?.jsonArray ?: JsonArray(emptyList())
        val items = data.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            if (shouldSkipRawItem(obj)) return@mapNotNull null
            runCatching { ZhihuJson.decodeJson(serializer, obj) }.getOrNull()
        }
        val paging = response["paging"]?.let { ZhihuJson.decodeJson<ZhihuPagingState>(it) }
            ?: ZhihuPagingState(isEnd = true, next = url)
        lastPaging = paging
        return ZhihuLoadedPage(items, data, paging)
    }
}
```

- [ ] **Step 4: Adapt Android PaginationViewModel**

Modify `app/src/main/java/com/github/zly2006/zhihu/viewmodel/PaginationViewModel.kt` so Android still owns UI side effects, but data loading goes through `ZhihuPageLoader`.

Keep Android-only error handling in this file. Do not move dialogs, Toasts, clipboard, `LoginActivity`, or `Context` into shared.

- [ ] **Step 5: Verify**

```bash
./gradlew :shared:ktlintCommonMainSourceSetFormat :shared:ktlintCommonTestSourceSetFormat :app:ktlintMainSourceSetFormat
./gradlew :shared:jvmTest :desktopApp:compileKotlin assembleLiteDebug :app:testLiteDebugUnitTest
```

Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/paging shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/paging app/src/main/java/com/github/zly2006/zhihu/viewmodel/PaginationViewModel.kt
git commit -m "refactor: 抽取共享分页加载器"
```

## Task 6: Create Shared Hot List Screen And Use It On Desktop

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/hotlist/SharedHotListScreen.kt`
- Modify: `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopQrLogin.kt`
- Optional follow-up in the same lane: `app/src/main/java/com/github/zly2006/zhihu/ui/HotListScreen.kt`

- [ ] **Step 1: Create shared hot-list UI**

Create `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/hotlist/SharedHotListScreen.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.hotlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem

@Composable
fun SharedHotListScreen(
    username: String,
    loadHotItems: suspend () -> List<FeedDisplayItem>,
    modifier: Modifier = Modifier,
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var hotItems by remember { mutableStateOf<List<FeedDisplayItem>>(emptyList()) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorText = null
        runCatching { loadHotItems() }
            .onSuccess { hotItems = it }
            .onFailure { errorText = it.message ?: "加载失败" }
        isLoading = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("知乎热榜", style = MaterialTheme.typography.headlineSmall)
            Text("已登录：$username", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.size(12.dp))
            Button(onClick = { refreshKey += 1 }) {
                Text("刷新")
            }
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            errorText != null -> Text(
                text = errorText.orEmpty(),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error,
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(hotItems, key = { it.stableKey }) { item ->
                    SharedHotListItem(item)
                }
            }
        }
    }
}

@Composable
private fun SharedHotListItem(item: FeedDisplayItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            item.summary?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.size(6.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(item.details, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

- [ ] **Step 2: Replace JVM-private hot-list composables**

Modify `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopQrLogin.kt`:

```kotlin
SharedHotListScreen(
    username = store.load().username,
    loadHotItems = {
        val account = store.load()
        store.createHttpClient(account.cookies).use { client ->
            fetchHotListPage(client, account.cookies)
                .data
                .flattenFeeds()
                .map { it.toDisplayItem(enableQualityFilter = false) }
        }
    },
)
```

Remove `DesktopHotListScreen` and `HotListItem` from `DesktopQrLogin.kt`.

- [ ] **Step 3: Verify desktop remains WebView-free**

Run:

```bash
rg -n "WebView|android\\.webkit|androidx\\.webkit" desktopApp/src shared/src
./gradlew :shared:ktlintCommonMainSourceSetFormat :shared:ktlintJvmMainSourceSetFormat :desktopApp:compileKotlin
```

Expected: grep has no output; compile passes.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/hotlist/SharedHotListScreen.kt shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopQrLogin.kt
git commit -m "refactor: 共享热榜桌面页面"
```

## Task 7: Split Notification Preferences From Android ViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/notification/NotificationVisibilityPolicy.kt`
- Create: `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/notification/NotificationVisibilityPolicyTest.kt`
- Modify: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/NotificationViewModel.kt`
- Modify: `app/src/main/java/com/github/zly2006/zhihu/ui/NotificationSettingsScreen.kt` only if needed for adapter wiring.

- [ ] **Step 1: Write policy test**

Create `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/notification/NotificationVisibilityPolicyTest.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.notification

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationVisibilityPolicyTest {
    @Test
    fun unknownVerbIsVisibleByDefault() {
        assertTrue(shouldShowNotificationVerb("unknown_verb", emptySet()))
    }

    @Test
    fun disabledVerbIsHidden() {
        assertFalse(shouldShowNotificationVerb("MEMBER_VOTEUP_ARTICLE", setOf("MEMBER_VOTEUP_ARTICLE")))
    }
}
```

- [ ] **Step 2: Implement policy**

Create `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/notification/NotificationVisibilityPolicy.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.notification

fun shouldShowNotificationVerb(
    verb: String,
    disabledVerbs: Set<String>,
): Boolean = verb !in disabledVerbs
```

- [ ] **Step 3: Adapt Android viewmodel**

In `NotificationViewModel.shouldShowNotification`, keep reading Android preferences in app, but call the shared policy with an explicit disabled set. Do not move `SharedPreferences` into shared.

- [ ] **Step 4: Verify**

```bash
./gradlew :shared:jvmTest --tests '*NotificationVisibilityPolicyTest*'
./gradlew :app:ktlintMainSourceSetFormat :shared:ktlintCommonMainSourceSetFormat :shared:ktlintCommonTestSourceSetFormat
./gradlew :shared:jvmTest :desktopApp:compileKotlin assembleLiteDebug :app:testLiteDebugUnitTest
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/notification shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/notification app/src/main/java/com/github/zly2006/zhihu/viewmodel/NotificationViewModel.kt
git commit -m "refactor: 抽取通知显示策略"
```

## Task 8: Move Local Recommendation Orchestration Behind Adapters

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/recommendation/LocalRecommendationEngineCore.kt`
- Create: `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/recommendation/LocalRecommendationEngineCoreTest.kt`
- Modify: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/local/LocalRecommendationEngine.kt`
- Modify in the same lane after engine core compiles: `CrawlingExecutor.kt`, `TaskScheduler.kt`, `UserBehaviorAnalyzer.kt`, `FeedGenerator.kt`.

- [ ] **Step 1: Write pure ranking/diversity tests**

Create `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/recommendation/LocalRecommendationEngineCoreTest.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.recommendation

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalRecommendationEngineCoreTest {
    @Test
    fun rankByScoreDescending() {
        val ranked = rankLocalCandidates(
            candidates = listOf(
                LocalCandidateScore(id = "a", score = 1.0, reason = "Trending", createdAt = 100),
                LocalCandidateScore(id = "b", score = 3.0, reason = "Follow", createdAt = 100),
            ),
            nowEpochMillis = 100,
        )

        assertEquals(listOf("b", "a"), ranked.map { it.id })
    }
}
```

- [ ] **Step 2: Implement pure engine core**

Create `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/recommendation/LocalRecommendationEngineCore.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.recommendation

data class LocalCandidateScore(
    val id: String,
    val score: Double,
    val reason: String,
    val createdAt: Long,
)

fun rankLocalCandidates(
    candidates: List<LocalCandidateScore>,
    nowEpochMillis: Long,
): List<LocalCandidateScore> =
    candidates.sortedWith(
        compareByDescending<LocalCandidateScore> { it.score }
            .thenByDescending { it.createdAt.coerceAtMost(nowEpochMillis) },
    )
```

- [ ] **Step 3: Wire Android engine to the pure core**

In `LocalRecommendationEngine.rankCandidate`, keep Android database/network code in app for now, but move the pure formula into common helpers one function at a time. Every moved helper must get a common test before the Android call site changes.

- [ ] **Step 4: Do not move platform pieces**

Keep these Android-only until explicit adapter tasks exist:

```text
android.net.ConnectivityManager
android.content.Context
AlertDialog / Toast
AccountData.fetchGet(context, ...)
```

`NavDestination` is not listed here because it is shared route semantics. Only platform runtime adapters around it should remain Android-only.

- [ ] **Step 5: Verify**

```bash
./gradlew :shared:jvmTest --tests '*LocalRecommendationEngineCoreTest*'
./gradlew :shared:ktlintCommonMainSourceSetFormat :shared:ktlintCommonTestSourceSetFormat :app:ktlintMainSourceSetFormat
./gradlew :shared:jvmTest :desktopApp:compileKotlin assembleLiteDebug
```

Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/recommendation shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/recommendation app/src/main/java/com/github/zly2006/zhihu/viewmodel/local/LocalRecommendationEngine.kt
git commit -m "refactor: 抽取本地推荐核心排序"
```

## Task 9: Replace Jsoup Candidates With Ksoup-Compatible Shared Parsers

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/html/ZhihuHtmlText.kt`
- Create: `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/html/ZhihuHtmlTextTest.kt`
- Modify candidates only after tests pass:
  - `app/src/main/java/com/github/zly2006/zhihu/ui/DailyScreen.kt`
  - `app/src/main/java/com/github/zly2006/zhihu/ui/ArticleScreen.kt`
  - `app/src/main/java/com/github/zly2006/zhihu/markdown/MdAst.kt`

- [ ] **Step 1: Write parser test**

Create `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/html/ZhihuHtmlTextTest.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.html

import kotlin.test.Test
import kotlin.test.assertEquals

class ZhihuHtmlTextTest {
    @Test
    fun extractsReadableTextFromParagraphs() {
        assertEquals(
            "第一段 第二段",
            zhihuHtmlText("<p>第一段</p><p>第二段</p>"),
        )
    }
}
```

- [ ] **Step 2: Implement with Ksoup**

Create `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/html/ZhihuHtmlText.kt`:

```kotlin
package com.github.zly2006.zhihu.shared.html

import com.fleeksoft.ksoup.Ksoup

fun zhihuHtmlText(html: String): String =
    Ksoup.parse(html)
        .text()
        .trim()
```

- [ ] **Step 3: Replace only pure text extraction first**

In `DailyScreen.kt`, replace `Jsoup.parse(...).text()` style calls with `zhihuHtmlText(...)` only when the result is plain text. Do not move WebView document manipulation from `ArticleScreen` until a Ksoup equivalent is proven.

- [ ] **Step 4: Verify**

```bash
./gradlew :shared:jvmTest --tests '*ZhihuHtmlTextTest*'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin assembleLiteDebug
rg -n "org\\.jsoup|Jsoup" shared/src/commonMain/kotlin
```

Expected: tests and builds pass; shared has no Jsoup.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/html shared/src/commonTest/kotlin/com/github/zly2006/zhihu/shared/html app/src/main/java/com/github/zly2006/zhihu/ui/DailyScreen.kt
git commit -m "refactor: 使用 Ksoup 共享 HTML 文本解析"
```

## Task 10: Runtime Validation Gate

**Files:**
- Modify only if validation finds bugs.
- Update: `docs/kmp-migration-status.md`

- [ ] **Step 1: Run compile gate**

```bash
./gradlew :shared:compileKotlinJvm :shared:jvmTest :desktopApp:compileKotlin assembleLiteDebug :app:testLiteDebugUnitTest
rg -n "WebView|android\\.webkit|androidx\\.webkit" desktopApp/src shared/src
rg -n "android\\.content|android\\.webkit|androidx\\.webkit|\\bIntent\\b|\\bContext\\b|FileProvider|APK|lite|full" shared/src/commonMain/kotlin shared/src/jvmMain/kotlin desktopApp/src -g '*.kt'
git diff --check
```

Expected: Gradle commands pass; boundary grep has no shared/desktop violations; diff check has no output.

- [ ] **Step 2: Validate Android on AVD**

Use AVD, not a physical device:

```bash
grep "applicationId" app/build.gradle.kts
./gradlew assembleLiteDebug
adb install -r app/build/outputs/apk/lite/debug/app-lite-debug.apk
adb shell am force-stop com.github.zly2006.zhplus.lite
adb shell monkey -p com.github.zly2006.zhplus.lite -c android.intent.category.LAUNCHER 1
python3 .agents/skills/ui-test/llm_test_helper.py dump
```

Expected: app launches; login/cookie-covered core operations still work; UI has no unexpected blank screen or obvious layout regression.

- [ ] **Step 3: Validate JVM QR login**

Run desktop app:

```bash
./gradlew :desktopApp:run
```

When QR appears, notify:

```bash
terminal-notifier -message "需要扫码登录 JVM 端" -sound default
```

Expected after user scans: `~/.zhihu-plus-plus/account.json` contains backed-up cookies and subsequent app launches do not require repeated login.

- [ ] **Step 4: Update status ledger**

Append results to `docs/kmp-migration-status.md`:

```markdown
## Runtime Validation

- Android AVD: passed on YYYY-MM-DD with lite package `com.github.zly2006.zhplus.lite`.
- JVM QR login: passed on YYYY-MM-DD; cookies persisted to `~/.zhihu-plus-plus/account.json`.
- Desktop WebView boundary: `rg` check passed on YYYY-MM-DD.
```

- [ ] **Step 5: Commit validation notes**

```bash
git add docs/kmp-migration-status.md
git commit -m "docs: 记录 KMP 运行验证结果"
```

## Final Completion Checklist

- [ ] `desktopApp` remains shallow and contains no QR login/network/business state.
- [ ] shared/commonMain contains shared navigation semantics, but no Android `Context`, WebView, intent/file-provider, APK/lite/full runtime side effects.
- [ ] desktop/shared contains no WebView imports or implementation.
- [ ] Room databases touched by migration use Room KMP with common entities/DAO/database and platform builders only.
- [ ] Android lite builds and runs on AVD.
- [ ] JVM desktop compiles and QR login persists cookies.
- [ ] Android and JVM use shared QR login and shared core data/network logic where available.
- [ ] iOS targets remain present but no iOS build/test/debug command was run for this migration.
- [ ] Every completed lane has a focused commit.
