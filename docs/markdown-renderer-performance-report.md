# Markdown renderer 性能优化报告

## 结论

本轮保留完整 Markdown AST、文本选择、脚注跳转、链接点击和真实公式绘制，同时删除可退回全量 eager 布局的公开开关。静态与可选择正文统一使用递归 deferred 布局，非选择正文使用 `LazyColumn`。

在 412 × 892 的 Compose Desktop JVM 画布上，最终 16 类稳定场景的中位数全部低于 15.6 ms，其中 15 类低于 15 ms；全部原始样本低于 100 ms，超过 98% 低于 50 ms。由项目真实公式语料中最长 80 条公式组成的同一篇压力文章，首次向前滚动中位数从 341.7 ms 降到 27.8 ms。

## 优化前后

下表使用相同的 selectable + deferred 生产路径、相同的最长 80 公式文章和相同的 700 px 分步滚动边界。每一步都会等待 Compose idle 并截图确认实际 draw。

| 指标 | 优化前 | 优化后 | 变化 |
| --- | ---: | ---: | ---: |
| 首次向前滚动中位数 | 341.7 ms | 27.8 ms | -91.9% |
| 首次向前滚动 P90 | 523.1 ms | 34.0 ms | -93.5% |
| 返回滚动中位数 | 81.6 ms | 20.9 ms | -74.4% |
| 返回滚动 P90 | 108.7 ms | 24.0 ms | -77.9% |

最终压力回归的完整边界：

| 路径 | 中位数 | P90 | 最大值 | 低于 50 ms |
| --- | ---: | ---: | ---: | ---: |
| 向前，包含公式准备、布局和 draw 验证 | 27.8 ms | 34.0 ms | 56.2 ms | 39 / 40 |
| 返回，命中文档缓存 | 20.9 ms | 24.0 ms | 49.3 ms | 40 / 40 |
| 公式准备期间的独立 UI 响应 | 16.8 ms | 26.5 ms | 37.1 ms | 40 / 40 |

普通正文、格式文本、链接、长文、列表、代码、行内与块公式、表格及混合文档共 16 类场景中：

- 16 / 16 场景中位数低于 30 ms，15 / 16 低于 15 ms，最大中位数为 15.6 ms；
- 所有原始样本低于 100 ms；
- 至少 70% 原始样本低于 50 ms，实际超过 98%；
- issue #495 的 36,460 字符 HTML 转完整 AST 中位数为 9.27 ms；
- 3,703 条真实知乎公式 parser-only 中位总耗时为 20.67 ms。

## 根因

原来的 LaTeX 组件只把解析放到后台，Compose 主线程仍会同步测量公式；长文滚动时，每个新进入视口的公式都会触发布局，滚出视口再进入后又可能重复计算。大量公式同时启动后台任务还会占满 CPU，与输入、Compose layout 和 draw 争抢调度。

`yield()` 只能让已经在后台运行的协程协作调度，不能把同步主线程测量移走，也不能消除重复布局。因此主要收益不是简单增加 yield 次数，而是改变完整准备链路和缓存生命周期。

## 实现

1. LaTeX alpha5 把解析和完整布局放到单一受限后台 lane，在解析与布局边界检查取消并主动 yield。
2. 每个 Markdown 文档持有页面级、可固定活跃公式的 LRU，公式离屏回收后可以复用不可变的准备结果。
3. 行内公式先使用保守占位，准备完成后只按该公式的真实尺寸刷新，删除主线程同步预测量。
4. selectable/static 正文使用一个 `SubcomposeLayout`，只组合视口及前后 1.5 个视口的块；列表、引用等容器内部同样递归 deferred。
5. 实测块高度会替代估算高度；脚注跳转可临时物化屏外定义或引用，完成导航后继续回收。
6. HTML 转 AST 的后向内容判断由逐节点 `drop()` 扫描改成一次反向索引，消除长输入上的二次复杂度。
7. 链接由轻量字符串 annotation 和共享点击节点处理，避免为每个链接创建较重的交互对象。

## JVM 与 Android 校准

日常功能和性能回归优先使用 Compose JVM。只在 `off` 的 API 35、2 核 KVM x86_64 AVD 上执行一次同输入校准：

| 场景 | JVM 中位数 | AVD 中位数 | AVD / JVM |
| --- | ---: | ---: | ---: |
| 短正文 | 4.52 ms | 81.67 ms | 18.1x |
| 30 段格式正文 | 5.43 ms | 85.81 ms | 15.8x |
| 80 个块公式 | 3.87 ms | 82.89 ms | 21.4x |

倍率中位数为 18.1x。它包含两核、x86_64 和软件 GPU 的保守开销，只用于控制 AVD 占用和发现量级回退，不能冒充真机倍率；执行面、Compose 版本或硬件变化后需要重新校准。

## 固定回归门

`MarkdownPerformanceTest` 固定以下要求：

- 全部稳定 JVM 样本低于 100 ms；
- 至少 70% 稳定样本低于 50 ms；
- 全部场景中位数低于 30 ms，至少 70% 场景中位数低于 15 ms；
- 长公式文章每个方向的全部滚动样本低于 100 ms，至少 70% 低于 50 ms；
- 公式准备期间至少 70% UI 响应低于 50 ms。

压力文章直接读取 `shared/src/jvmTest/resources/zhihu-formula-corpus/formulas.json`，去重后按长度降序选择 80 条，不使用缩短公式或预构造布局绕开瓶颈。

功能回归还覆盖真实公式像素、行内公式尺寸更新、缓存跨组合回收、纯链接与公式混合段落链接点击、300 项递归列表尾部物化，以及外层滚动容器中的脚注定义跳转与返回。

## 复现

```bash
MARKDOWN_PERFORMANCE=1 ./gradlew --no-daemon --rerun-tasks \
  :markdown-renderer:jvmTest \
  --tests 'com.hrm.markdown.renderer.MarkdownPerformanceTest'

MARKDOWN_PERFORMANCE=1 ./gradlew --no-daemon \
  :shared:jvmTest \
  --tests 'com.github.zly2006.zhihu.markdown.MarkdownConversionPerformanceTest' \
  --tests 'com.github.zly2006.zhihu.markdown.LatexParserPerformanceTest'
```
