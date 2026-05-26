# Desktop System Actions Helper Boundary Review

## Input

- Target: repeated JVM desktop browser and clipboard side effects.
- Files reviewed:
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/components/ShareDialogRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/components/SegmentedTextRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreenData.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreenRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/AccountSettingsRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModelRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/markdown/MarkdownRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/CommentScreenRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PeopleScreenRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/QuestionScreenRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PinScreenRuntime.jvm.kt`

## Conclusion

Desktop browser opening and AWT clipboard writes are JVM-only platform side effects. They should be shared within `jvmMain` under `shared.desktop`, while common continues to depend on runtime interfaces and Android keeps its own Intent/clipboard adapters.

## Evidence

- Multiple JVM adapters repeated `Desktop.getDesktop().browse(URI(url))`.
- Multiple JVM adapters repeated `Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)`.
- These calls are platform IO side effects and do not belong in `commonMain`.

## Master Similarity

Keep each existing runtime method and callback order. Callers still own `runCatching`, success/error messages, and any surrounding network or UI state transitions.

## Minimal Steps

1. Add JVM-only `openDesktopExternalUrl(url)` and `copyDesktopPlainText(text)`.
2. Replace only simple one-line browser/clipboard side effects.
3. Leave update, developer, video navigation, and article action flows for separate review because they have specific state or message ordering.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverJvmMainSourceSet --continue
git diff --check
rg -n "Desktop\\.getDesktop\\(\\)\\.browse|Toolkit\\.getDefaultToolkit\\(\\)\\.systemClipboard|StringSelection" shared/src/jvmMain -g '*.kt'
rg -n "java\\.awt|openDesktopExternalUrl|copyDesktopPlainText" shared/src/commonMain shared/src/androidMain app/src/main desktopApp/src/main -g '*.kt'
```
