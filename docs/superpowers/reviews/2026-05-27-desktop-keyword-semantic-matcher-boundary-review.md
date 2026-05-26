# Desktop Keyword Semantic Matcher Boundary Review

## Input

- Target: duplicated JVM desktop keyword semantic matcher and tokenizer.
- Files reviewed:
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/components/FeedBlockActions.jvm.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/BlockedKeywordService.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedDisplayFilterPipeline.kt`
  - Android NLP adapter call sites under `app` and `shared/src/androidMain`

## Conclusion

The lightweight lexical matcher is JVM-only desktop fallback behavior. It should be shared inside `jvmMain`, not moved to `commonMain`. Common owns the `KeywordSemanticMatcher` injection contract and filtering services; Android full/lite continue to inject their own NLP implementation through Android platform adapters.

## Evidence

- `DesktopPaginationEnvironment` and `FeedBlockActions.jvm` used identical token-overlap matcher logic.
- `BlockedKeywordService` only consumes a `KeywordSemanticMatcher`; it does not own a concrete platform NLP implementation.
- Android binds `AndroidContentFilterRuntime.semanticMatcher` from app/full/lite NLP adapters, so using the desktop fallback as common default would weaken variant-specific behavior.

## Master Similarity

Keep current calling structure: `DesktopPaginationEnvironment.applyHomeFeedFilters()` still injects the matcher into feed filtering, and `createDesktopBlockedKeywordService()` still injects the matcher when constructing `BlockedKeywordService`. Do not move database creation, service creation, feed filtering, UI, or Android NLP wiring.

## Minimal Steps

1. Add a JVM helper containing only `desktopKeywordSemanticMatcher` and its private token extraction.
2. Import that helper from the two existing JVM call sites.
3. Delete only the duplicate private matcher/tokenizer blocks.
4. Keep the helper out of common and out of Android source sets.

## Verification

```bash
rg -n "desktopKeywordSemanticMatcher|extractDesktopSemanticTokens" shared/src/jvmMain shared/src/commonMain shared/src/androidMain app/src -g '*.kt'
rg -n "NLPService|sentence_embeddings|HanLP|onnxruntime|android\\.|Context|Toast|WebView" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverJvmMainSourceSet --continue
git diff --check
```
