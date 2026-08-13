# 话题支持

Zhihu++ 现在可把网页 `https://www.zhihu.com/topic/{id}`、`zhihu://topic/{id}` 和已出现的 pin20 topic 变体解析到原生话题页。个人关注列表与想法详情中的话题也会进入同一页面。

话题页使用历史稳定的 Web API 契约读取 `/api/v4/topics/{id}`，并以 `top_activity`、`essence`、`timeline_activity` 三个 feed 端点展示热门、精华和最新内容，支持服务端 `paging.next` 分页。详情字段均提供缺省值。当前登录态无法完成生产账号取证，因此关注状态只读，没有猜测关注写接口。

发想法可通过 `search_v3` 和 `show_all_topics=1` 搜索话题，最多选择五个。选中项同时进入草稿和发布的既有 `PinContentTopic` payload；空列表不序列化 `topic`。登录写路径仅以 MockEngine/序列化最终请求契约验证，本次不声称真实发布成功。

本文记录的是 Zhihu++ 已实现能力和公开/历史请求契约，不代表已验证知乎官方 Android 客户端的完整界面或灰度能力。仓库没有文章创作功能；回答的话题仍归属于问题，本次未为回答发布增加话题字段。
