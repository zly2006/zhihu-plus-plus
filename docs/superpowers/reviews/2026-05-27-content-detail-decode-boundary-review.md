# Content Detail Decode Boundary Review

Date: 2026-05-27

## Input

- Target files:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/data/ContentDetailCache.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/DataHolder.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PinScreenRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/QuestionScreenRuntime.jvm.kt`
- Goal: decide whether content-detail JSON decoding can be shared after platform fetch/signing has completed.
- Subagent attempt: spawning a new `gpt-5.5 xhigh` reviewer failed with `agent thread limit reached`, so this document records the local review using the same checklist.

## Conclusion

Article, answer, question, and pin detail JSON decoding is shared wire-format parsing logic. Platform code should keep request execution, account/cookie access, signing, cache lookup, logging, and error handling. Common code can own the typed decode helpers because `DataHolder.*` models and `ZhihuJson.decodeJson()` already live in common.

## Ownership

- Shared:
  - `ArticleType` to `DataHolder.Answer` / `DataHolder.Article` decode selection.
  - Article/answer and question top-level long `id` normalization.
  - Pin detail decode without long-id normalization, because `DataHolder.Pin.id` is a string.
- Android/platform:
  - `Context`, `AccountData.fetchGet()`, `signFetchRequest()`, and Android `Log`.
- JVM/platform:
  - `DesktopAccountStore`, `fetchAuthenticatedJson()`, signed request setup, cache wiring, and local logging/error policy.

## Minimal Implementation

- Add common helpers in `ContentDetailCache.kt`:
  - `decodeArticleContentDetail(article, json)`
  - `decodeQuestionContentDetail(json)`
  - `decodePinContentDetail(json)`
- Replace only repeated decode blocks after successful fetch.
- Keep existing fetch methods, early returns, `runCatching`, cache lookup, request URLs, signing, and logs unchanged.

## Master Structure Check

The affected functions keep their original structure:

- Android `DataHolder.getContentDetail(context, dest/question/pin)`: URL creation, signed GET, `runCatching`, cancellation-aware log, and null fallback remain in place.
- JVM `fetchArticleContentDetail()` / `fetchDesktopArticleContentDetail()` / `fetchDesktopPinDetail()` / question fetch helpers: request construction, `runCatching`, signed fetch, and null fallback remain in place.

This slice replaces only the typed decode expression. It does not change UI, navigation, WebView/Markdown behavior, cache policy, request parameters, or side effects.

## Risk

Do not apply article/question long-id normalization to pin detail. Do not move fetch, signing, cache, logging, or account store access into common. Use `ZhihuJson.decodeJson()` so Android and JVM keep the same snake_case mapping.

## Verification

```bash
rg -n "decodeArticleContentDetail|decodeQuestionContentDetail|decodePinContentDetail|normalizeArticleContentDetailJson|normalizeQuestionDetailJson" shared/src/commonMain shared/src/androidMain shared/src/jvmMain -g '*.kt'
rg -n "Context|AccountData|DesktopAccountStore|HttpMethod|signFetchRequest|signZhihuFetchRequest|fetchAuthenticatedJson|fetchGet|android\\." shared/src/commonMain/kotlin/com/github/zly2006/zhihu/data/ContentDetailCache.kt
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
