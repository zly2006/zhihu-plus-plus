# Content Filter Blocklist Boundary Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/BlocklistManager.kt`
- `app/src/main/java/com/github/zly2006/zhihu/nlp/BlockedKeywordRepository.kt`
- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- feed callers in `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/` and `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/za/`
- blocklist UI callers in `BlocklistSettingsScreen`, `BlockByKeywordsDialog`, and `NLPKeywordManagementScreen`
- shared Room DAO/entity/database files under `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/`

## Conclusion

The remaining content-filter work must be split by semantic ownership:

- `BlocklistManager` CRUD, matching, stats, and backup encode/import semantics are shared Room-backed core. Android `Context`, singleton lookup, `Uri`, content resolver, and filesystem export are Android adapters.
- `BlockedKeywordRepository` database CRUD and blocked-record history are shared, but semantic NLP matching is a full/lite variant adapter. Do not move `NLPService`, `SentenceEmbeddingManager`, HanLP, or `sentence_embeddings` into shared.
- `ContentFilterExtensions` owns feed filtering semantics and should move toward shared core, but settings reads, detail fetch/cache, Jsoup HTML parsing, Toast/message display, and NLP matching must be injected as small platform capabilities.

## Subagent

Independent `gpt-5.5 xhigh` boundary review completed and was closed before implementation. It recommended the following order:

1. Slice B1: extract shared `BlocklistService` plus `BlocklistStats`, keep Android import/export facade.
2. Slice B2: extract shared `BlockedKeywordService` plus `KeywordSemanticMatcher`.
3. Slice B3: extend `SettingsStore` for `Float` and move filter settings into a shared snapshot.
4. Slice C: split `ContentFilterExtensions` into shared core and Android dependency bundle, then update feed callers.

## Implementation Decision For Slice B1

Use `git mv` to preserve history:

- Move old Android `BlocklistManager.kt` to `shared/commonMain/.../BlocklistService.kt`.
- Remove `Context`, `Uri`, `File`, `Dispatchers.IO`, and Android database lookup from the shared service.
- Add `shared/androidMain/.../BlocklistManager.kt` as a thin facade that builds `BlocklistService` from `getContentFilterDatabase(context)` and keeps import/export file I/O.
- Keep existing callers on `BlocklistManager.getInstance(context)` for now, so UI and feed call-site churn stays small.

## Risks

- `BlocklistManager` now exists in `shared/androidMain`, not `app`, so app and shared Android callers use the same facade.
- Android build still has broader migration debt in unrelated shared Android screens and ViewModels; this slice should be verified with common/JVM compile, Android source-set compile where possible, and boundary grep.
- Do not merge feed-open history into feed filtering. `ContentOpenEventSupport` remains separate from `ContentFilterExtensions`.

## Verification Commands

Passed:

```bash
rg -n "Context|Uri|contentResolver|getExternalFilesDir|filesDir|Toast|Jsoup|SentenceEmbeddingManager|NLPService" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/BlocklistService.kt
rg -n "class BlocklistManager|class BlocklistService|data class BlocklistStats" app/src shared/src -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabaseTest
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet
git diff --check
```

Known existing blocker:

```bash
./gradlew :shared:compileAndroidMain
```

This still fails in unrelated migration debt such as `AccountSettingScreen`, `ArticleScreen`, `PaginationViewModel`, and `ContentFilterExtensions`. The failure output no longer contains `BlocklistManager` or `BlocklistService` errors.
