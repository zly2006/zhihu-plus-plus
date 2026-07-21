# Vendored Markdown

本目录内置 Zhihu++ 实际使用的 Markdown parser、runtime 和 Compose renderer，替代原来的 Maven 二进制依赖。

- 上游基线：[`huarangmeng/Markdown` 1.2.9](https://github.com/huarangmeng/Markdown/tree/0ae14148bbe427e27629117b3581ea071d86c4c7)
- 上游基线提交：`0ae14148bbe427e27629117b3581ea071d86c4c7`
- 保持原应用行为的源码提交：`4f2ab8c13f44bf24cc070821ea6b510efe188759`
- 内置日期：2026-07-21
- 生产源码清单 SHA-256：`f47a2aa49019f73987f03ae5d722809bc6cf0b3cc2488a88a0a541f84237ae48`

替换前，应用依赖的是 `io.github.zly2006:markdown-*:0.0.1-alpha.11`。这个坐标只说明被替换的分叉构件，不是本目录的上游 base version；本目录的 base version 始终按上游实际版本记为 `1.2.9`。源码提交 `4f2ab8c...` 在 1.2.9 基础上保留了原应用所需的 `NativeBlock` 与 LaTeX 1.4.6-zly 兼容改动。

`/Users/zhaoliyan/IdeaProjects/Zhihu/.tmp/Markdown-issue-495` 是 issue #495 的实验工作树，只用于判断哪些性能思路值得采用。这里没有整体带入它基于后续上游版本的 parser、renderer、布局引擎或自定义选择实现。特别是后续自定义选择仍有[选择手柄问题](https://github.com/huarangmeng/Markdown/issues/33)，本目录继续使用 1.2.9 分支的原生 Compose `SelectionContainer`。

issue #495 的性能修复位于 Zhihu++ 自己的 HTML 转 AST 与正文组合层：首屏只解析少量顶层 HTML 块，滚动接近末端时再增量解析后续块，并复用已经生成的 AST。这样不修改 vendored renderer 的选择、脚注和布局语义，也不会通过 benchmark 预解析来绕过真实卡顿点。

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
