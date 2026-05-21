# BlockedKeywordService Shared Boundary Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/nlp/BlockedKeywordRepository.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/BlockedKeywordService.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/nlp/BlockedKeywordRepository.kt`
- `app/src/main/java/com/github/zly2006/zhihu/nlp/NlpServiceKeywordSemanticMatcher.kt`
- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- full/lite variant `NLPService` implementations under `app/src/full` and `app/src/lite`

## Conclusion

`BlockedKeywordRepository` mixed three concerns:

- shared Room-backed keyword CRUD and blocked-record history;
- weighted text construction and matched-keyword JSON parsing;
- Android/full/lite NLP execution through `NLPService`.

The first two belong in shared. The third must stay as a platform/variant adapter because full variant uses HanLP and `sentence_embeddings`, while lite returns an empty result.

## Implementation

- Moved the old repository implementation with `git mv` to `shared/commonMain/.../BlockedKeywordService.kt`.
- Converted it to `BlockedKeywordService`, taking `BlockedKeywordDao`, `BlockedContentRecordDao`, and a `KeywordSemanticMatcher`.
- Removed Android `Context`, `Dispatchers.IO`, `withContext`, `System.currentTimeMillis()`, and direct `NLPService` references from common code.
- Added `shared/androidMain/.../nlp/BlockedKeywordRepository.kt` as an Android facade preserving the old API for existing callers.
- Added app-side `NlpServiceKeywordSemanticMatcher` so `ContentFilterExtensions` still uses the current full/lite `NLPService` implementation without moving model dependencies into shared.

## Boundaries

Shared:

- keyword CRUD;
- exact/NLP keyword queries;
- NLP weighted-text construction using title and excerpt only;
- blocked content record insertion, limit maintenance, recent record query, deletion, clear;
- matched keyword JSON parsing;
- `KeywordSemanticMatcher` interface.

Platform/variant:

- Android database lookup from `Context`;
- coroutine dispatcher selection for Android call sites;
- full/lite `NLPService`, HanLP, `SentenceEmbeddingManager`, and `sentence_embeddings`;
- any future desktop NLP implementation.

## Verification

Passed:

```bash
rg -n "Context|Uri|contentResolver|getExternalFilesDir|filesDir|Toast|Jsoup|SentenceEmbeddingManager|NLPService|android\\.|Dispatchers|withContext|System\\.currentTimeMillis" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/BlockedKeywordService.kt
rg -n "class BlockedKeywordRepository|class BlockedKeywordService|KeywordSemanticMatcher|NlpServiceKeywordSemanticMatcher" app/src shared/src -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabaseTest
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet
git diff --check
```

Expected remaining work:

- `ContentFilterExtensions` still owns Android setting reads, Jsoup parsing, Toast, and detail-cache dependency.
- `SettingsStore` still needs `Float` support before `nlpSimilarityThreshold` can move into a shared filter setting snapshot.
- `./gradlew :shared:compileAndroidMain` remains blocked by broader KMP migration debt outside this slice, including `AccountSettingScreen`, `ArticleScreen`, `PaginationViewModel`, and `ContentFilterExtensions`; the failure output did not include `BlockedKeywordRepository` or `BlockedKeywordService`.
