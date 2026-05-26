# Article Content Detail JSON Normalization Boundary Review

## Input

- Target: duplicated Article/Answer content detail JSON top-level `id` normalization.
- Files reviewed:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/data/ContentDetailCache.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/DataHolder.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/DataHolder.kt`
  - `master:app/src/main/java/com/github/zly2006/zhihu/data/DataHolder.kt`

## Conclusion

Article/Answer content detail JSON `id` normalization is shared wire-format parsing semantics. Platform code should keep request execution, signing, account/session access, HTTP client configuration, decode runtime, logging, and error handling.

## Evidence

- `DataHolder.Answer.id` and `DataHolder.Article.id` are `Long`.
- Android and two JVM detail fetch paths all normalize the top-level `id` before decoding Article/Answer detail.
- Pin detail must not use this helper because `DataHolder.Pin.id` is a `String`.

## Master Similarity

Keep the existing order: build detail URL, execute signed GET, normalize detail JSON, decode by `ArticleType`, and preserve current error handling. Do not change UI, navigation, ViewModel call order, or platform network adapters.

## Minimal Steps

1. Add `normalizeArticleContentDetailJson(jo)` in common.
2. Keep `normalizeQuestionDetailJson(jo)` and share a private long-id helper internally.
3. Replace only the Android Article detail fetch and the two JVM Article detail fetch call sites.
4. Do not apply this helper to Pin, feed/list, or unrelated API responses.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
git diff --check
rg -n "JsonPrimitive\\(value\\.jsonPrimitive\\.long\\)|if \\(key == \"id\"\\)" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/DataHolder.kt shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.jvm.kt shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt
rg -n "Context|AccountData|DesktopAccountStore|HttpMethod|signFetchRequest|signZhihuFetchRequest|fetchAuthenticatedJson|fetchGet|android\\." shared/src/commonMain/kotlin/com/github/zly2006/zhihu/data/ContentDetailCache.kt
```
