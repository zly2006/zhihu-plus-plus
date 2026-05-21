# Comment Screen Boundary Review

## 输入

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/CommentScreenComponent.kt`
- `app/src/main/java/com/github/zly2006/zhihu/ui/CommentScreen.kt`
- shared comment viewmodels
- `CommentItem`
- `CommentHolder` / `SegmentCommentHolder`
- image/open-url/emoji/html parser dependencies

## 结论

`CommentScreenComponent` 和 `CommentScreen` 都不是 app-only。评论 bottom sheet 编排、root/child sheet 状态、评论列表、排序、回复输入、子评论入口、点赞 UI、测试 tag、`CommentHolder` / `SegmentCommentHolder` 语义都应迁向 shared。当前仍需拆分，因为页面和 ViewModel 混有 Android/JVM-only 副作用。

comment ViewModel 的分页、去重、评论 URL、提交/点赞状态也应迁向 shared；当前仍依赖 `Context`、Toast、Android `signFetchRequest`、`AccountData.decodeJson`、`htmlEncode`、`paginationEnvironment(context)`。

## 当前最小修复

当前 blocker 是 `CommentScreenComponent` 已在 shared/androidMain，但 `CommentScreen.kt` 仍在 app，导致 shared 编译不可见。最小修复应：

- `git mv app/src/main/java/com/github/zly2006/zhihu/ui/CommentScreen.kt shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/CommentScreen.kt`
- 保持 package 和现有结构。
- 如 shared androidMain 缺 preview 依赖，则优先移除 preview import/annotation，而不是给迁移切片扩大依赖面。

不拆整页 slot，不引入新 loader。

## 后续 shared 方向

- `Jsoup` 纯解析替换为 Ksoup。
- `java.text` / `Calendar` 替换为 `kotlinx-datetime`。
- Toast 改 `UserMessageSink`。
- `AccountData.decodeJson` 改 `ZhihuJson.decodeJson`。
- 签名走 common `signZhihuFetchRequest` / `PaginationEnvironment.configureSignedRequest`。
- 图片打开/保存/分享、外部 URL 打开拆 platform adapter。

## 验证命令

```bash
rg -n "import com.github.zly2006.zhihu.ui.CommentScreen|fun CommentScreen\\(" app/src shared/src -g '*.kt'
./gradlew :shared:compileAndroidMain --continue
git diff --check
```
