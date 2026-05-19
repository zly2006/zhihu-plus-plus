---
name: zhihu-reproduce
description: 当用户要求你模仿知乎官方网站的某个功能时，使用此技能来进行爬虫、API清洗、数据分析、验证，并直接产出高质量UI设计代码。
license: CC BY-NC-SA 4.0
---

## Overview

本技能的原理：通过chrome devtools的mcp功能，远程控制浏览器，模仿用户操作，爬取数据，分析数据，并直接产出高质量UI设计代码。

必须接入有登陆状态的浏览器，才能访问知乎的用户专属功能。

### 1. 确保mcp正常

首先，检查是否已经安装了chrome-devtools-mcp

然后，检查9222端口是否被占用，是否是CDP协议。如果没有占用，或者不是合法的CDP协议：

1. 询问用户使用的是msedge还是chrome浏览器。
2. 根据用户的浏览器，给出启动命令，要求用户在本地执行，启动一个带有远程调试功能的浏览器实例。

假如你是Claude，在.mcp.json已经设置好了mcp server。

假如你是codex，执行以下命令添加MCP server，然后请用户重启对话：

```bash
codex mcp add chrome-devtools -- npx -y chrome-devtools-mcp@latest --browserUrl http://127.0.0.1:9222
```

### 2. 检查该浏览器的知乎登录状态

用CDP请求 https://www.zhihu.com/api/v4/me ，看看能否正常打开。

### 3. 根据用户的需求，模仿用户操作，爬取数据，产出ViewModel以及API代码，并编写API文档

推荐先按下面的顺序做：

1. 用 CDP 抓真实数据，不要猜字段。
2. 先确认字段在 feed、详情接口里的具体位置，再落数据模型。
3. 只在用户指定的渲染链路里改 UI；不要默认 WebView、Markdown、feed 卡片一起改。

#### segment_infos 实战经验

- 推荐流里可先抓：
  - `https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20`
- 回答详情里可先抓：
  - `https://www.zhihu.com/api/v4/answers/{answerId}?include=content,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,attachment,reaction,ip_info,pagination_info,question.topics,reaction.relation.voting`
- 真实样本里，`segment_infos` 常见在：
  - `data[i].target.segment_infos`
  - `answer.segment_infos`
- `allow_segment_interaction` 不是稳定 boolean；实测可能返回 `0/1`，落 Kotlin Serialization 时要做兼容 serializer，不能直接用裸 `Boolean`。
- `segment_infos` 的核心结构是：
  - `pid`: 对应正文里的 `p[data-pid]`
  - `text`: 该段原文
  - `marks[*].start_index/end_index`: 高亮范围
  - `marks[*].seg_info`: `seg_ids / is_like / like_count / comment_count / my_comment_count / is_span`
- Web 端实际 DOM 会把正文片段包成：
  - `span.highlight-wrap.other`
  - 若有评论再带 `has-comments`
  - 常见属性包括 `data-highlight-id`、`data-highlight-split-type`

#### API 文档最低要求

- 写清字段来源接口、字段路径、示例值。
- 说明该字段是否只在部分内容类型返回。
- 标记兼容点：
  - `allow_segment_interaction` 可能是 `0/1`
  - `segment_infos` 可能为空数组
  - 同一段里可能有多个 `seg_ids`

### 4. 分析知乎原版的UI是如何设计的，并进行安卓适配，产出UI代码

做 UI 之前，先确认用户要你模仿的是哪一层：

- feed 卡片摘要
- 回答/文章详情正文
- WebView 渲染
- Markdown/Compose 原生渲染

#### 正文划线功能的安卓适配建议

- 如果只改 Markdown 渲染，不要顺手把 feed 卡片或 WebView 一起改掉。
- 推荐做法：
  1. 先把 `segment_infos` 注入正文 HTML，按 `pid + start/end` 生成 `span.highlight-wrap`
  2. 在 Markdown 解析层识别带 `highlight-wrap` 的段落
  3. 对这类段落走原生 Compose 组件，而不是普通 Markdown `Text`
- 交互上，先做最小可用动作：
  - 点赞
  - 评论 / 打开评论区
  - 复制
- 不要默认塞太多动作；先抄清楚原版，再按用户要求裁剪。

#### 原版样式观察

- Web 端高亮不是整段底色，而是按 mark 范围切片。
- 常见视觉特征：
  - 轻底色
  - 明确的下划线/边界
  - 点击后弹 bottom sheet / popover
- 同一段里可能出现多块高亮，必须保留分块，不要把整段合并成一坨可点击区域。

### 5. 使用真实数据，产出测试代码

你需要从浏览器里面获取一个数据较多，便于测试的真实的内容ID，然后只把这个ID作为测试输入，产出测试代码。测试你写的API调用代码，看看能不能正确获取到数据，并正确渲染UI。

实战里可以直接用一个高密度样本回答做回归，例如：

- `questionId = 507920275`
- `answerId = 2025269343450080224`

它有这些好处：

- 正文里有多处高亮
- 某些高亮带评论数
- 至少有一处高亮聚合了很多 `seg_ids`

#### 建议测试项

1. `segment_infos` 能正确解码。
2. `allow_segment_interaction = 1` 不会导致反序列化失败。
3. HTML 注入后，目标 `p[data-pid]` 内能生成 `span.highlight-wrap`。
4. 点击高亮后，动作面板能显示正确的点赞数/评论数。
5. 如果用户要求只在正文生效，feed 卡片就不应该解析高亮。

### Troubleshooting

#### 1. 看得到字段，但 App 一进详情就炸

先查 `logcat`，重点看：

- `JsonDecodingException`
- `allowSegmentInteraction`
- `getContentDetail`

如果是 `Failed to parse literal '1' as a boolean value`，优先修 serializer。

#### 2. 页面里有高亮，但 Compose 里整段都变成一块

说明你把 `segment_infos` 当整段处理了。应回到 `start/end` 切片，而不是只用 `text` 整段包裹。

#### 3. 想验证点击弹层，但 dump 看不到高亮节点

`ui-test` / `uiautomator` 不一定能把高亮文本暴露成独立可点击节点。

处理顺序：

1. 先 `dump` 确认正文在目标页。
2. 再用截图验证高亮视觉是否出现。
3. 必要时用 `adb shell input tap x y` 点击高亮区域，再 `dump` 弹层内容。

#### 4. 并行跑两个 Gradle 任务后出现大面积假错

不要并行跑 `assemble` 和 `test`。Kotlin 增量缓存可能互相踩坏，表现成一串和当前改动无关的 `Unresolved reference`。处理方式：

```bash
./gradlew --stop
./gradlew assembleLiteDebug
./gradlew testLiteDebugUnitTest --tests your.test.Name
```
