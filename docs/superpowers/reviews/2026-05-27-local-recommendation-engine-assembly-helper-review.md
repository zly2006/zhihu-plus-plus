# Local Recommendation Engine Assembly Helper Review

## Input

- Target: duplicated Android/JVM local recommendation engine assembly.
- Files reviewed:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/LocalRecommendationEngine.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/CrawlingExecutor.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/TaskScheduler.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/LocalContentInitializer.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/local/LocalRecommendationEngine.android.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`

## Conclusion

The assembly of `CrawlingExecutor`, `TaskScheduler`, `LocalContentInitializer`, `FeedGenerator`, `UserBehaviorAnalyzer`, and `LocalRecommendationEngine` is shared orchestration. Platform source sets should provide only database access, authenticated feed fetch, network availability, and logging.

## Evidence

- The engine and its collaborators already live in `shared/commonMain`.
- Their constructors depend on `LocalContentDao` and lambda capabilities, not on Android or JVM runtime types.
- Android and JVM repeated the same construction order while only differing in feed fetch, database builder, network availability, and logs.

## Master Similarity

Keep the current `LocalRecommendationEngine(context)` Android entry and the JVM `createLocalRecommendationEngine()` caller. Preserve engine behavior: initialize content, start scheduling, reuse the same executor for scheduled and on-demand tasks, then run the existing ranking/fallback/diversity flow unchanged.

## Minimal Steps

1. Add `buildLocalRecommendationEngine(...)` in common.
2. Let Android keep `Context`, `AccountData.fetchGet`, `ConnectivityManager`, and `Log` in its wrapper.
3. Let JVM keep desktop database path, signed fetch, and log sink in its caller.
4. Do not move Room builders, UI, Android NLP, or desktop account storage.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationSupportTest --continue
git diff --check
rg -n "Context|ConnectivityManager|AccountData|android.util.Log|java.io.File|Room\\.databaseBuilder|DesktopAccountStore" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local -g '*.kt'
```
