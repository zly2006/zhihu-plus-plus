# Feed Ad Filter Shared Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedAdFilter.kt`
- `shared/src/commonTest/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedAdFilterTest.kt`

This slice is within the already planned `ContentFilterExtensions` shared split. No new unclear file or dependency was introduced, so no new boundary subagent was started in this slice.

## Conclusion

Ad, paid-content, Zhihu School, Zhihu ad platform, and WeChat official-account detection are shared feed-filter policy. Android only owns the settings source. The policy should accept a plain settings snapshot instead of reading `SharedPreferences`.

## Implementation

- Added shared `FeedAdBlockSettings`.
- Added shared `isFeedAdOrPaidContent(...)`.
- Added shared `getFeedAdBlockReason(...)`.
- Kept Android `SharedPreferences` reads in `ContentFilterExtensions` via a small `toFeedAdBlockSettings()` adapter.
- Added common tests for link-based ads and paid-content settings.

## Boundaries

Shared:

- content-type dispatch over `DataHolder.Answer` / `Article` / `Pin`;
- paid-content reason selection;
- link-based ad reason matching;
- reverse-block ad detection with default enabled settings.

Still Android:

- `SharedPreferences` access;
- `reverseBlock` control-flow source;
- feed detail fetching and blocked-feed record persistence;
- Toast/log/NLP/Jsoup side effects.

## Verification

```bash
rg -n "android\\.|Context|SharedPreferences|Toast|Jsoup|ContentDetailCache|NLPService|Dispatchers|withContext" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedAdFilter.kt shared/src/commonTest/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedAdFilterTest.kt
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.FeedAdFilterTest :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverCommonTestSourceSet
git diff --check
```
