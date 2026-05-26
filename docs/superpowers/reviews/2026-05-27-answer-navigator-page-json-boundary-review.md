# Answer Navigator Page JSON Boundary Review

Date: 2026-05-27

## Input

- Target files:
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/navigation/AndroidAnswerNavigatorRepository.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.jvm.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation/AnswerNavigator.kt`
- Goal: decide whether response `data` and `paging.next` mapping for `AnswerNavigatorPage` can move to common.
- Subagent attempt: spawning the required `gpt-5.5 xhigh` boundary reviewer failed with `agent thread limit reached`, so this document records the local review using the same checklist.

## Conclusion

The page envelope mapping belongs in shared code. The platform network request, authentication, signing, and decode policy stay in platform adapters.

## Ownership

- Shared:
  - `JsonObject["data"]` missing fallback to `AnswerNavigatorPage(emptyList(), "")`.
  - `paging.next` extraction with empty-string fallback.
  - `AnswerNavigatorPage<T>` construction.
- Android:
  - `AccountData.fetchGet(appContext, url)`.
  - Android `signFetchRequest()`.
  - Existing strict `AccountData.decodeJson<List<T>>()` behavior.
- JVM:
  - `DesktopAccountStore` / runtime `fetchGet`.
  - `configureSignedRequest(this)`.
  - Existing per-element lenient decode behavior.

## Minimal Implementation

Add `answerNavigatorPageFromJson(response, decodeItems)` in `AnswerNavigator.kt`. The helper receives a platform-provided decoder so it does not force Android and JVM to use the same failure policy.

Then:

- `AndroidAnswerNavigatorRepository.fetchQuestionFeeds()` and `fetchCollectionItems()` keep their names, URL selection, fetch call, signing call, and strict decoder.
- `DesktopArticleViewModelRuntime.answerNavigatorRepository()` keeps its anonymous repository, `fetchGet`, `configureSignedRequest`, and per-element `runCatching` decode.
- No UI, route registration, answer prefetch queue, or answer-switch state changes.

## Risk

Changing Android to per-element lenient decode or JVM to strict list decode would be a behavior change. The helper must not make that decision; it should only share the envelope mapping.

## Verification

```bash
rg -n "answerNavigatorPageFromJson|jojo\\[\"data\"\\]|paging.*next|JsonArray\\.serializer" shared/src/commonMain shared/src/androidMain shared/src/jvmMain -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
