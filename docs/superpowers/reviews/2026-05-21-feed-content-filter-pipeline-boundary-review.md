# Feed Content Filter Pipeline Boundary Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedFilterContent.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedFilterSettings.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/BlocklistService.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/BlockedKeywordService.kt`

Subagent: `019e48cb-7a10-7a12-bb56-fa1ba9eb1b1f` (`gpt-5.5`, `xhigh`) completed before implementation.

## Conclusion

The `filterContents` orchestration should move to `shared/commonMain`, but the whole Android `ContentFilterExtensions.kt` file should not be moved yet. The file still owns Android settings access, `Context`, `ContentDetailCache`, Toast/log/main-thread delivery, and Android variant NLP wiring.

Shared ownership:

- author block filtering;
- exact/regex/case keyword filtering through `BlocklistService`;
- NLP blocked-record database semantics through `BlockedKeywordService`;
- topic block filtering and blocked reason assembly;
- filter ordering and blocked-content result collection.

Platform ownership:

- `Context`, `SharedPreferences`, Room builder/file path, detail fetch/cache, Toast/log/main-thread delivery;
- Android full/lite NLP implementation and `NlpServiceKeywordSemanticMatcher`;
- any future network/cache-backed content detail provider.

## Dependency Notes

- `Jsoup` must not enter common. Shared already has `Ksoup` available and `HTMLDecoder` proves common HTML parsing works, but this slice can avoid adding a parser dependency by injecting an HTML-to-text function from Android.
- Toast should become `UserMessageSink` later. This slice keeps the existing Android Toast behavior behind a callback because current filtering runs from non-Compose ViewModel/background paths.
- `SharedPreferences` should continue shrinking toward `SettingsStore`; this slice uses the already shared `FeedFilterSettings` snapshot.

## Implementation Direction

- Add a shared pipeline helper for author/keyword/NLP/topic rules.
- Inject `BlocklistService`, `BlockedKeywordService`, settings, HTML text extraction, and NLP-block notification callback.
- Keep Android wrapper responsible for constructing services from `getContentFilterDatabase(context)`, passing `NlpServiceKeywordSemanticMatcher`, and showing the existing Toast.

## Verification

```bash
rg -n "android\\.|androidx\\.|Context|SharedPreferences|Toast|Log\\.|Build\\.|ContentDetailCache|org\\.jsoup|Jsoup|NLPService|SentenceEmbeddingManager|getContentFilterDatabase\\(" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
rg -n "ContentFilterExtensions|filterContents|BlocklistManager|getInstance|BlockedKeywordRepository|NlpServiceKeywordSemanticMatcher|ContentDetailCache|Jsoup|Toast\\.makeText|mainExecutor" app/src shared/src -g '*.kt'
./gradlew :shared:jvmTest :desktopApp:compileKotlin :app:compileLiteDebugKotlin :app:testLiteDebugUnitTest
```
