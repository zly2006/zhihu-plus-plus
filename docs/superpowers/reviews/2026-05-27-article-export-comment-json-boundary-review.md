# Article Export Comment JSON Boundary Review

Date: 2026-05-27

## Input

- Target files:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/comment/ZhihuCommentJson.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.android.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.jvm.kt`
- Goal: decide whether article export root-comment response parsing can be shared while keeping platform fetch/signing intact.
- Subagent attempt: spawning a new `gpt-5.5 xhigh` reviewer is currently blocked by `agent thread limit reached`, so this document records the local review using the same checklist.

## Conclusion

The root-comment response `data` array to `DataHolder.Comment` list mapping is shared Zhihu API wire-format logic. Android and JVM should keep their current request execution and signing code, but both can call one common decoder after receiving the `JsonObject`.

## Ownership

- Shared:
  - Reading `json["data"]` as a `JsonArray`.
  - Decoding each element as `DataHolder.Comment` with snake_case conversion.
  - Ignoring malformed elements with `runCatching` and applying the requested limit.
- Android/platform:
  - `AccountData.fetchGet()`, Android `Context`, request parameter syntax, and `signFetchRequest()`.
- JVM/platform:
  - `DesktopAccountStore` authenticated fetch, `parameter()` request syntax, and desktop signing.

## Minimal Implementation

Add `decodeZhihuCommentData(json, limit)` in common comment code. Replace only the repeated post-fetch parsing block in Android and JVM `fetchExportComments()`.

## Master Structure Check

The key function `fetchExportComments()` keeps the same body structure:

- Clamp `requestedCount`.
- Return early for zero.
- Fetch `article.rootCommentUrl` with `order`, `limit`, and `include` parameters.
- Sign the request.
- Return an empty list when fetch fails.
- Decode comments and cap to the requested count.

No UI, export HTML/image flow, request parameters, endpoint, signing order, or error behavior changes.

## Risk

Do not move fetch or signing into common. Do not change `safeRequestedCount.coerceAtMost(20)` request limit. The helper must use `ZhihuJson.decodeJson()` rather than raw Kotlin serialization so Android and JVM keep the same snake_case mapping.

## Verification

```bash
rg -n "decodeZhihuCommentData|DataHolder\\.Comment|rootCommentUrl|json\\[\"data\"\\].*Comment" shared/src/commonMain shared/src/androidMain shared/src/jvmMain -g '*.kt'
rg -n "Context|AccountData|DesktopAccountStore|signFetchRequest|parameter\\(|android\\." shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/comment/ZhihuCommentJson.kt -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
