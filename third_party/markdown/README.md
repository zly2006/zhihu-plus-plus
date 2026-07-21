# Vendored Markdown

本目录内置 Zhihu++ 实际使用的 Markdown parser、runtime 和 Compose renderer，替代原来的 Maven 二进制依赖。

- 上游基线：[`huarangmeng/Markdown` 1.2.9](https://github.com/huarangmeng/Markdown/tree/0ae14148bbe427e27629117b3581ea071d86c4c7)
- 上游基线提交：`0ae14148bbe427e27629117b3581ea071d86c4c7`
- 保持原应用行为的源码提交：`4f2ab8c13f44bf24cc070821ea6b510efe188759`
- 内置日期：2026-07-21
- 生产源码清单 SHA-256：`84dfec2a2e279e96863fffdfa989b47f6c9fcbf6c2b9267553108eec1d171c28`

替换前，应用依赖的是 `io.github.zly2006:markdown-*:0.0.1-alpha.11`。这个坐标只说明被替换的分叉构件，不是本目录的上游 base version；本目录的 base version 始终按上游实际版本记为 `1.2.9`。源码提交 `4f2ab8c...` 在 1.2.9 基础上保留了原应用所需的 `NativeBlock` 与 LaTeX 1.4.6-zly 兼容改动。

`/Users/zhaoliyan/IdeaProjects/Zhihu/.tmp/Markdown-issue-495` 是 issue #495 的实验工作树，只用于判断哪些性能思路值得采用。这里没有整体带入它基于后续上游版本的 parser、renderer、布局引擎或自定义选择实现。特别是后续自定义选择仍有[选择手柄问题](https://github.com/huarangmeng/Markdown/issues/33)，本目录继续使用 1.2.9 分支的原生 Compose `SelectionContainer`。

issue #495 的性能修复不会拆分 HTML 或构造不完整 AST。Zhihu++ 仍一次性生成完整文档结构，renderer 默认只对视口及半屏预取范围内的顶层块执行真实 Compose 布局；其余块按文字宽度、换行、公式行数、代码行数、图片比例和容器子块预估高度。块离开预取范围后会恢复为占位，并优先复用已经测得的实际高度，因此滚动条从首帧起就能得到接近完整文档的范围，同时把公式等重布局成本移动到滚动阶段。

vendored renderer 继续使用 1.2.9 分支的原生 Compose `SelectionContainer`，没有带入实验工作树中仍有 issue 的自定义选择实现。脚注实现恢复自[上游 PR #15](https://github.com/huarangmeng/Markdown/pull/15)，并仅按其评论中的方案增加 1px 宽度补偿，修复 Android AVD 上的舍入裁切；视口懒布局还会在脚注跳转时临时物化目标定义或引用，完成 `bringIntoView` 后再恢复常规回收策略。

完整的阶段计时、错误复盘、被拒绝方案和最终验证结果见 [`docs/markdown-issue-495-performance-report.md`](../../docs/markdown-issue-495-performance-report.md)。

三个模块的 `build.gradle.kts` 是接入当前 Zhihu++ Kotlin/Compose 工程所需的宿主构建适配，不属于上游生产源码快照。更新内置库时必须重新核对实际应用基线、逐文件审查生产源码差异，并禁止把实验工作树或上游新版本整体覆盖进来。

可在仓库根目录重新计算生产源码清单摘要：

```bash
rg --files \
  third_party/markdown/markdown-parser/src \
  third_party/markdown/markdown-runtime/src \
  third_party/markdown/markdown-renderer/src \
  | sort \
  | xargs shasum -a 256 \
  | shasum -a 256
```
