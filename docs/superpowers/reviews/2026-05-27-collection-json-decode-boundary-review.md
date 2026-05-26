# Collection JSON Decode Boundary Review

Date: 2026-05-27

## Input

- Target files:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/Collection.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModel.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.android.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.jvm.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/util/OpenInBrowser.kt`
- Goal: decide whether Zhihu collection response/list JSON decoding should be owned by common code instead of platform runtimes.
- Subagent attempt: spawning a new `gpt-5.5 xhigh` reviewer failed with `agent thread limit reached`, so this document records the local review using the same checklist.

## Conclusion

Collection JSON decoding is shared API wire-format logic. `Collection` and `CollectionResponse` are common data classes, and common `ZhihuJson.decodeJson()` already performs the same snake_case to camelCase conversion that Android `AccountData.decodeJson()` delegates to.

The platform runtimes should keep only network execution, authentication, signing, cookie/account access, and Android side effects. They do not need a collection-specific decode method.

## Ownership

- Shared:
  - `Collection` / `CollectionResponse` schema.
  - snake_case to camelCase conversion for collection JSON.
  - Decoding `CollectionResponse` and collection `data` lists.
- Android/platform:
  - `Context`, `AccountData.fetchGet()` and `AccountData.fetchPost()`.
  - `signFetchRequest()`, authenticated request execution, and Android open-in-browser side effect.
- JVM/platform:
  - `DesktopAccountStore`, signed fetch, local cookie storage, and desktop side effects.

## Minimal Implementation

- Add common helpers beside `Collection`:
  - `decodeZhihuCollectionResponse(json)`
  - `decodeZhihuCollectionList(json)`
- Make `ArticleViewModel.loadCollections()` call the common response helper directly.
- Remove `decodeCollectionResponse()` from `ArticleViewModelRuntime` and both platform implementations.
- Make Android `OpenInBrowser.openUrlInBrowser()` use the common list helper while preserving `jojo["data"]!!` strictness and the original collection lookup order.

## Master Structure Check

The key function names and body shape remain unchanged:

- `ArticleViewModel.loadCollections()`: keeps content type selection, URL construction, signed GET, collection sorting, `collectionOrder` refresh, and error logging order.
- `OpenInBrowser.openUrlInBrowser()`: keeps account self check, collection list fetch, open-in-browser collection lookup/create fallback, destination type branch, content URL/body construction, and success status check.

This slice only replaces the decode entry point. It does not change UI, navigation, request order, endpoint strings, signing, or side effects.

## Risk

Do not decode through raw `Json.decodeFromJsonElement()` without snake_case conversion. Use `ZhihuJson.decodeJson()` so Android and JVM keep the same field mapping. Do not change `CollectionResponse` requirements for the open-in-browser list path, which intentionally decodes only `data`.

## Verification

```bash
rg -n "decodeCollectionResponse|decodeZhihuCollectionResponse|decodeZhihuCollectionList|decodeJson<List<Collection>" shared/src/commonMain shared/src/androidMain shared/src/jvmMain -g '*.kt'
rg -n "Context|AccountData|DesktopAccountStore|signFetchRequest|android\\." shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/Collection.kt -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
