# Collection Create Payload Boundary Review

Date: 2026-05-27

## Input

- Target files:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/CollectionContentViewModel.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModel.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/util/OpenInBrowser.kt`
- Goal: decide whether the create-collection JSON body can be shared between article collection creation and the Android open-in-browser collection helper.
- Subagent attempt: spawning new `gpt-5.5 xhigh` reviewers is currently blocked by `agent thread limit reached`, so this document records the local review using the same checklist.

## Conclusion

The JSON payload shape for creating a Zhihu collection is shared API contract logic. Network execution, authentication, signing, and the Android-only open-in-browser side effect stay in their existing adapters.

## Ownership

- Shared:
  - `title`, `description`, and `is_public` request field names.
  - The `buildJsonObject` payload construction order.
  - Existing collection endpoint helpers.
- Android/platform:
  - `Context`.
  - `AccountData.fetchPost()` and `ArticleViewModelRuntime.fetchPost()`.
  - `signFetchRequest()` / `configureSignedRequest()`.
  - The Android-only browser-opening collection side effect.

## Minimal Implementation

Add `zhihuCollectionCreateBody(title, description, isPublic)` near the existing collection URL helpers in common code. Replace only the duplicated inline `buildJsonObject` blocks in:

- `ArticleViewModel.createNewCollection()`
- `OpenInBrowser.openUrlInBrowser()`

Keep all function names, request order, content type setup, signing, and follow-up calls unchanged.

## Risk

Do not move `OpenInBrowser` itself to common. Its behavior depends on Android account state and the Android browser/favorite collection side effect. This slice only shares the request body contract.

## Verification

```bash
rg -n "zhihuCollectionCreateBody|is_public|Zhihu\\+\\+: 要在浏览器中打开的内容|com.github.zly2006.zhplus.openinbrowser" shared/src/commonMain shared/src/androidMain -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
