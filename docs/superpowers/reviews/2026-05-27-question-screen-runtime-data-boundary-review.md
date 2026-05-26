# Question Screen Runtime Data Boundary Review

## Input

- Target: duplicated question screen loaded-state mapping and question detail JSON `id` normalization.
- Files reviewed:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/QuestionScreenRuntime.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/QuestionScreenRuntime.android.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/QuestionScreenRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PinScreenRuntime.jvm.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/DataHolder.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/question/QuestionScreenUiState.kt`
  - `master:app/src/main/java/com/github/zly2006/zhihu/ui/QuestionScreen.kt`

## Conclusion

`DataHolder.Question` to `LoadedQuestionScreenData` mapping and question detail JSON `id` normalization are shared semantics. Platform runtimes should keep request execution, signing, history, open-event recording, WebView/browser opening, sharing, and user messages.

## Evidence

- Android and JVM runtimes both construct `Question(question.questionId, questionData.title)` and copy the same `DataHolder.Question` fields into `QuestionScreenUiState`.
- Android content-detail fetch and JVM question fetch paths both normalize the question JSON `id` field before decoding `DataHolder.Question`.
- JVM Pin link-card question preview uses the same question detail endpoint and the same pre-decode normalization.

## Master Similarity

Keep the existing load order: fetch question detail, post/add history, record open event, then return loaded screen data. Do not change the common `QuestionScreen` layout, detail expansion, follow button, share/comment entry points, WebView slot, or visible text.

## Minimal Steps

1. Add common `loadedQuestionScreenData(question, questionData)`.
2. Add common `normalizeQuestionDetailJson(jo)`.
3. Replace only duplicated mapping/normalization calls in Android/JVM runtime adapters.
4. Do not expand this slice to article/pin detail normalization or UI behavior.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
git diff --check
rg -n "QuestionScreenUiState\\(|Question\\(question\\.questionId, questionData\\.title\\)|JsonPrimitive\\(value\\.jsonPrimitive\\.long\\)" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui shared/src/commonMain/kotlin/com/github/zly2006/zhihu -g '*.kt'
rg -n "Context|AccountData|DesktopAccountStore|DesktopHistoryStorage|Desktop\\.|WebviewComp|ContentOpenEventSupport|getContentFilterDatabase|android\\." shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/QuestionScreenRuntime.kt shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/question -g '*.kt'
```
