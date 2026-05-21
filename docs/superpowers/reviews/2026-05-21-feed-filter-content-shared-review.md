# Feed Filter Content Shared Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedFilterContent.kt`
- shared data/navigation helpers used by feed filtering

## Conclusion

Feed content identity and filterable content snapshots are shared semantics. They describe what content a feed item represents and how it should be serialized into blocked-feed history; they do not need Android `Context`, Toast, Jsoup, or content-detail fetching.

## Implementation

- Added shared `FilterableContent`.
- Added shared `FeedContentIdentity`.
- Moved `FeedDisplayItem.resolveContentIdentity()`.
- Moved `FeedDisplayItem.toFilterableContent(...)`.
- Moved topic extraction for feed filtering.
- Moved the Json instance used for blocked-feed record snapshots.
- Removed those helpers from Android `ContentFilterExtensions.kt`.

## Boundaries

Shared:

- feed item identity;
- filterable content snapshot;
- raw content to title/author/topic metadata mapping;
- feed/nav JSON snapshot generation.

Still Android in `ContentFilterExtensions`:

- `Context` and `SharedPreferences` settings reads;
- `ContentDetailCache.getOrFetch`;
- Jsoup HTML cleanup before NLP;
- Toast/message display;
- Android database lookup for blocked-feed records;
- full/lite NLP matcher wiring.

## Verification

```bash
rg -n "android\\.|Context|SharedPreferences|Toast|Jsoup|ContentDetailCache|NLPService|Dispatchers|withContext" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedFilterContent.kt
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabaseTest
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet
git diff --check
```

Known remaining blocker:

- `./gradlew :shared:compileAndroidMain` is still expected to fail until broader Android source-set migration debt is removed.
