# ContentFilterManager Shared Boundary Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterManager.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterManager.kt`
- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- Existing KMP Room DAO/database files under `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/`.

## Conclusion

`ContentFilterManager` is shared core logic. It maintains feed exposure records, interactions, already-viewed queries, cleanup/reset, and stats over the shared `ContentFilterDao`. The Android-only part was the old `Context` singleton and database lookup, not the manager behavior.

The file was moved with `git mv` to `shared/commonMain`, and the constructor now accepts `ContentFilterDao`. Android callers still provide the DAO through `getContentFilterDatabase(context)` until `ContentFilterExtensions` is migrated in a later slice.

## Evidence

- `ContentFilterManager.kt` in commonMain has no `Context`, `SharedPreferences`, `Toast`, `Log`, `ContentDetailCache`, `Jsoup`, or Android imports.
- The old `ContentFilterManager.getInstance(context)` call sites in `ContentFilterExtensions.kt` were replaced with `ContentFilterManager(getContentFilterDatabase(context).contentFilterDao())`, keeping the database builder lookup in Android code.
- Stats/cleanup/reset reuse the shared `ContentFilterMaintenance` owner instead of keeping a second cleanup implementation.
- `ContentViewRecord` and `ContentOpenEvent` remain separate; this slice only moves feed exposure state management.

## Platform Boundaries

Shared:

- `ContentFilterManager`
- `ContentFilterDao` operations and shared Room entities.
- Feed exposure/interaction record mutation, already-viewed lookup, cleanup/reset semantics.

Platform:

- Android `Context` and `getContentFilterDatabase(context)` lookup.
- Feed filtering orchestration in `ContentFilterExtensions` remains Android-side for now because it still contains settings reads, detail fetch, NLP, logging, and Toast.

## Subagent

Independent `gpt-5.5 xhigh` review completed before this change. It recommended `Slice A`: move shared content-view/filter-maintenance core and replace `ContentFilterManager` first. This slice follows that recommendation.

## Verification

Passed:

```bash
rg -n "android\\.|Context|SharedPreferences|Toast|Build\\.|Log\\.|ContentDetailCache|Jsoup|NLPService|getContentFilterDatabase\\(" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterManager.kt shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/filter -g '*.kt'
rg -n "ContentFilterManager\\.getInstance|class ContentFilterManager|typealias FilterStats|data class FilterStats" app/src/main shared/src -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabaseTest
```

Expected remaining work:

- `ContentFilterExtensions`, `BlocklistManager`, and `BlockedKeywordRepository` still need split/migration in later slices.
- Android build still has unrelated broader KMP migration debt; this slice should be evaluated by common/JVM compile and boundary grep until those blockers are removed.
