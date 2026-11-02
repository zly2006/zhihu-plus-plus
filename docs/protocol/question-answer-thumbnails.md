# 问题回答列表缩略图

2026-11-02 使用登录会话采集问题 `593275984` 的真实响应。私有原始数据保存在采集机仓库外；回归夹具为 `shared/src/jvmTest/resources/real-api/question-answer-thumbnails.json`，保留响应结构和图片地址，只对作者身份字段做一致性脱敏。

## 响应与请求

生产接口为 `GET https://www.zhihu.com/api/v4/questions/593275984/feeds?limit=20&order=default`，现有 include 为 `data[*].content,excerpt,headline,target.author.badge_v2`。Web 签名请求和 Android headers 请求均返回 `data[*].target.thumbnail_info`；无 include 和追加候选 `target.thumbnail,target.thumbnails,target.thumbnail_info` 的请求也进行了对照。当前样本的顶层 `thumbnail`、`thumbnails` 未提供图片，不能用现有同名字段代替嵌套对象。

```json
{"thumbnail_info":{"count":1,"type":"thumbnail_info","thumbnails":[{"height":375,"token":"v2-d4e49469d89079f500cd67ab8ae0c954","type":"image","url":"https://picx.zhimg.com/80/v2-d4e49469d89079f500cd67ab8ae0c954_720w.jpg?source=1def8aca","width":800}]}}
```

无图回答返回 `count: 0` 和空数组；采集样本覆盖 0、1、2、3 张图。三页分别返回 19、20、20 条记录，首屏额外连续重放四次均成功。三个回答的详情在无 include 时不返回该字段，候选 include 可返回；列表已满足展示需要，不追加详情请求。

## 实现与空间预算

复用 `DataHolder.ThumbnailInfo` 与 `ThumbnailRich`，经生产 `ZhihuJson.decodeJson` 解码到回答模型。问题列表取第一张 image 交给现有 FeedCard。摘要和缩略图共享原有一行，图片尺寸、裁切和 `showFeedThumbnail` 偏好沿用卡片实现；没有图片时不占位。点击仍使用原有回答导航。新增 API 请求数为 0，仅有图片组件本身的图片加载。

## 验证边界

真实响应夹具覆盖无图、单图和多图，不对字段在所有知乎数据源中的可用性作保证。本功能仅承诺问题回答列表的现有请求链路，不增加其他列表的显示入口。

图片执行面验证发现，样本原 URL 在 off 上使用默认 curl UA 返回 HTTP 403；仅增加 `Referer` 仍为 403，使用 `Mozilla/5.0` UA 返回 HTTP 200 image/png。本机 Python 请求同一 URL 也为 200。因此卡片缩略图统一通过带浏览器 UA 的 ImageRequest 加载，不携带账号 Cookie；默认 URL、缓存键和布局仍沿用原请求。
