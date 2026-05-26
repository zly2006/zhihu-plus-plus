# Official Badge Icon Resource Boundary Review

## Input

- Target paths:
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components/OfficialBadgeIconModel.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/OfficialBadgeIconModel.android.kt`
  - `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/components/OfficialBadgeIconModel.jvm.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components/AuthorBadge.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PeopleScreen.kt`
  - `shared/src/androidMain/res/drawable/ic_zh_plus_author_badge.xml`
- Current behavior: common UI passed badge icon URLs through `officialBadgeIconModel()`. Android mapped the app-local `zhplus://zh_plus_author_badge_icon` sentinel to a drawable resource, while JVM returned the sentinel URL unchanged.
- Desired target: render the local Zhihu++ author badge from common Compose resources on Android and desktop, while leaving remote badge URLs on the existing Coil image path.

## Conclusion

The local official badge icon is shared UI presentation. It is tied to common `DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON` data, not to Android platform capabilities. The `OfficialBadgeIconModel` expect/actual layer is a migration bridge and should be removed.

## Boundary

- Shared ownership: sentinel-to-local-resource UI rendering, `AuthorBadge`, and the `PeopleScreen` official badge details row.
- Platform ownership: none for this slice.
- Not involved: lifecycle, threading, persistence, serialization, database, network, navigation, theme state, `Context`, `Intent`, WebView, Toast, file paths, or platform clipboard.
- Dependencies: existing Compose Multiplatform resources are sufficient. No new Gradle dependency is required.

## Implementation Notes

- Move `ic_zh_plus_author_badge.xml` to `shared/src/commonMain/composeResources/drawable/`.
- Keep `AuthorBadge` early returns, compact size behavior, content description, semantics, and modifier order.
- Keep `PeopleScreen`'s `OfficialBadgeDetails` structure: `Column`, per-badge `Row`, optional icon, and the existing text line with ellipsis.
- Do not extract a micro-component. The branch is intentionally inline at the original rendering points.
- Use `Image(painterResource(...))` for the local resource and keep `AsyncImage(model = badge.iconUrl)` for remote URLs.

## Master Shape Check

- Compared `master:app/src/main/java/com/github/zly2006/zhihu/ui/components/AuthorBadge.kt`.
  - Preserved key function name: `AuthorBadge`.
  - Preserved early returns, `compact` size rule, `contentDescription`, semantics, and modifier placement.
  - Replaced only the old local-resource mapping helper with an inline local-resource branch because the helper was the removed platform bridge.
- Compared `master:app/src/main/java/com/github/zly2006/zhihu/ui/PeopleScreen.kt` `OfficialBadgeDetails`.
  - Preserved key function name: `OfficialBadgeDetails`.
  - Preserved `Column -> badges.forEach -> Row -> optional icon -> Text` structure, icon `padding(end = 6.dp).size(18.dp)`, text style, color, max lines, and ellipsis.
  - Changed only the local Zhihu++ author badge model source so Android and desktop use the same common resource.

## Verification

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet
git diff --check
rg -n "OfficialBadgeIconModel|officialBadgeIconModel" shared/src app/src
rg -n "R\\.drawable\\.ic_zh_plus_author_badge|com.github.zly2006.zhihu.shared.R" shared/src app/src
rg -n "ic_zh_plus_author_badge|ZH_PLUS_AUTHOR_BADGE_ICON" shared/src app/src -g '*.kt' -g '*.xml'
```
