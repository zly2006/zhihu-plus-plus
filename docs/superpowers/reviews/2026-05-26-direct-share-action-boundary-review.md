# Direct Share Action Boundary Review

## Scope

Review duplicated direct share action handling before moving `shareActionMode` branching into common code.

## Conclusion

The `ask` / `copy` / `share` interpretation of `shareActionMode` belongs in `shared/commonMain`; platform source sets should only provide the concrete share and copy side effects. Android keeps `Intent`, `ClipData`, clipboard destination and message side effects. JVM keeps AWT clipboard and desktop message side effects.

## Master Shape

The common helper preserves current direct action behavior:

- `ask` and unknown values call `onShowDialog()`.
- `copy` copies `getShareText(content)` and shows `已复制链接`.
- Android direct `share` keeps `Intent.ACTION_SEND`, `EXTRA_TEXT`, `EXTRA_TITLE`, chooser title `分享到`, and `FLAG_ACTIVITY_NEW_TASK`.
- JVM direct `share` keeps the desktop fallback behavior: copy text and show `已复制分享文本`.
- Dialog UI continues to use `ShareDialogContent`; the dialog share button still uses the lighter dialog `share` side effect and does not gain `EXTRA_TITLE`.

## Validation Plan

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
git diff --check
rg -n "private fun handleDesktopShareAction|fun handleShareAction\\(|getSharedPreferences\\(PREFERENCE_NAME|shareActionMode" shared/src -g '*.kt'
rg -n "android\\.|java\\.awt|Toolkit|StringSelection|Intent|ClipData|Toast|SharedPreferences|getSharedPreferences|ACTION_SEND|EXTRA_TEXT|EXTRA_TITLE|startActivity" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/platform -g '*.kt'
```
