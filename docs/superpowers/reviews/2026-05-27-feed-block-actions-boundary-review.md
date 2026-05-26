# Feed Block Actions Boundary Review

## Input

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components/FeedBlockActions.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/FeedBlockActions.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/components/FeedBlockActions.jvm.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/BaseFeedViewModelAndroidExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/BaseFeedViewModel.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/data/ContentDetailCache.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/ContentDetailCache.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`
- Callers: `HomeScreen.kt`, `FollowScreen.kt`

Review performed by `gpt-5.5 xhigh` subagent `019e658c-27cd-75f3-85a2-00fe98ab7834`.

## Conclusion

`FeedBlockActions.kt` and the dialog UI structure belong in `shared/commonMain`. Android and JVM actual files should remain platform runtime adapters for `Context`, database entry points, desktop file paths, desktop network fetch, and user messages.

The repeated business logic should move to common:

- resolving a feed author for user blocking;
- resolving title/summary/content for keyword blocking;
- removing current displayed items for a blocked topic.

These helpers should depend only on `FeedDisplayItem`, `DataHolder`, `ContentDetailProvider`, and `BaseFeedViewModel.displayItems`. They must not depend on Android `Context`, desktop `File`, platform message sinks, or UI state.

## Evidence

- Android keeps `master`-shaped `handleBlockUser`, `handleBlockByKeywords`, and `handleBlockTopic` extension functions, but their private author/content/topic logic is pure feed semantics.
- JVM reimplemented the same author/content/topic semantics in `FeedBlockActions.jvm.kt`, causing Android/JVM drift.
- Common filtering already has `ContentDetailProvider`, and common feed filtering already has `extractTopicIds(raw)`.
- Android `ContentDetailCache.android.kt` already supplies platform detail fetch for `Article`, `Question`, and `Pin`.
- JVM already has desktop detail fetchers for article, question, and pin; they can be reused as platform provider functions.

## Minimum Migration

1. Add a common helper for feed block action semantics.
2. Keep `rememberFeedBlockActions()` and all screen call sites unchanged.
3. Keep Android extension function names and branch structure.
4. Make JVM actual call the same common helper and reuse existing desktop detail fetchers.
5. Do not change dialog UI, visible text, navigation, or screen layout.

## Risks

- Android `handleBlockUser` and `handleBlockByKeywords` currently run detail fallback inside `Dispatchers.IO`; keep that wrapper in the Android extension.
- Desktop provider must not pull Android APIs into JVM source set.
- The common helper must not become a UI component or a page-level abstraction.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
git diff --check
rg -n "ensureAuthorInfo|ensureContentForKeywordBlocking|handleBlockUser\\(|handleBlockByKeywords\\(|handleBlockTopic\\(|removeAll \\{" shared/src/androidMain/kotlin shared/src/jvmMain/kotlin shared/src/commonMain/kotlin -g '*.kt'
rg -n "ContentDetailProvider|ContentDetailCache\\.getOrFetch|fetchDesktopQuestionDetail|question\\.topics" shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/jvmMain/kotlin -g '*.kt'
```
