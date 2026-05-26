# Pagination Online History Boundary Review

Date: 2026-05-27
Reviewer: main agent local review

## Subagent Status

Attempted to start a `gpt-5.5` `xhigh` boundary-review subagent, but the tool returned `agent thread limit reached`. Per project instructions, this review records the fallback local boundary audit before implementation.

## Scope

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/ZhihuReadHistoryClient.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/ZhihuOnlineHistoryClient.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/AndroidPaginationEnvironment.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`
- `master:app/src/main/java/com/github/zly2006/zhihu/viewmodel/feed/QuestionFeedViewModel.kt`
- `master:app/src/main/java/com/github/zly2006/zhihu/viewmodel/feed/HomeFeedViewModel.kt`

## Conclusion

The mapping from `Feed` targets to Zhihu last-read touch payload rows is shared. Android and JVM currently duplicate the same `AnswerTarget -> ["answer", id, action]`, `ArticleTarget -> ["article", id, action]`, and `PinTarget -> ["pin", id, action]` logic for the `read` action, while `markItemsAsTouched()` repeats the same pair-to-row shape for the `touch` action.

The request execution remains platform-owned. Android keeps `Context`, `AccountData`, Android cookies, `signFetchRequest()`, response status logging, and local `HistoryStorage`. JVM keeps `DesktopAccountStore`, `d_c0`, `signZhihuFetchRequest()`, desktop history storage, and raw response handling.

## Ownership

Shared:

- `ZHIHU_LAST_READ_TOUCH_URL`
- Last-read touch payload row shape.
- `Feed` target to content type/id mapping for supported read status targets.
- `encodeZhihuLastReadTouchItems()`.

Android-only:

- `Context`, `AccountData`, `AccountData.withAuthenticatedResponse()`, Android cookie storage, Android signing wrapper, Android log/message handling.

JVM-only:

- `DesktopAccountStore`, desktop cookies, `d_c0`, `signZhihuFetchRequest()`, `DesktopHistoryStorage`, desktop request wrapper.

## Direct Move

No direct `git mv` is appropriate. The platform environment files mix shared payload semantics with platform networking, signing, history storage, database and logging side effects. The safe step is to keep the environment functions in place and replace only the duplicated payload construction with common helpers.

## Minimum Steps

1. Add common helpers to `ZhihuReadHistoryClient.kt`.
2. Keep `sendFeedReadStatus()` and `markItemsAsTouched()` function names in Android/JVM environments.
3. Replace only the duplicated payload construction.
4. Preserve request URL, POST method, multipart key `items`, `x-requested-with` header, signing calls, status checks and log behavior.
5. Do not change UI, navigation, `HomeFeedViewModel`, `QuestionFeedViewModel`, or any Compose code.

## Validation

```bash
rg -n "AnswerTarget -> listOf\\(\"answer\"|ArticleTarget -> listOf\\(\"article\"|PinTarget -> listOf\\(\"pin\"|zhihuLastReadTouch" shared/src/commonMain shared/src/androidMain shared/src/jvmMain -g '*.kt'
rg -n "Context|AccountData|DesktopAccountStore|signFetchRequest|signZhihuFetchRequest|HistoryStorage|DesktopHistoryStorage|bodyAsText|HttpMethod|MultiPartFormDataContent" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/ZhihuReadHistoryClient.kt
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
git diff --check
```
