# Share Dialog Runtime Boundary Review

## Scope

Review `QuestionShareDialog` / `PinShareDialog` whole-page `expect` / `actual` wrappers and Android `ShareDialogComponent` before moving the dialog wrapper to `shared/commonMain`.

## Conclusion

The share sheet visual structure belongs in `shared/commonMain`: it already uses common `ShareDialogContent`, common `NavDestination`, common `getShareText` / `getShareTitle`, and shared `LocalNavigator` for the settings route. Android and JVM should only provide the small platform side effects for sharing and copying.

Android-only side effects remain `Context`, `Intent.ACTION_SEND`, `ClipData`, clipboard access, `articleHost()?.clipboardDestination`, Android message sink, and `shareActionMode` storage used by the direct share action. JVM-only side effects remain AWT clipboard and desktop message sink behavior.

## Master Shape

The migrated wrapper keeps the same dialog body and click order from `master`:

- `ShareDialog` receives `content`, `shareText`, `showDialog`, and `onDismissRequest`.
- The sheet keeps the same three actions in order: share, copy link, share settings.
- Each action dismisses first, then performs its side effect.
- Settings navigation stays inside shared UI through `LocalNavigator.current.onNavigate(Account.AppearanceSettings(setting = "shareAction"))`.

The direct `handleShareAction` path is intentionally left as the Android/JVM platform path in this slice so `shareActionMode` behavior is not broadened or rewritten.

## Validation Plan

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
git diff --check
rg -n "expect fun (QuestionShareDialog|PinShareDialog)|actual fun (QuestionShareDialog|PinShareDialog)" shared/src
rg -n "android\\.content|java\\.awt|Toolkit|LocalContext|Context" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components -g '*.kt'
rg -n "shareActionMode|clipboardDestination|ACTION_SEND|AppearanceSettings\\(setting = \"shareAction\"\\)" shared/src -g '*.kt'
```
