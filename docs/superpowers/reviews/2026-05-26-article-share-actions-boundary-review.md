# Article Share Actions Boundary Review

Date: 2026-05-26

## Subagent status

Required `gpt-5.5 xhigh` boundary subagent was started for this slice, but the service returned:

```text
unexpected status 503 Service Unavailable: auth_unavailable: no auth available (providers=codex, model=gpt-5.5)
```

The subagent reached a final errored state, so this document records the local review using the same checklist before code changes.

## Input

- Target files:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ArticleActionsRuntime.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleActionsRuntime.android.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/ArticleActionsRuntime.jvm.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components/ShareDialog.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/ShareDialogComponent.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/components/ShareDialogRuntime.jvm.kt`
- Callers: common `ArticleScreen` article menu share and copy actions.
- Current source sets: article action interface and menu UI are common; platform share, clipboard, browser open and TTS are Android/JVM adapters.
- Expected target: remove duplicated article share/copy platform code by reusing existing `ShareDialogRuntime`, without changing article menu UI, `articleActionText`, TTS or browser open behavior.

## Ownership

- Shared ownership:
  - `articleActionText(article, questionId, title, authorName)` and the answer/article URL/title text rules.
  - `ArticleActionsRuntime` action names and common call sites.
  - The decision that article menu share uses the article-specific text, not generic `getShareText(article)`.
- Platform ownership:
  - Android `Intent.ACTION_SEND`, chooser title, `FLAG_ACTIVITY_NEW_TASK`, clipboard `ClipData`, `clipboardDestination`, and user message.
  - JVM desktop clipboard fallback and user message.
  - TTS engine/process and browser open side effects.

## Master shape check

Master `ArticleScreen` article menu share/copy computes text inline, then:

- Share: sends `ACTION_SEND` with `type = text/plain`, `EXTRA_TEXT` only, chooser title `分享到`, and `FLAG_ACTIVITY_NEW_TASK`.
- Copy: sets clipboard destination to the current `Article`, copies `ClipData.newPlainText("Link", text)`, and shows `已复制链接`.

Current common code already preserves the text formula through `articleActionText(...)`. The minimal safe migration is to keep `shareArticle` and `copyArticleLink` names, keep the same text computation, and delegate only the platform side effects to existing `ShareDialogRuntime.share(...)` and `ShareDialogRuntime.copyLink(...)`. This preserves master behavior because Android dialog `share` sends only `EXTRA_TEXT`, while `copyLink` preserves destination, label, and message.

## Decision

Reuse `rememberShareDialogRuntime()` inside platform `rememberArticleActionsRuntime()` implementations:

- Android `shareArticle`: compute `articleActionText(...)`, then call `shareRuntime.share(article, text)`.
- Android `copyArticleLink`: compute `articleActionText(...)`, then call `shareRuntime.copyLink(article, text)`.
- JVM `shareArticle`: call the same `shareRuntime.share(article, text)`, preserving `已复制分享文本`.
- JVM `copyArticleLink`: call `shareRuntime.copyLink(article, text)`, preserving `已复制链接`.

No new UI component, navigation lambda, route model, or article-specific share abstraction is needed.

## Risks

- Android direct share and dialog share are intentionally different for generic content: direct share includes `EXTRA_TITLE`; dialog share does not. Article menu master behavior matches dialog-style `share`, so this slice must not call `directShare`.
- Article answers must keep question-scoped URLs: `https://www.zhihu.com/question/{questionId}/answer/{article.id}`. Do not replace with generic `getShareText(article)`.
- TTS and browser open are unrelated platform side effects and should not be moved in this slice.

## Validation

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
git diff --check
rg -n "articleActionText\\(|ACTION_SEND|ClipData|clipboardDestination|copyText\\(" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleActionsRuntime.android.kt shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/ArticleActionsRuntime.jvm.kt shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ArticleUiCommon.kt -g '*.kt'
```
