# 话题支持

Zhihu++ 可将网页 `https://www.zhihu.com/topic/{id}`、`zhihu://topic/{id}` 和已出现的 pin20 topic 变体解析到原生话题页。个人关注列表、问题和想法中的话题也进入同一页面。

## 已验证请求契约

2026-08-13 使用已登录 Web 会话对话题详情和列表进行了真实请求，并在操作后恢复初始关注状态：

- `/api/v4/topics/{id}` 返回 `id`、`name`、`introduction`、`avatar_url`、`followers_count`、`questions_count` 和 `is_following`。简介读取纯文本 `introduction`，不把百科内容当作同一字段。
- 讨论的热度、时间排序分别使用 v4 `top_activity`、`timeline_activity`；精华使用当前 Web 的 v5.1 `essence/v2`；待回答使用 v4 `unanswered_questions`。
- 服务端分页可能返回 `172.16.201.121:80` 内部 origin，并把路径切换到 `essence_v4` 或 `timeline_activity_no_video`。客户端只把内部 origin 归一到 `https://www.zhihu.com/api/v4`，保留原始 path/query；不直连内网地址。
- 父话题、子话题和优秀答主分别来自 v3 `parent`、v3 `children`、v4 `best_answerers`，按已观察的强类型字段解码并支持本地导航。空的创作者墙不渲染占位区。
- 关注使用同一个 `/api/v4/topics/{id}/followers`：POST 返回 200 和 `is_following`，DELETE 返回 204。客户端执行乐观更新，失败时同时回滚状态、按钮文案和关注数。
- 话题搜索使用 `search_v3?t=topic`。仅在解析边界移除 `object.name` 中已证实的 `<em>` / `</em>` 高亮标记，不做通用 HTML 展平。

官方 Android 11.4.0（API 31 AVD）的顶层话题页可见“讨论 / 想法 / 待回答”、关注入口和“发想法”操作。Reqable 抓包及登录请求验证了想法最热、最新分别使用 `api.zhihu.com/v5.1/topics/{id}/feeds/pin-hot` 和 `pin-new`；客户端按已观察的窄模型解析想法文本、作者、赞同/评论与导航，不把字符串正文误套进既有的想法内容数组模型。浏览/讨论量没有进入已验证详情字段，因此不伪造展示。

发想法最多选择五个话题。话题页传入的预选项和搜索选择项都进入草稿与发布的既有 `PinContentTopic` payload；空列表不序列化 `topic`。官方 Android 某样本的“发想法”由服务端活动配置预选了活动圈，而不是当前主话题；该行为属于活动或灰度规则，Zhihu++ 不硬编码，稳定地预选当前话题。仓库没有文章创作功能；回答的话题仍归属于问题，本次未为回答发布增加话题字段。
