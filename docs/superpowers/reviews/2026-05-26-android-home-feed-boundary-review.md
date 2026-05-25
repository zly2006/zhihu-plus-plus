# Android Home Feed Boundary Review

Date: 2026-05-26

Reviewer: independent gpt-5.5 xhigh subagent

## Input

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/za/AndroidHomeFeedViewModel.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/za/MixedHomeFeedViewModel.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/HomeFeedViewModel.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/BaseFeedViewModel.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.kt`
- Android/JVM `PaginationEnvironment` implementations
- Calls around `ContentFilterExtensions`, `ContentDetailProvider`, `AccountData`, Android headers/cookies, `resolveContent`, and `FeedDisplayItem` navigation JSON.

## Conclusion

`AndroidHomeFeedViewModel` is not Android-only. It currently mixes shared business logic with platform side effects.

Shared ownership:

- Mobile recommendation `ComponentCard` JSON parsing.
- `route_url` to `resolveContent` mapping.
- `Article` title, author, and avatar enrichment.
- `FeedDisplayItem(navDestinationJson = ...)` construction.
- Paging order and display/filter ordering.

Platform ownership:

- Android `Context`.
- Android SharedPreferences and cookie persistence.
- Toast and Android `Log`.
- Android `ContentDetailCache.getOrFetch(context, ...)`.
- Android Room database builder/file path.
- Android full/lite NLP matcher injection.

`MixedHomeFeedViewModel` is semantically shared, but should move only after the mobile recommendation delegate has a common body or a minimal platform adapter. Preserve the two-delegate `android` / `web` structure and shared `displayItems` shape.

## Master Shape

When migrating `AndroidHomeFeedViewModel`, compare with `master:app/src/main/java/com/github/zly2006/zhihu/viewmodel/za/AndroidHomeFeedViewModel.kt` and preserve:

- `httpClient`
- `joStrMatch`
- `fetchFeeds`
- `recordContentInteraction`
- `onUiContentClick`

Do not reorder the main `fetchFeeds` body: HTTP fetch, `ComponentCard` loop, route/footer/author parsing, foreground display, background filtering/removal, `lastPaging`, catch/finally.

## Recommended Slices

1. First slice: in `AndroidHomeFeedViewModel`, replace the duplicated content-filter block with `environment.applyHomeFeedFilters(itemsToDisplay)`. Do not move files and do not change UI.
2. Second slice: move `ComponentCard -> FeedDisplayItem` parsing into common code with JVM tests for `route_url`, footer, author, and `navDestinationJson`.
3. Third slice: move `MixedHomeFeedViewModel` to common after the mobile delegate boundary is common enough, then make JVM `HomeScreenRuntime` respect `RecommendationMode`.

## Validation

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:jvmTest :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
rg -n "ContentFilterExtensions.apply.*DisplayItems|android\\.content|android\\.util|android\\.widget|Context|Toast|Log\\.|AccountData|ContentDetailCache|PREFERENCE_NAME|getSharedPreferences" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/za
rg -n "RecommendationMode|viewModel<HomeFeedViewModel>" shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreenRuntime.jvm.kt
```

## Risks

- Lifecycle/threading: `viewModelScope`, `Dispatchers.Main`, and display ordering.
- Persistence/database: SharedPreferences, account cookies, Room filter database, and desktop file-backed stores.
- Network/serialization: Android headers, user agent, cookies, `lastPaging`, Zhihu JSON conventions, and `navDestinationJson`.
- Navigation: keep shared `resolveContent` / `NavDestination`; do not introduce navigation lambdas.
- Theme state is not involved in this slice.
