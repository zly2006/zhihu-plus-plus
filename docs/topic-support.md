# 话题支持

Zhihu++ 可将网页 `https://www.zhihu.com/topic/{id}`、`zhihu://topic/{id}` 和已出现的 pin20 topic 变体解析到原生话题页。个人关注列表、问题和想法中的话题也进入同一页面。

## 已验证请求契约

2026-08-13 使用独立 Edge profile 的已登录 Web 会话，以 DOM、JavaScript 和 CDP 网络记录重新核对话题详情、列表、关注和创作；仅用官方 Android 11.4.0 补充 Web 不提供的移动端页签和入口形态。关注操作结束后已恢复初始状态，测试草稿也已删除：

- `/api/v5.1/topics/{id}` 返回公网 ID、发布使用的内部 `topic_id`、纯文本 `excerpt`、头像、关注/问题/浏览/讨论数和关系状态。客户端直接读取 `excerpt`，不把带 `<p>` 的 `introduction` 当纯文本显示。
- 讨论的精华、最热、最新分别使用 v5.1 `top_activity/v2`、`essence/v2`、`timeline_activity/v2`；待回答使用 v5.1 `top_question/v2`。Web 连续滚动已观察到 `offset=20`、`offset=40`，客户端沿用触底自动分页，不提供“加载更多”按钮。
- 服务端分页可能返回 `172.16.201.121:80` 内部 origin，并把路径切换到 `essence_v4` 或 `timeline_activity_no_video`。客户端只把内部 origin 归一到 `https://www.zhihu.com/api/v4`，保留原始 path/query；不直连内网地址。
- 父话题、子话题和优秀答主分别来自 v3 `parent`、v3 `children`、v4 `best_answerers`，按已观察的强类型字段解码并支持本地导航。空的创作者墙不渲染占位区。
- 关注使用同一个 `/api/v4/topics/{id}/followers`：POST 返回 200 和 `is_following`，DELETE 返回 204。客户端执行乐观更新，失败时同时回滚状态、按钮文案和关注数。
- 全站搜索中的“话题”是独立顶层结果类型：使用 `search_v3?t=topic&show_all_topics=1`，按服务端 `paging.next` 触底分页；结果展示简介、浏览/讨论量，支持原生话题导航和可逆关注。创作输入的推荐仍使用专门的 `content/publish/topics/recommend`，不能与全站搜索混用。

官方 Android 的顶层话题页可见“讨论 / 想法 / 待回答”、关注入口和“发想法”操作；讨论内有“精华 / 最热 / 最新”，想法内有“最热 / 最新”。想法列表的首屏和服务端 `paging.next` 均使用 `www.zhihu.com/api/v5.1/topics/{id}/feeds/pin-hot` 或 `pin-new`，避免同一分页链路在 `api` 与 `www` 两个 host 间切换。客户端按真实窄模型解析字符串 ID、正文、作者、赞同/评论与导航。

发想法的话题不是外置选择器或 chips。官方 Web 编辑器输入 `#关键词` 后调用 `content/publish/topics/recommend`，选中项以内联实体留在正文中；实测连续插入 7 项仍可继续，因此不设置未经证实的数量上限。草稿中的 `hybrid.html` 使用 `hash_tag` 节点，同时 `topic.topics` 携带内部 `topic_id` 和 `#名称#`；Zhihu++ 按同一契约生成草稿与发布 payload，删除或改写内联实体时同步移除结构化话题。官方 Android 从话题页进入创作也把当前话题直接放进正文；服务端活动圈是独立灰度配置，不在客户端硬编码。

问题的话题只归问题：它们位于问题详情可折叠正文的最底部，正文折叠时一起折叠；回答页不重复展示。仓库目前没有文章创作入口，不能把想法发布协议套给文章。
