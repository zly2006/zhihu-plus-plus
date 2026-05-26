# Segmented Text Reaction Helper Boundary Review

Date: 2026-05-27
Reviewer: gpt-5.5 xhigh subagent `019e660f-08cc-7c42-9c5a-182c0907a610`

## Scope

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components/SegmentedText.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/SegmentedTextRuntime.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/components/SegmentedTextRuntime.jvm.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/util/SegmentHighlightUtils.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/SegmentInfo.kt`

## Conclusion

The segment reaction body and local meta update logic belong in `shared/commonMain`. Platform actual files should keep request execution, authentication, signing, cookies, and clipboard side effects.

Shared ownership:

- `zhihuSegmentReactionUrl()`.
- Unlike request body shape with `seg_ids`.
- Like request body shape with optional `seg_id`, `content`, and `position.start/end`.
- Response parsing for `payload.segId`.
- Local `SegmentInfoMeta` updates for `segIds`, `isLike`, and `likeCount`.

Platform ownership:

- Android `LocalContext`, `ClipData`, `AccountData.withAuthenticatedResponse()`, `AccountData.fetchPost()`, Android `signFetchRequest()`, and clipboard.
- JVM `DesktopAccountStore`, `d_c0` cookie lookup, `signZhihuFetchRequest(dc0, body)`, and AWT clipboard.

## Required Compatibility

Keep these names and structure:

- `SegmentedText`
- `rememberSegmentedTextRuntime`
- private platform `toggleSegmentLike`
- `zhihuSegmentReactionUrl`

Keep the existing platform order: `contentId`/`targetType` early return, URL construction, unlike DELETE branch, like POST branch, then local meta update. Android must keep its request block order around signing, content type, and body.

## Minimum Implementation

Add common pure helpers:

- `buildSegmentUnlikeBody(highlight)`
- `buildSegmentLikeBody(highlight)`
- `updateSegmentMetaAfterUnlike(highlight)`
- `updateSegmentMetaAfterLike(highlight, response)`

Then update Android/JVM actual implementations to call these helpers while keeping request execution and signing in platform files.

## Validation

```bash
rg -n "Context|LocalContext|ClipData|AccountData|DesktopAccountStore|Toolkit|StringSelection|signFetchRequest|signZhihuFetchRequest|HttpMethod|setBody" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/util -g '*.kt'
rg -n "buildSegmentUnlikeBody|buildSegmentLikeBody|updateSegmentMetaAfterLike|updateSegmentMetaAfterUnlike|seg_ids|seg_id|payload|segId" shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/jvmMain/kotlin -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet --continue
git diff --check
```
