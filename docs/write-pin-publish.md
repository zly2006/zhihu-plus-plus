# 写想法发布链路记录

## 功能目标

在 Zhihu++ 内提供“发想法”入口，复用写回答的 Markdown 编辑、预览、错误复制和草稿保存能力。图片上传复用底层图床能力，但想法图片不复用回答的 Markdown 插图格式；发布成功后进入对应想法详情页。

非目标范围：

- 不实现圈子 / ring 配置、定时发布、视频附件和话题选择。
- 不改 WebView 正文渲染链路。
- 不接管网页版已有草稿列表和草稿恢复。

## 网页证据

已在登录态浏览器中验证首页“发想法”入口。打开编辑器后可见：

- 标题输入框，占位文案为“标题”。
- 正文编辑器，占位文案为“分享你此刻的想法...”。
- 发布按钮。

打开编辑器时触发的关键请求：

- `GET https://api.zhihu.com/content/publish/control/v2?scene=pin`
- `GET https://www.zhihu.com/content/publish/configs?scene=ring_config`
- `POST https://www.zhihu.com/content/publish/configs/v2`，请求体 `{"scene":"pin_config"}`
- `GET https://www.zhihu.com/api/v4/me/switches?include=oppose_right`
- `GET https://www.zhihu.com/api/v4/content/drafts/count?action=pin`

草稿保存请求：

- `POST https://api.zhihu.com/content/drafts`
- 顶层字段：`action=pin`、`data`
- `data.publish.traceId`：时间戳和 UUID 拼接
- `data.commentsPermission.comment_permission=all`
- `data.extra_info.view_permission=all`
- `data.extra_info.publisher=pc`
- `data.draft.disabled=1`
- `data.title.title`：可为空
- `data.hybrid.html`：编辑器 HTML
- `data.hybrid.textLength`：正文文本长度
- 标题、正文、媒体和话题均由官方 builder 按有无内容决定是否写入；空标题不会硬塞 `title`，空图片不会硬塞 `media`。

图片上传和发布 payload：

- 官方想法编辑器调用图片上传时使用 `source="pin"`，不是回答/文章使用的 `article`。
- 上传仍走 `POST https://api.zhihu.com/images`、OSS 上传、`PUT /images/{image_id}/uploading_status`、`GET /images/{image_id}` 轮询。
- 上传成功后，官方把图片保存到编辑器图片区，不写进 `hybrid.html`。
- 发布时每张图片追加为 `data.media.medias[].image`：
  - `height`
  - `width`
  - `url`：上传状态里的 `src`
  - `originalUrl`：上传状态里的 `original_src`
  - `watermark`：上传状态里的原始字符串，例如 `watermark`
  - `watermarkUrl`
- 当前官方网页 builder 不把 `image_id` 写入想法发布 payload。
- 若图片 URL 是 `pic-private.zhihu.com`，官方只在已有草稿/内容 token 时追加 `draft_token`；新想法没有 token 时保持上传状态里的 URL。

发布请求复用知乎内容发布端点：

- `POST https://www.zhihu.com/api/v4/content/publish`
- 顶层字段同样使用 `action=pin` 和上述 `data` 形态。
- 成功响应的 `data.result` 是 JSON 字符串，内部 `publish.id` 是发布后的想法 ID。
- 实测新想法发布成功响应也可能把 pin id 放在 `data.result` 的顶层 `id`，例如 `{"id":"...","type":"pin"}`。

真实验证记录：

- `source=pin` 上传图片得到 imageId `2051492456387043407`。
- 首次误用 `original_src` 作为 `media.image.url` 且把 `watermark` 改为布尔值时，发布接口返回 `code=2000`。
- 改为 `url=src`、`originalUrl=original_src`、`watermark=watermark` 后，`POST https://www.zhihu.com/api/v4/content/publish` 返回 `code=0`，生成测试想法 `2051493233436386836`。
- `GET https://www.zhihu.com/api/v4/pins/2051493233436386836` 返回 200，可读取到该想法。
- `DELETE https://www.zhihu.com/api/v4/pins/2051493233436386836` 返回 `{"success":true}`，再次 GET 返回 404，测试想法已删除。

## Android 适配

Android 页面不机械复制网页弹层，而是复用已有写回答编辑器：

- 首页顶部新增“发想法”图标入口。
- 新增 `WritePin` route。
- 写想法页面显示可选标题，正文或图片至少存在其一。
- 正文支持 Markdown 快捷工具栏、预览、草稿保存、发布。
- 图片按钮在回答页仍插入 Markdown 图片；在想法页使用 `source=pin` 上传，并维护单独的图片队列，提交时进入 `media.medias`。
- 回答专属的“生成目录”设置只在写回答页面展示。

## 兼容与验证点

- 请求字段保持网页抓到的 key 命名，不经过知乎响应模型的 snake/camel 转换。
- 标题、正文、图片字段按官方 builder 行为只在有值时发送。
- 想法图片不应转换为 Markdown `<img>` 或 `![]()`；只走 `media.medias[].image`。
- `textLength` 按官方逻辑由 HTML 去标签后的文本长度计算，不按 Markdown 源文本长度计算。
- 发布成功后用通用 `publish.id` 解析函数跳转 `Pin(id)`。
- 单测覆盖 `publish.id` 解析、文本想法 payload 关键字段和带图想法 media 字段。
