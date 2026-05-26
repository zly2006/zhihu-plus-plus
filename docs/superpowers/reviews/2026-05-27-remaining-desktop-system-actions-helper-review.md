# Remaining Desktop System Actions Helper Review

## Input

- Reviewer: `gpt-5.5 xhigh` subagent `019e666b-e872-7a30-91e8-f1b9a248c87c`.
- Target files:
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/ArticleActionsRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/SystemAndUpdateSettingsRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/DeveloperSettingsRuntime.jvm.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/DesktopZhihuMain.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopSystemActions.kt`

## Conclusion

The remaining direct desktop browser and clipboard calls are JVM-only platform side effects. They can reuse the existing JVM helper without moving behavior into `commonMain` and without changing UI, navigation, or runtime interfaces.

## Evidence

- Article actions only need the system browser side effect after `articleUrl(article)` is produced.
- System/update settings own update state transitions and blank-download-link errors; only the private browser-opening implementation is platform IO.
- Developer settings own signed GET and response return order; only clipboard writing is platform IO.
- Desktop main video route owns navigation and video URL lookup; only final browser opening is platform IO.

## Master Similarity

Keep existing function names and state/message order:

- `openArticleInBrowser()` shows `已发送到浏览器` only after the helper reports the browser was opened.
- `openDesktopUrl()` keeps the `下载链接为空` blank URL error and stays inside the existing `downloadUpdate` `runCatching` flow.
- `signedGetAndCopy` still fetches the body, copies it, then returns the body.
- Desktop video navigation still reports unknown content and URL fetch failures in the same branches.

## Minimal Steps

1. Import `openDesktopExternalUrl` or `copyDesktopPlainText` in the four JVM runtime files.
2. Replace only the direct `Desktop.getDesktop().browse(...)` and AWT clipboard writes.
3. Delete now-unused `java.awt.*`, `StringSelection`, and `URI` imports.
4. Leave common runtime interfaces, UI, navigation, and helper signatures unchanged.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverJvmMainSourceSet --continue
git diff --check
rg -n "Desktop\\.getDesktop\\(\\)\\.browse|Toolkit\\.getDefaultToolkit\\(\\)\\.systemClipboard|StringSelection" shared/src/jvmMain -g '*.kt'
rg -n "java\\.awt|openDesktopExternalUrl|copyDesktopPlainText" shared/src/commonMain shared/src/androidMain app/src/main desktopApp/src/main -g '*.kt'
```
