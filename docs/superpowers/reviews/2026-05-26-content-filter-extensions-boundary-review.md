# ContentFilterExtensions Boundary Review

Date: 2026-05-26
Reviewer: gpt-5.5 xhigh subagent `019e5ffc-58a3-7660-a713-835ee13ff1c1`, with local source check by main agent.

## Scope

Target lane: `ContentFilterExtensions` and feed content-filter orchestration migration.

Primary files:

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterEntries.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedDisplayFilterPipeline.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ForegroundReadFilterPipeline.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterManager.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/AndroidPaginationEnvironment.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`

Master reference:

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`

## Conclusion

`ContentFilterExtensions` is feed-stage filtering orchestration, not Android ownership. Its core semantics belong in `shared/commonMain`: setting interpretation, foreground read filtering, feed item to `FilterableContent` mapping, ad/paid filtering, keyword/NLP/user/topic filtering, reverse-block behavior, blocked-feed record persistence, exposure records, interaction records, and maintenance cleanup.

Android ownership is limited to `Context`, `SharedPreferences` bridge, Android database builder/file path, `ContentDetailCache.getOrFetch(context, ...)`, Toast/log callbacks, and full/lite NLP matcher injection through `AndroidContentFilterRuntime`.

`ContentOpenEventSupport` is content-open/read-history semantics and must stay separate from feed filtering.

## Evidence

- Current `shared/commonMain` already contains `ContentFilterEntries.kt`, `FeedDisplayFilterPipeline.kt`, `ForegroundReadFilterPipeline.kt`, `ContentFilterManager.kt`, KMP Room database definitions, and shared `FilterableContent`.
- Current Android `ContentFilterExtensions.kt` wraps shared functions with `Context -> settings/database/detail provider/runtime callbacks`.
- JVM `PaginationEnvironment.jvm.kt` already calls common `applyForegroundReadFilterToDisplayItems()` and `applyContentFilterToDisplayItems()` directly with `settingsStore`, `contentFilterDatabase`, and `desktopKeywordSemanticMatcher`.
- Android `AndroidPaginationEnvironment.android.kt` still calls `ContentFilterExtensions.applyForegroundReadFilterToDisplayItems(context, items)` and `ContentFilterExtensions.applyContentFilterToDisplayItems(context, foregroundItems)`, so the remaining Android object is a compatibility wrapper, not core logic.

## Recommended First Slice

Move the `ContentFilterExtensions` object itself to `shared/commonMain` as a common compatibility API over the existing common entry functions, preserving these names:

- `isContentFilterEnabled`
- `isKeywordBlockingEnabled`
- `isNLPBlockingEnabled`
- `getNLPSimilarityThreshold`
- `isUserBlockingEnabled`
- `isTopicBlockingEnabled`
- `getTopicBlockingThreshold`
- `recordContentDisplay`
- `recordContentInteraction`
- `performMaintenanceCleanup`
- `applyForegroundReadFilterToDisplayItems`
- `applyContentFilterToDisplayItems`

Use common parameters such as `FeedFilterSettings`, `ContentFilterDatabase`, `ContentDetailProvider`, `KeywordSemanticMatcher`, and callback hooks. Keep Android `Context` overloads as a thin Android wrapper or update Android callers to use `AndroidPaginationEnvironment` dependencies directly. Do not change UI.

## Platform Boundaries

Shared:

- Feed filter settings model and setting-derived booleans.
- `ContentFilterManager`, `FilterableContent`, `FeedDisplayFilterPipeline`, `ForegroundReadFilterPipeline`.
- Blocked feed record saving and cleanup limits.
- Filtering strategy and output ordering, including followed-user preservation and reverse-block mode.

Android-only:

- `Context`, `SharedPreferences`, `PREFERENCE_NAME` bridge.
- `ContentDetailCache.getOrFetch(context, ...)`.
- Toast/log presentation and `mainExecutor`.
- Android Room builder/file path.
- Full/lite NLP runtime injection.

JVM-only:

- Desktop database file path and desktop matcher/provider implementations.
- Desktop message/log sink behavior.

## Risks

- Do not replace filtering with no-op or desktop-only fallback.
- Do not merge settings, maintenance, Toast/log, database, detail fetch, and NLP into one page-specific adapter.
- Do not move full/lite NLP implementation into common; common should accept matcher interfaces or callbacks.
- Do not mix `ContentOpenEventSupport` with feed filtering.
- Preserve master function names and the feed filtering order; do not invent a parallel filtering implementation.

## Validation

Recommended commands:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:jvmTest :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet
git diff --check
```

Boundary grep:

```bash
rg -n "ContentFilterExtensions|ContentFilterManager|FilterableContent|ContentFilterRuntime|ContentOpenEventSupport|BlocklistManager|ContentFilterDatabase" app/src/main/java shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/jvmMain/kotlin -g '*.kt'
rg -n "android\\.|Context|SharedPreferences|Toast|Log\\.|MainActivity|AccountData|getContentDetail|ContentDetailCache" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
rg -n "Toast.makeText|getSharedPreferences|ContentFilterMaintenance|BlockedFeedRecord|ContentOpenEvent|ContentViewRecord" app/src/main/java shared/src/androidMain/kotlin shared/src/commonMain/kotlin -g '*.kt'
```

## Caveat

The subagent initially timed out repeatedly and was interrupted to produce a concise conclusion. The main agent then checked the target files, master reference path, Android/JVM callers, and existing common pipeline files before recording this review.
