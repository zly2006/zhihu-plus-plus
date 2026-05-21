# ContentType Shared Boundary Review

Date: 2026-05-21

## Input

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `app/src/main/java/com/github/zly2006/zhihu/navigation/AndroidAnswerNavigatorRepository.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/HomeFeedViewModel.kt`
- content-open tests under `app/src/test` and `app/src/androidTest`

## Conclusion

`ContentType` is shared content identity vocabulary. It is used by feed filtering, content-open tracking, answer navigation, and tests; it has no Android dependency and should not live inside Android `ContentFilterExtensions`.

## Implementation

- Added `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentType.kt`.
- Removed the nested `ContentType` object from Android `ContentFilterExtensions.kt`.
- Kept the existing package and constants to avoid call-site churn.

## Verification

```bash
rg -n "object ContentType|ContentType\\." app/src shared/src -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabaseTest
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet
git diff --check
```
