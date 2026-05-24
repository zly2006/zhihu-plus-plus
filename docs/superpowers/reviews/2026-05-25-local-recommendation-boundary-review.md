# Local Recommendation Boundary Review

Date: 2026-05-25
Reviewer: gpt-5.5 xhigh subagent `019e5b41-3ee4-7462-a847-33db93b80b93`

## Scope

Target lane: desktop/JVM home local recommendation opened/feedback parity.

Primary files:

- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreenRuntime.jvm.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreenRuntime.android.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreen.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreenRuntime.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/HomeFeedInteractionViewModel.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/LocalHomeFeedViewModel.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/LocalRecommendationEngine.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/UserBehaviorAnalyzer.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/LocalContentCache.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/LocalContentDao.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/LocalContentDatabase.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.android.kt`

## Conclusion

Do not implement JVM `recordLocalItemOpened` / `recordLocalItemFeedback` as direct database writes in the current runtime. That would report success in common UI without matching Android local recommendation behavior.

Reasons:

- JVM `HomeScreenRuntime` currently always uses `HomeFeedViewModel`, not a local recommendation ViewModel.
- Android switches ViewModel by `RecommendationMode` and only returns feedback success when the active ViewModel is `LocalHomeFeedViewModel`.
- Android feedback goes through `LocalRecommendationEngine`, updates `local_feeds.userFeedback`, inserts behavior records, and removes disliked items from the current list.
- Common UI text depends on the boolean feedback result, so a JVM half-implementation would mislead users.

## Ownership

Shared ownership:

- `FeedDisplayItem.localContentId`, `localFeedId`, `localReason`
- Local recommendation opened/like/dislike semantics
- Reason preference and content affinity scoring
- Candidate ranking, diversity, entity, DAO, and Room database definitions

Platform ownership:

- Android `Context`, `AlertDialog`, `Log`, `ConnectivityManager`, `AccountData.fetchGet`, Android database builder inputs
- JVM database file path, desktop browser/clipboard/message sinks

Needs split:

- `LocalRecommendationEngine`
- `UserBehaviorAnalyzer`
- `FeedGenerator`
- `TaskScheduler`

## Recommended Slice

First migrate the `UserBehaviorAnalyzer` core to `shared/commonMain` by making it depend on `LocalContentDao` instead of Android `Context`.

Minimum implementation:

- Move `UserBehaviorAnalyzer` body to common.
- Constructor takes `LocalContentDao`.
- Android keeps a thin factory from `Context` to `getLocalContentDatabase(context).contentDao()`.
- Add JVM test coverage for click/like/dislike behavior records and profile multipliers.

Do not yet make JVM `HomeScreenRuntime` return `true` for local feedback or switch to local recommendation mode until `LocalRecommendationEngine` and `LocalHomeFeedViewModel` are migrated or have equivalent JVM adapters.

## Validation

Recommended commands:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:jvmTest :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet
git diff --check
```

Boundary grep:

```bash
rg -n "recordLocalItemOpened|recordLocalItemFeedback|localContentId|localFeedId|localReason|LocalHomeFeedViewModel|LocalRecommendationEngine|UserBehaviorAnalyzer|getLocalContentDatabase" shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/jvmMain/kotlin app/src desktopApp/src -g '*.kt'
rg -n "android\\.|\\bContext\\b|ConnectivityManager|Toast|AlertDialog|Log\\.|AccountData|signFetchRequest|Room\\.databaseBuilder|Dispatchers|CoroutineScope|viewModelScope|java\\.awt|DesktopAccountStore" shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/jvmMain/kotlin -g '*.kt'
rg -n "LocalRecommendationEngine|UserBehaviorAnalyzer|recordContentOpened|recordRecommendationFeedback|updateFeedFeedback|insertBehavior|getBehaviorsSince|getMostLikedContent|buildBehaviorProfile|generateRecommendations" shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/jvmMain/kotlin shared/src/*Test/kotlin -g '*.kt'
```
