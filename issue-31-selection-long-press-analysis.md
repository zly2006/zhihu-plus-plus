# Issue #31 长文档无法通过长按选择文本：根因记录

## 环境

- 上游仓库：`huarangmeng/Markdown`
- 基线：`master` at `fbf1d2089334286f0a8a61efe6f165ffd6a4b441`
- Preview：`数学公式 -> 性能回归 -> Issue #31 长 LaTeX 直渲染`
- Preview 调用已显式传入 `enableSelection = true`
- 复现平台：Compose Desktop/JVM

长公式文档的渲染性能在人工滚动测试中可接受，但静止长按正文后不会保留选区，也不会出现可复制的文本选择状态。

## 已确认根因

问题不在 `enableSelection` 参数传递，而在长按手势建立的初始选区是零长度范围。

手势层通过 `detectDragGesturesAfterLongPress` 监听长按：

1. 长按超时后，`onDragStart` 调用 `beginSelectionAtRootLocal()`。
2. `beginSelectionAt()` 将起点和终点都设为同一个 anchor。
3. 如果用户只是长按后抬手，没有继续拖动，选区始终没有字符。
4. `onDragEnd` 调用 `finishSelectionGesture()`。
5. `finishSelectionGesture()` 发现 `selectedText` 为空，立即执行 `clearSelection()`。

因此当前实现实际支持的是“长按后拖动形成非零范围”，不支持通常意义上的“长按直接选中一个词或字符”。从用户视角看，静止长按必然没有任何效果。

关键代码位置：

- `markdown-renderer/src/commonMain/kotlin/com/hrm/markdown/renderer/internal/selection/SelectionGestures.kt`
  - `onDragStart` 只调用 `beginSelectionAtRootLocal()`。
  - `onDragEnd` 调用 `finishSelectionGesture()`。
- `markdown-renderer/src/commonMain/kotlin/com/hrm/markdown/renderer/internal/selection/MarkdownSelectionController.kt`
  - `beginSelectionAt()` 创建 `SelectionRange(anchor, anchor)`。
  - `finishSelectionGesture()` 在 `selectedText.isEmpty()` 时清空选区。

## 自动化复现证据

诊断期间临时增加了一个 Compose JVM 指针测试：

1. 构造内容为 `hello world` 的可选文本块。
2. 对文本执行真实 `performTouchInput { longClick(...) }`。
3. 手指抬起后读取 selection controller 的 `selectedText`。
4. 期望得到 `hello`，实际得到空字符串。

测试失败信息：

```text
expected:<[hello]> but was:<[]>
```

该测试证明问题发生在真实长按手势闭环中，不是 Preview 没有传入 `enableSelection`，也不是单纯的高亮绘制问题。

## 建议修复方向

长按开始时不应创建零长度范围，而应立即建立一个非空的初始选区：

- 拉丁字母、数字和下划线：选择光标所在的完整词。
- 中文、日文、韩文：至少选择当前字符；若项目引入可靠的跨平台分词能力，可再扩展到语言词边界。
- Emoji 和代理对：不能从 UTF-16 代理对中间截断。
- 标点：选择当前标点字符。
- 空白：选择相邻的非空白词或字符，不要留下只有空白的选区。

长按后继续拖动时，应以初始词/字符范围为原点向两侧扩展，不能在向左拖动时丢失初始范围的右边界。

## 修复后的验收条件

1. 静止长按普通英文正文，抬手后保留完整单词选区。
2. 静止长按中文正文，抬手后至少保留一个完整字符选区。
3. 长按后向左或向右拖动，可以连续扩展选区。
4. 可以跨 Markdown block 选择，并复制出正确的换行文本。
5. 点击选区外部只清除一次，不与长按结束事件竞争。
6. `enableSelection = false` 时不注册选择手势。
7. Issue #31 长公式文档启用 selection 后，首屏和滚动性能不出现明显回退。

建议保留真实 Compose 指针测试，不要只测试 selection model 的纯函数；否则可能遗漏手势结束后又被清空这类生命周期问题。

## 尚未确认的次要风险

根容器目前分别注册了长按拖动和点击清除两个 `pointerInput`。两套 detector 都会观察同一组 pointer 事件，代码通过 `skipNextTapClear` 避免长按结束后的 tap 误清除。当前已确认的首要问题是零长度选区；修复后仍应通过真实指针测试确认两个 detector 的事件顺序在 Android、Desktop、iOS 上一致，必要时再合并手势状态机。

## 范围说明

本文只记录问题、证据和建议修复方向，没有提交上游 selection 实现修改。
