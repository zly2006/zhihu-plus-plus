---
name: zhihu-reproduce
description: 当用户要求复刻知乎网页版的功能、交互、接口行为或视觉细节时，使用此技能采集真实网页证据、分析 API 与 UI 结构，并落地到 Zhihu++ Android 代码。
license: CC BY-NC-SA 4.0
---

## Overview

本技能用于把知乎网页版上的真实功能复刻到 Zhihu++ Android 客户端。核心原则是：先用已登录浏览器观察真实页面和接口，再根据证据设计数据模型、UI、交互与验证方案，不凭印象猜字段或样式。

适用范围包括但不限于：

- feed 卡片、详情页、评论区、个人页、搜索页等页面结构
- 点赞、收藏、关注、评论、分享、折叠、展开等交互行为
- Web API 字段、请求参数、分页、状态位、权限位和异常返回
- 原版视觉样式、布局节奏、弹层、菜单、空状态和加载状态
- Android 端 Compose / WebView / Markdown 渲染链路适配

必须使用有知乎登录状态的浏览器。未登录时只能观察公开页面，不能据此推断用户专属功能。

## 1. 环境与 MCP 检查

1. 询问用户使用的是msedge还是chrome浏览器。
2. 根据用户的浏览器，给出启动命令，要求用户在本地执行，启动一个带有远程调试功能的浏览器实例。

假如你是Claude，在.mcp.json已经设置好了mcp server。

假如你是codex，执行以下命令添加MCP server，然后请用户重启对话：

先确认当前 Codex 会话是否已经可用 `chrome-devtools`。如果工具不可用，按下面顺序检查，而不是只看仓库里的 `.mcp.json`：

```bash
codex mcp list
codex mcp get chrome-devtools
curl -s http://127.0.0.1:9222/json/version
```

`127.0.0.1:9222/json/version` 必须返回合法 CDP 信息。Edge 和 Chrome 都可以，只要 CDP 端点有效并且是用户实际登录知乎的浏览器实例。

如果还没有注册 DevTools MCP，在 Codex 中使用：

```bash
codex mcp add chrome-devtools -- npx -y chrome-devtools-mcp@latest --browserUrl http://127.0.0.1:9222
```

注册后通常需要重启或重新进入对话，当前会话不一定会热加载新 MCP。重启后再确认工具真的可用。

如果 9222 没有可用浏览器实例，询问用户使用 Edge 还是 Chrome，再给出对应的远程调试启动命令。不要擅自假设浏览器类型或登录状态。

## 2. 登录状态检查

用已连接浏览器访问或请求：

```text
https://www.zhihu.com/api/v4/me
```

确认返回的是已登录用户信息，而不是登录页、错误页或匿名状态。只有登录状态确认后，才继续采集需要账号权限的功能。

## 3. 需求定界

在动代码前先把用户要复刻的范围说清楚。至少确认这些边界：

- 目标页面：feed、回答详情、文章详情、评论区、搜索页、个人页或其他页面
- 目标对象：卡片、正文、工具栏、弹层、菜单、状态按钮、列表项等
- 目标行为：只显示数据、支持点击、支持状态切换、支持分页、支持离线缓存等
- Android 落点：Compose 原生 UI、Markdown 渲染、WebView、Repository/API、Room 缓存或导航逻辑
- 非目标范围：明确哪些相邻页面或渲染链路本次不改

如果用户只要求分析，先产出分析结论，不要直接写代码。如果用户要求实现，按项目既有架构落地，避免把相邻概念合并。

## 4. 采集真实网页证据

用 DevTools 模仿真实用户操作，采集三类证据：

1. 页面证据：截图、DOM 结构、可见文案、布局关系、交互前后状态
2. 网络证据：请求 URL、方法、参数、响应字段、分页参数、权限字段、错误返回
3. 行为证据：点击、长按、展开、收起、弹层、跳转、滚动加载、空状态和失败状态

采集时遵守这些规则：

- 不要只抓一个接口就下结论；关键字段要看列表接口和详情接口是否一致。
- 不要只看字段名；记录字段路径、示例值、缺省值、空数组、null、数字/布尔混用等兼容点。
- 不要把 Web DOM 结构直接等同于 Android 实现；先理解它表达的产品语义。
- 对用户专属状态，如是否点赞、是否关注、是否收藏，必须采集状态切换前后的返回。

常见入口示例：

```text
https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20
https://www.zhihu.com/api/v4/answers/{answerId}
https://www.zhihu.com/api/v4/questions/{questionId}/answers
https://www.zhihu.com/api/v4/articles/{articleId}
https://www.zhihu.com/api/v4/comment_v5/{contentType}/{contentId}/root_comment
```

具体接口以 DevTools 实际捕获为准，不要把上面的示例当成固定答案。

## 5. API 与数据模型设计

落数据模型前，先写清楚字段来源：

- 来源接口、请求参数和认证要求
- 字段路径、类型、示例值
- 该字段是否只在某些内容类型或登录状态下返回
- 字段缺失、null、空数组、数字/字符串/布尔混用时的兼容策略
- 与项目现有 data class、Repository、ViewModel 的关系

实现时遵守项目约定：

- 知乎 API 原始字段通常是 `snake_case`，DataHolder 和 Kotlin data class 使用 `camelCase`。
- `AccountData.fetch*()` 和 `decodeJson()` 已经处理 `snake_case2camelCase()`，不要手写重复转换。
- Web API 使用 `signFetchRequest(context)`；Android API 使用 `AccountData.ANDROID_HEADERS` 和 `ANDROID_USER_AGENT`。
- 新增模型优先复用现有结构；只有语义不同或生命周期不同才新增类型。

如果发现网页字段和现有存储模型不匹配，先说明差异和迁移影响，再决定是否改 Room、Repository 或只做内存态展示。

## 6. UI 与交互复刻

做 UI 前先判断原版设计服务的产品语义，而不是机械照搬像素：

- 信息层级：哪些内容是主信息、辅助信息、状态信息
- 交互优先级：哪些操作常驻，哪些放在菜单或弹层
- 状态表达：未登录、无权限、已操作、加载中、失败、空数据
- Android 适配：触控面积、Material 3 组件、深色模式、无障碍文本、窄屏布局

修改范围要精确：

- 只改用户指定的渲染链路，不顺手扩散到 feed、详情、WebView、Markdown 等相邻链路。
- Compose 中用 `LaunchedEffect` 处理副作用，并设置正确 key。
- 用 `collectAsState()` 观察 Flow/StateFlow。
- 导航相关改动前必须先检查 `NavDestination.kt`。

如果原版依赖 hover、复杂 DOM 或桌面布局，应转换成 Android 上自然的交互，例如 bottom sheet、菜单、长按、可点击行或显式按钮。

### 官方认证 / 徽章复刻注意事项

复刻知乎官方认证、优秀答主、社区成就等 badge 时，不能只从 `badge_v2.title` / `description` 推断成文字 chip。必须先在 Web 端同时采集：

- 用户名旁的 DOM：知乎 Web 通常用 `badge_v2.icon` 渲染一个约 `18px × 18px` 的图片 icon，`aria-label` / `data-tooltip` 承载具体说明。
- 页面侧栏或个人主页的明细区：People/Profile 里会把 `detailBadges` 展开成“认证与成就”，例如“社区成就：知势榜教育校园领域影响力榜答主”“认证信息：华东师范大学 理学硕士”。
- `#js-initialData` 或接口响应里的 `badgeV2`：区分顶层 `icon/nightIcon`、`mergedBadges` 和 `detailBadges`。用户名旁的主 icon 可能来自顶层 `badgeV2.icon`，而明细列表应优先使用 `detailBadges[*].icon/nightIcon`。
- 空值兼容：`mergedBadges[*].icon` 可能为空，但 `detailBadges[*].icon` 或顶层 `badgeV2.icon` 有值；`badgeStatus` / `badge_status` 需要兼容，只展示 `passed` 或无状态的 badge。

本技能的失败经验：如果只实现 title/description 的 Compose 文字胶囊，会明显偏离官方样式；正确方向是“列表/详情用户名旁用官方 icon，PeopleScreen 展开具体认证/成就信息”。做完后应增加解析测试覆盖顶层 icon、detail badge icon、identity 与 reward/community badge 的优先级。

本技能的失败经验：新增一个非官方身份标识时，不能把它塞进官方 badge 的主显示槽或用 `A ?: B` 改变既有优先级。官方认证、MCN、会员等属于不同信息层级，应先复刻原需求里的并列/独立展示方式，再决定是否复用组件。例子：用户同时有官方认证和 MCN 时，用户名旁的官方图标应保留，MCN 应作为单独标识或详情项展示；不能让无图标的 MCN 占掉官方图标位置。

## 7. 测试与验证

优先用真实采集到的内容 ID 做回归测试。测试输入只保留必要 ID，不把整份真实响应硬编码进测试，除非测试目标就是解析兼容性。

最低验证要求：

- API 请求能成功获取真实数据。
- 新增字段能正确解码，包括缺失、null、空数组和类型兼容情况。
- ViewModel 状态流能覆盖加载、成功、失败和空状态。
- UI 能在真实样本上正确显示，并且交互前后状态符合网页证据。
- 如果用户要求只影响某一页面或链路，必须验证相邻链路没有被误改。

如果改动涉及用户可见 UI，按项目 AGENTS.md 的 UI 调试与双代理复检流程执行。

## 8. 文档输出

实现复杂功能时，同步补一份简短 API / 设计记录，至少包含：

- 功能目标和非目标范围
- 采集到的关键接口和字段路径
- 与原版 UI / 交互的差异和 Android 适配原因
- 已知兼容点和测试样本 ID
- 后续可扩展点

文档面向后续维护者，不要写成泛泛教程。

## Troubleshooting

### 用户纠正术语时要继续原任务

复刻流程中如果用户只是在纠正工具名、技能名或术语拼写，不能把这类短句当成新的独立任务并停止当前工作。应把纠正立即应用到正在进行的复刻、验证、提交或 PR 流程里，然后继续完成原任务。例子：用户把一个写错的技能名纠正为正确技能名时，应该继续使用该技能处理剩余的实现或收尾步骤，而不是只回复“已记住”后中断。

### 采集时不要抢占用户正在使用的浏览器

用 DevTools 采集知乎网页证据前，必须先判断这个浏览器实例是不是用户正在工作的前台窗口。不能为了切到目标页面而直接 `select_page` 并 `bringToFront`，也不能在用户未授权时点击、滚动或导航已有标签页；这会打断用户当前工作。同时，复刻任务里的接口、字段和行为仍然必须真实请求验证，不能因为不能动标签页就靠猜。正确做法是优先使用后台真实请求、已打开页面的只读网络/DOM 信息，或新建/选择不会影响用户操作的调试页面；如果必须前台操作，先征得用户明确同意。例子：用户给了目标回答链接时，可以后台 fetch 回答 API 和赞同者 API，再读本地代码落实现；不能把用户正在使用的 Edge 标签页强行切到目标回答并继续点击 social credit，也不能不请求接口就凭字段名想象实现。

### social credit 预览名不能用列表顺序代替

复刻“某某、某某 等 N 人赞同了该回答”这类 social credit 时，不能把赞同者列表第一页的前两个人直接当成官方预览名字。列表接口负责查看完整赞同者，social credit 文案可能来自回答详情、页面初始数据或专门的社交关系字段，顺序和个性化规则都可能不同。必须先用真实网页或真实接口确认可见文案的字段来源，再决定 Android 展示逻辑；如果只找到了列表接口，也只能用于点击后的列表，不能反推顶部文案。例子：回答详情顶部文案应显示官方给出的两个预览名字，而不是把 `/voters?offset=0` 第一页第一、第二个用户拼进去。

### DevTools MCP 已注册但本轮不可用

当前会话可能没有热加载新 MCP。先确认：

```bash
codex mcp list
curl -s http://127.0.0.1:9222/json/version
```

如果两者都正常但工具仍不可用，要求重启对话后再继续。

### 能看到字段，但 App 一进页面就崩溃

先查 `logcat`，重点看：

- `JsonDecodingException`
- 新增字段名
- 对应 Repository / ViewModel / UI 渲染调用栈

常见原因是网页返回了 `0/1`、字符串数字、null 或空数组，而模型写成了过窄类型。

### 网页行为和 Android 行为不一致

回到 DevTools 复现原版操作，记录操作前后：

- DOM 变化
- 请求和响应
- URL / history 变化
- 可见状态和错误提示

不要只根据最终视觉状态补 Android 逻辑。

### UI 自动化 dump 看不到目标节点

`ui-test` / `uiautomator` 不一定能暴露所有富文本或 WebView 内部节点。处理顺序：

1. 先用 `dump` 确认页面和主要文本存在。
2. 再用截图确认视觉状态。
3. 必要时用手势或坐标点击目标区域，再用 `dump` 或截图验证结果。

### PeopleScreen 个人徽章在 web members 接口缺失

个人主页徽章不要只看 `https://www.zhihu.com/api/v4/members/{id}`。实测同一作者在这个接口里可能返回 `badge_v2: null`，但 `https://api.zhihu.com/people/{urlToken}?include=badge_v2,...` 会返回网页展示所需的 `badge_v2.detail_badges` 和官方图标。遇到 PeopleScreen 徽章缺失时，先用当前登录态分别请求两个接口对比字段，再决定 Android 数据源。

### 并行 Gradle 任务后出现大量无关错误

不要并行跑 `assemble` 和 `test`。Kotlin 增量缓存可能互相影响，表现成一串与当前改动无关的 `Unresolved reference`。处理方式：

```bash
./gradlew --stop
./gradlew assembleLiteDebug
./gradlew testLiteDebugUnitTest --tests your.test.Name
```

## References

### segment_infos 正文划线案例

这是正文划线功能的历史案例，不是本技能的通用流程。主要特点是字段同时出现在 feed 和回答详情，Web DOM 与 API 字段需要按 `pid + start/end` 对齐后才能复刻到 Android。

主要踩坑：

- `allow_segment_interaction` 可能返回 `0/1`，不能直接建模为裸 `Boolean`。
- `segment_infos` 可能为空数组，同一段内也可能有多个 `seg_ids`。
- `marks[*].start_index/end_index` 是范围切片，不能把整段文本合并成一个点击区域。
- 如果用户只要求正文 Markdown 渲染，不要顺手扩散到 feed 卡片或 WebView。

完整采集接口、字段结构、DOM 特征和回归样本见 [references/segment_infos.md](references/segment_infos.md)。
