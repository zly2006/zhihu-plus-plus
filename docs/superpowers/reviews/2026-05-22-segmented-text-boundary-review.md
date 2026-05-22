# SegmentedText KMP Boundary Review

Date: 2026-05-22
Reviewer: gpt-5.5 xhigh subagent Ohm
Scope: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/SegmentedText.kt`

## Inputs

- Target file: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/SegmentedText.kt`
- Related shared model: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/util/SegmentHighlightUtils.kt`
- Related route model: `SegmentCommentHolder` in `NavDestination.kt`
- Callers checked: Android markdown AST/rendering paths using `SegmentedText` and `LocalSegment*` hosts.

## Verdict

`SegmentedText.kt` is split ownership, but the main component belongs in `shared/commonMain`.

Shared ownership:

- Text rendering and virtual underline drawing.
- `AnnotatedString` link construction.
- Segment action sheet UI.
- Selection/action state.
- `SegmentCommentHolder` mapping.

Platform ownership:

- Android `Context`.
- Font/line-height persistence source.
- Clipboard implementation.
- Signed segment reaction network calls through Android account/runtime.

## Minimum Migration Direction

1. Move the component body to `shared/commonMain`.
2. Replace Android preference reads with shared `SettingsStore`.
3. Introduce a small segment runtime for copying text and toggling segment likes.
4. Keep `LocalSegmentCommentHost` shared and keep hosts outside text selection containers.
5. Do not migrate the whole Android markdown renderer in this slice.

## Risks

- The selection hierarchy crash must not be reintroduced; action/comment hosts must stay outside the selectable markdown subtree.
- UI text, button order, icon choices, dashed underline style, and modal behavior should not change.
- Desktop/JVM must not gain WebView dependencies.
- Network failures should preserve current local fallback behavior by retaining the previous metadata when toggling fails.

## Verification

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
rg -n "android\\.|Context|LocalContext|ClipData|SharedPreferences|getSharedPreferences|AccountData|signFetchRequest|clipboardManager|WebView" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components/SegmentedText.kt
rg -n "LocalSegmentCommentHost|LocalSegmentActionSheetHost|SegmentActionSheet|SegmentedText|SegmentCommentHolder" shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/jvmMain/kotlin app/src/main/java -g '*.kt'
```
