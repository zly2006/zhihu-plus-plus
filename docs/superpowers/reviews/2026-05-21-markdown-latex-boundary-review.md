# Markdown/LaTeX KMP Boundary Review

Date: 2026-05-21

## Scope

Reviewed the current markdown and LaTeX rendering files as blockers for
`:shared:compileAndroidMain`:

- `app/src/main/java/com/github/zly2006/zhihu/markdown/RenderMarkdown.kt`
- `app/src/main/java/com/github/zly2006/zhihu/markdown/MdAst.kt`
- `app/src/main/java/com/github/zly2006/zhihu/latex/LatexFontDownloader.kt`

Direct callers are already in `shared/src/androidMain`, including
`ArticleScreen`, `QuestionScreen`, and `PinScreen`.

## Conclusion

The minimum correct migration slice is to move the three files to
`shared/src/androidMain/kotlin` with their packages unchanged. This removes the
invalid `shared -> app` dependency for markdown rendering without pretending the
current implementation is common-safe.

These files must not be moved directly to `shared/commonMain` yet. The current
implementation still depends on Android runtime capabilities and Android-only
adapters: `Context`, SharedPreferences, `AccountData.httpClient(context)`,
image dialogs, gallery save/share, Custom Tabs URL launching, `androidx.core`
URI helpers, Android cache files, and androidMain comment/segment UI wrappers.

## Ownership

- `RenderVideoBox`: mostly shared UI/navigation semantics; can later move to
  common after separating it from the Android markdown wrapper.
- `MdAst`: long-term shared parsing semantics; currently androidMain because it
  uses Jsoup, Android URI helpers, and native Compose blocks wired to
  androidMain UI.
- `RenderMarkdown`: currently androidMain; it reads Android preferences,
  loads Android-side LaTeX fonts, opens images, saves/shares images, launches
  browser URLs, and shows Android comment/action sheet wrappers.
- `LatexFontDownloader`: currently androidMain; URL lists and validation are
  shared semantics, but `Context.cacheDir`, `java.io.File`, Compose `Font(file)`,
  and the `.done` marker are platform implementation details.

## Dependency Notes

`shared/androidMain` already has the dependencies needed for this minimum move:
markdown parser/renderer Android artifacts, LaTeX renderer Android artifact,
Coil Android network support, AndroidX core, WebKit/browser dependencies, and
Jsoup.

Long-term common migration should prefer KMP-capable dependencies and adapters:
Ksoup for HTML parsing, shared image URL helpers, shared navigation semantics,
and platform adapters for file storage, image open/save/share, browser launch,
and LaTeX font persistence.

## Risks

This move does not make `:shared:compileAndroidMain` pass by itself. Other
known app-owned references remain in `ArticleScreen`, `QuestionScreen`,
`PinScreen`, `PeopleScreen`, `SearchScreen`, `WebviewComp`,
`ShareDialogComponent`, `CommentScreenComponent`, and developer/settings pages.

The move is still aligned because it removes a concrete reverse dependency
from shared androidMain to app and keeps the current Android behavior intact.

## Validation

Run:

```bash
rg -n "com\\.github\\.zly2006\\.zhihu\\.markdown|com\\.github\\.zly2006\\.zhihu\\.latex|RenderMarkdown|htmlToMdAst|rememberLatexFonts" app/src shared/src -g '*.kt'
rg -n "org\\.jsoup|Jsoup" shared/src/commonMain/kotlin
./gradlew :shared:compileAndroidMain --continue
./gradlew testLiteDebugUnitTest --tests 'com.github.zly2006.zhihu.markdown.MdAstTest'
```

