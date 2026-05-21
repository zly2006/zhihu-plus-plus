# Foreground Read Filter Shared Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ForegroundReadFilterPipeline.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterManager.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterDao.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/BlockedFeedRecordDao.kt`

Subagent: `019e48eb-9b3e-7442-b014-7d99fb8d5ee1` (`gpt-5.5`, `xhigh`) completed before implementation.

## Conclusion

Feed exposure recording, interaction recording, maintenance cleanup, and foreground read filtering are shared semantics. Android owns only settings retrieval, database instance construction, call timing, and logging.

## Implementation

- Added shared `ContentExposureRecorder` for display, interaction, and maintenance operations behind `FeedFilterSettings`.
- Added shared `ForegroundReadFilterPipeline`.
- Moved foreground read/low-quality filtering, followed-user bypass, view recording, viewed-id lookup, and blocked-feed history persistence into shared.
- Kept Android `ContentFilterExtensions` as a wrapper that reads `SharedPreferences`, gets the KMP Room database, and logs failures.
- Added JVM tests for disabled behavior, view recording, blocked history, followed-user bypass, interaction marking, and cleanup.

## Boundaries

Shared:

- `ContentFilterManager` operations over `ContentFilterDao`;
- `ContentViewRecord.generateId` and already-viewed checks;
- foreground low-quality heuristics (`小时前`, `分钟前`, `浏览`);
- kept-item view recording;
- `BlockedFeedRecord` construction/persistence via common DAO.

Android:

- `Context`, `SharedPreferences`, `PREFERENCE_NAME`;
- Android Room database builder lookup;
- lifecycle trigger for maintenance;
- Android logging.

## Verification

```bash
rg -n "android\\.|Context|SharedPreferences|Toast|Log\\.|Build\\.|ContentDetailCache|Jsoup|NlpService|getContentFilterDatabase\\(" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
rg -n "recordContentDisplay|recordContentInteraction|performMaintenanceCleanup|applyForegroundReadFilterToDisplayItems|ContentFilterExtensions" app/src/main/java shared/src -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.ForegroundReadFilterPipelineTest --tests com.github.zly2006.zhihu.viewmodel.filter.FeedDisplayFilterPipelineTest --tests com.github.zly2006.zhihu.viewmodel.filter.FeedContentFilterPipelineTest :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverJvmTestSourceSet
```

Result:

- Boundary grep found no Android platform imports in shared foreground/filter code. Matches under `shared/login` and Room coroutine context were unrelated string matches.
- Targeted JVM/shared/desktop compile, JVM tests, and ktlint format tasks passed.

Known existing blocker:

- Full Android compile still has broader `shared:compileAndroidMain` migration debt unrelated to this slice, so this slice was verified with the narrow shared/JVM/desktop checks above.
