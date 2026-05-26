# People Profile Runtime Boundary Review

## Input

- Target: duplicated `PeopleScreenRuntime` profile and follow-result semantics.
- Files reviewed:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PeopleScreenRuntime.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/PeopleScreenRuntime.android.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PeopleScreenRuntime.jvm.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PeopleScreen.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/people/PeopleScreenUiState.kt`
  - `master:app/src/main/java/com/github/zly2006/zhihu/ui/PeopleScreen.kt`

## Conclusion

The profile include string, `DataHolder.People` to `PeopleProfileLoadResult` mapping, and follow response fallback parsing are shared API/UI-state semantics. Platform runtimes should keep only request execution, signing, history recording, blocklist lookup, messages, WebView/image opening, and desktop browser behavior.

## Evidence

- Android and JVM runtimes currently duplicate the same profile include string.
- Android and JVM runtimes map the same `DataHolder.People` fields into the same `PeopleProfileUiState` fields.
- Android and JVM follow toggles both parse `follower_count` from response JSON and fall back to `followerCount +/- 1` based on previous follow state.

## Master Similarity

Keep the platform `toggleFollow` and `toggleBlock` lambdas in their current order: choose DELETE/POST from the previous state, sign and execute the platform request, validate status, parse the result, then return the new UI state. Do not change the common People page button UI or call order.

## Minimal Steps

1. Add a common `peopleProfileIncludePath`.
2. Add a common `toPeopleProfileLoadResult(loadedPerson, isBlockedInRecommendations)` helper.
3. Add a common `peopleFollowResult(isFollowingBefore, followerCountBefore, responseJson)` helper.
4. Keep Android/JVM request execution and side effects in platform runtime files.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
rg -n "allow_message,is_followed|follower_count\"\\]|PeopleProfileUiState\\(" shared/src/commonMain shared/src/androidMain shared/src/jvmMain -g '*.kt'
rg -n "AccountData|DesktopAccountStore|OpenImageDialog|Desktop\\.getDesktop|postHistoryDestination|getBlocklistManager|createBlocklistManager" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/people -g '*.kt'
git diff --check
```
