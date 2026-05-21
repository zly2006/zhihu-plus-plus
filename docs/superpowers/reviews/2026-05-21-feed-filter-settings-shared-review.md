# Feed Filter Settings Shared Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/FeedFilterSettings.kt`

This slice is within the planned `ContentFilterExtensions` shared split. No new unclear dependency was introduced; the Android-only API remains `SharedPreferences`.

## Conclusion

Feed filter settings are shared policy state. Android only owns how those values are read today. Moving the settings shape to common makes later shared filtering orchestration depend on a plain snapshot instead of platform storage.

## Implementation

- Added shared `FeedFilterSettings`.
- Kept existing Android public helper functions but backed them through the shared settings snapshot.
- Replaced internal direct `SharedPreferences` reads for reverse block, followed-user filtering, keyword/NLP/user/topic flags, NLP threshold, topic threshold, and ad settings with one Android adapter: `toFeedFilterSettings()`.

## Boundaries

Shared:

- setting names as semantic fields;
- default values;
- nested ad-block policy settings.

Still Android:

- `SharedPreferences` storage and key reads;
- Android `Context` ownership;
- filtering side effects, database access, Toast/log, NLP matcher wiring.

## Verification

```bash
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.FeedAdFilterTest :shared:runKtlintFormatOverCommonMainSourceSet
git diff --check
```
