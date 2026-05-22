# OpenImageDialog KMP Boundary Review

Date: 2026-05-22
Reviewer: gpt-5.5 xhigh subagent Socrates
Scope: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/OpenImageDialog.kt`

## Inputs

- Target file: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/OpenImageDialog.kt`
- Callers checked: Android markdown rendering, people avatar preview, WebView image open, and comment image menu call sites.
- Dependencies checked: Telephoto, Coil, Ktor, Compose, Android dialog/window, save/share/browser helpers.
- Goal: move shareable image preview UI toward `shared/commonMain` while keeping Android UI and behavior unchanged, and without introducing WebView on JVM/desktop.

## Verdict

`OpenImageDialog` is split ownership.

Shared ownership:

- Full-screen black preview UI.
- Click-to-dismiss behavior.
- Long-press menu state, offset, text, order, and layout.
- Haptic suppression around the image viewer.

Platform ownership:

- Android `ComponentDialog`, `ComposeView`, `Window`, `ViewGroup`, and `Context`.
- Save to gallery, share image, and open in browser side effects.
- Android `HttpClient` acquisition through `AccountData.httpClient(context)`.

## Dependency Evidence

Compose UI, Ktor core, and Coil Compose are already available in `commonMain`.

Telephoto needs care: `me.saket.telephoto:zoomable` has common/JVM variants, but the current `me.saket.telephoto:zoomable-image-coil3:0.19.0` artifact is Android-only in available metadata. Therefore the current `ZoomableAsyncImage` call cannot move directly to common. The minimal migration should put the preview shell in common and keep the concrete image renderer as a small slot or platform adapter.

## Risks

- Lifecycle and window hosting remain platform concerns.
- Save/share actions download image bytes and write Android MediaStore or cache files; these must not move to common UI.
- WebView is only a caller source. The migration must not introduce WebView into JVM/common.
- Do not change Android menu text, order, black background, full-screen sizing, click-to-dismiss, long-press menu position, or existing caller behavior.

## Minimum Migration Steps

1. Add a common `OpenImagePreviewContent` composable that owns the black background, haptic suppression, click/long-click wiring, menu placement, and menu item text/order.
2. Pass an image renderer slot into the common composable so Android can keep using `ZoomableAsyncImage`.
3. Keep Android `OpenImageDialog` as a thin `ComponentDialog` host that delegates to common content.
4. Keep Android save/share/open-browser actions in the Android wrapper.
5. Add JVM preview/host support separately only if a caller needs it; do not use WebView.

## Verification

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
rg -n "android\\.|ComponentDialog|ComposeView|Context|Intent|FileProvider|MediaStore|Toast|CustomTabsIntent|WebView|android\\.webkit|androidx\\.webkit|AccountData|saveImageToGallery|shareImage|luoTianYiUrlLauncher|me\\.saket\\.telephoto\\.zoomable\\.coil3" shared/src/commonMain/kotlin -g '*.kt'
rg -n "WebView|android\\.webkit|androidx\\.webkit|javafx|jxbrowser|cef|browser" desktopApp shared/src/jvmMain shared/src/commonMain -g '*.kt'
rg -n "OpenImageDialog\\(|OpenImagePreview|ZoomableAsyncImage|zoomable-image-coil3" shared/src app/src -g '*.kt'
```
