# 翻页键支持架构

## 事件流

```
┌──────────────┐    ┌──────────────┐
│ 实体按键      │    │ 悬浮翻页按钮  │
│ (PAGE_DOWN   │    │ (FAB)        │
│  VOLUME_DOWN │    │              │
│  等)         │    │              │
└──────┬───────┘    └──────┬───────┘
       │                   │
       ▼                   ▼
  MainActivity         pageTurnFlow
  .dispatchKeyEvent     .tryEmit(±1)
       │                   │
       ▼                   │
  pageTurnFlow ◄───────────┘
  MutableSharedFlow<Int>
       │
       ├──► 各屏幕的 PageTurnEffect (scrollBy)
       ├──► ZhihuMain (底部栏显/隐)
       └──► CommentScreen (弹层内翻页)
```

## 事件值语义

| 值 | 含义 |
|---|---|
| `1` | 下翻一页（视口的 N%） |
| `-1` | 上翻一页 |
| `Int.MAX_VALUE` | 长按跳到底部 |
| `Int.MIN_VALUE` | 长按跳到顶部 |

## 涉及的文件和职责

### 1. 事件产生层

- **`MainActivity.kt`** — `dispatchKeyEvent` 拦截实体按键，通过 `pageTurnDirection()` 判断是否是翻页键，短按在 `ACTION_UP` 时发射 ±1，长按在 `ACTION_DOWN` 时发射 `±MAX_VALUE`。`longPressConsumed` 标志防止长按后松手再触发短按。

- **`DraggablePageTurnButtons.kt`** — 定义 `pageTurnFlow`（`MutableSharedFlow`）、`LocalPageTurnChannel`（CompositionLocal）、`pageTurnFabVisible`（FAB 可见性）、`pageTurnModalDepth`（弹层计数）。FAB 点击时直接 `tryEmit(±1)`。

### 2. 事件消费层 — 滚动

- **`PageTurnScrollEffect`**（`DraggablePageTurnButtons.kt`）— 适用于 `ScrollState`（如 `ArticleScreen` 的 `Column + verticalScroll`）。收集 `LocalPageTurnChannel`，根据方向调用 `scrollState.scrollTo()` 或 `scrollState.scrollBy(viewport * percent)`。

- **`PageTurnLazyListEffect`**（`DraggablePageTurnButtons.kt`）— 适用于 `LazyListState`（如 `PaginatedList`、`CommentScreen`、`DailyScreen`）。逻辑类似，但用 `listState.scrollBy()` / `listState.scrollToItem()`。

- 每个需要翻页的屏幕只需一行调用：

```kotlin
PageTurnLazyListEffect(listState, pageTurnPercent, guideState)
// 或
PageTurnScrollEffect(scrollState, pageTurnPercent, ...)
```

### 3. 事件消费层 — UI 联动

- **`ZhihuMain.kt`** — 监听 `LocalPageTurnChannel` 更新 `isBottomBarVisible`，使 FAB/实体键翻页时底部导航栏的隐藏行为与触屏一致。（程序化 `scrollBy` 不经过 `NestedScrollConnection`，所以需要单独监听。）

### 4. 弹层翻页隔离

- **`CommentScreenComponent.kt`** — 评论弹层打开/关闭时 `pageTurnModalDepth++/--`。弹层使用 `usePlatformWindow = false` 以便翻页事件能传递进来（平台 Dialog 窗口会拦截按键事件且遮挡 FAB）。

- **`PageTurnLazyListEffect`** 默认 `skip = pageTurnModalDepth > 0`，弹层下方的页面自动停止响应翻页。评论弹层自己的 `CommentScreen` 传入 `skip = false` 来接管翻页。子评论打开时，父评论传入 `skip = childSheetVisible` 让出控制权。

### 5. 翻页引导线

- **`PageTurnGuideState`**（`DraggablePageTurnButtons.kt`）— 持有 `lastDirection` 和 `isScrolling`，供 `PageTurnGuideOverlay` 绘制翻页方向指示线。

### 6. 段评弹层修复

- **`RenderMarkdown.kt`** — 增加 `onOpenSegmentComment` 回调，允许调用方将段评弹层提升到外层渲染。
- **`ArticleScreen.kt`** — 使用该回调将段评 `CommentScreenComponent` 渲染在 `Scaffold` 外层，避免在滚动容器内渲染导致弹层和 FAB 失效。

