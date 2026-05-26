# Pin Link Card Preview Boundary Review

## Input

- Target: `PinScreenRuntime` link-card preview helpers.
- Files reviewed:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PinScreenRuntime.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PinScreen.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/PinScreenRuntime.android.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PinScreenRuntime.jvm.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/pin/PinScreenState.kt`
  - `master:app/src/main/java/com/github/zly2006/zhihu/ui/PinScreen.kt`

## Conclusion

`resolveLinkCardDestination`, compact title/preview text, and the `DataHolder` detail to `PinLinkCardPreview` mapping are shared display semantics. Android and JVM should only keep the platform detail fetch adapters.

Do not move Android `Context`, `AccountData`, request signing, JVM `DesktopAccountStore`, WebView, share runtime, read history, `ContentOpenEvent`, or external URL launch into common.

## Evidence

- `resolveLinkCardDestination`, `compactPreview`, and `compactTitle` already live in common `PinScreen.kt`.
- Android and JVM `fetch*LinkCardPreview` both resolve the same destination types and map `Article`, `Answer`, `Question`, and `Pin` details into the same `PinLinkCardPreview` shape.
- The only necessary platform differences are detail fetch implementations:
  - Android uses `DataHolder.getContentDetail(context, destination)`.
  - JVM uses `DesktopArticleViewModelRuntime`, `fetchDesktopQuestionDetailForFeedBlock`, and `fetchDesktopPinDetail`.

## Master Similarity

Keep `PinScreen`, `PinContent`, link-card loading state, `LaunchedEffect`, click behavior, fallback URL display, and visible text unchanged. The platform wrapper names `fetchAndroidLinkCardPreview` and `fetchDesktopLinkCardPreview` should remain so the migration is a small adapter cleanup rather than a UI rewrite.

## Minimal Steps

1. Add a common typed helper that accepts `ContentLinkCard` and a `suspend (NavDestination) -> DataHolder.Content?` detail provider.
2. Move duplicated preview mapping into that helper.
3. Keep Android/JVM wrapper functions as thin providers.
4. Move duplicated nullable JSON boolean helper into common.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
git diff --check
rg -n "Context|AccountData|DesktopAccountStore|signFetchRequest|java\\.awt|WebView|Toast|SharedPreferences" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
rg -n "fetchAndroidLinkCardPreview|fetchDesktopLinkCardPreview|resolveLinkCardDestination|compactPreview|booleanCompat" shared/src/commonMain shared/src/androidMain shared/src/jvmMain -g '*.kt'
```
