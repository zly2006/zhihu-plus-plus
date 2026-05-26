# Block User Confirm Runtime Boundary Review

## Scope

Review `BlockUserConfirmDialog` before replacing the whole-dialog `expect` / `actual` wrapper with a common dialog and a small platform runtime.

## Conclusion

`BlockUserConfirmDialog` belongs in `shared/commonMain`. Its UI body already delegates to common `BlockUserConfirmDialogContent`, and the Android/JVM actual implementations only differ in blocklist persistence setup. The common wrapper should own the current dialog call shape, coroutine launch, success message, failure log, and failure message; platform source sets should only provide `blockUser(author)`.

Android-only side effects remain `LocalContext` and `getBlocklistManager(context)`. JVM-only side effects remain desktop content-filter database file setup and `createBlocklistManager()`.

## Master Shape

The migrated wrapper keeps the existing call signature and UI structure:

- `BlockUserConfirmDialog(showDialog, userToBlock, displayItems, onDismiss, onConfirm)`.
- `BlockUserConfirmDialogContent` keeps its current `AlertDialog`, title, body text, confirm and cancel actions.
- Confirm action still writes the blocked user, calls `onConfirm()`, and then shows `已屏蔽用户：...`.
- Failure still logs `Failed to block user` and shows `屏蔽用户失败: ...`.

No navigation callback or page-specific adapter is introduced.

## Validation Plan

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
git diff --check
rg -n "expect fun BlockUserConfirmDialog|actual fun BlockUserConfirmDialog" shared/src -g '*.kt'
rg -n "LocalContext|Context|java\\.io\\.File|getBlocklistManager|createBlocklistManager" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components -g '*.kt'
```
