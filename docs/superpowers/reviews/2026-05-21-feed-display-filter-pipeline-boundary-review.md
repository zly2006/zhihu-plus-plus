# Feed Display Filter Pipeline Boundary Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedDisplayFilterPipeline.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedContentFilterPipeline.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedFilterSettings.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/BlockedFeedRecordDao.kt`

Subagent: `019e48dc-3d47-7d11-b6ed-5ee632773897` (`gpt-5.5`, `xhigh`) completed before implementation.

## Conclusion

`applyContentFilterToDisplayItems` should be split into a shared feed display filtering pipeline plus an Android wrapper. The whole Android `ContentFilterExtensions.kt` file still should not be moved yet because it retains `Context`, `SharedPreferences`, `ContentDetailCache`, Toast, Android logging, and Android NLP matcher setup.

Shared ownership:

- followed-user bypass and `filterFollowedUserContent` behavior;
- `reverseBlock` behavior and `AdvertisementFeed` inclusion;
- ad/paid-content filtering through `FeedAdFilter`;
- `FeedDisplayItem` to `FilterableContent` mapping and raw content copy-back;
- blocked feed record construction and persistence through common KMP Room DAO;
- details post-filter keywords (`感兴趣`, `购买`) while preserving the current no-history behavior.

Android ownership:

- setting source (`SharedPreferences`);
- detail fetch/cache adapter (`ContentDetailCache`);
- Room builder/file path;
- Toast/main-thread dispatch;
- Android full/lite NLP implementation and logging.

## Implementation

- Added shared `ContentDetailProvider`.
- Added shared `FeedDisplayFilterPipeline`.
- Added common `saveBlockedFeedRecords(BlockedFeedRecordDao, ...)`.
- Changed Android `ContentFilterExtensions.applyContentFilterToDisplayItems` to construct the shared pipeline from Android adapters.
- Kept foreground read filtering and display/interaction recording in Android for a later slice.
- Added JVM tests for raw copy-back, blocked-feed history persistence, followed-user bypass, reverse-block behavior, and details post-filter no-history behavior.

## Verification

```bash
rg -n "android\\.|Context|SharedPreferences|Toast|Log\\.|MainActivity|AccountData|getContentDetail" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
rg -n "ContentFilterManager|ContentFilterExtensions" app/src/main/java shared/src/androidMain/kotlin shared/src/commonMain/kotlin -g '*.kt'
./gradlew :shared:jvmTest :desktopApp:compileKotlin :app:compileLiteDebugKotlin :app:testLiteDebugUnitTest
```

Known existing blocker:

- Android compilation still fails before this slice on broader `shared:compileAndroidMain` migration debt (`AccountSettingScreen`, `ArticleScreen`, `PaginationViewModel`/feed ViewModels). This slice is validated with shared/JVM targeted tests until that debt is removed.
