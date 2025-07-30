# Zhihu++：注重隐私、互联网个人权利和无广告的知乎客户端

<img src="misc/zhihu_shit.png" width="100" height="100" />

Zhihu++独创本地推荐算法，把内容推荐完全放在本地进行，为您提供和筛选高质量内容。
本地推荐算法完全独立于知乎算法，依赖爬虫运行，可以自由定制各种推荐权重，保证看到自己想看的内容。
我相信，这点绵薄之力可以帮助广大用户从大公司的手中夺回本该属于我们的权利——选择自己的生活，不被算法奴役的权利。

**Update 2025/3/15**：更新，兼容最新版知乎签名算法。

[![GitHub release](https://img.shields.io/github/v/release/zly2006/zhihu-plus-plus)](https://github.com/zly2006/zhihu-plus-plus/releases)

[交流群](https://qm.qq.com/q/Rz6KFswFoK) 群号：619307382

[交流、反馈 discord](https://discord.gg/YCPFZV5XSA) （请在 my-other-apps/zhihu-plus-plus 频道讨论）

知乎手机客户端，蹲坑神器。去广告，去推广软文，去推销带货，去盐选专栏。

支持手机端/网页端/混合等多种推荐方案。

可以设置屏蔽词、AI屏蔽回答、屏蔽用户、屏蔽话题等（以上都是愿景）。

本项目不是自由软件，详见[授权协议](LICENSE.md)。

## 下载

告别知乎 110MB+ 的客户端，只要 3 MB！

[点我下载](https://github.com/zly2006/zhihu-plus-plus/releases)

[下载最新开发版本](https://github.com/zly2006/zhihu-plus-plus/releases/tag/nightly)

## 路线图

### 已经实现的功能

- 登录
  - 支持手机验证码登录
  - 支持通过扫码在电脑端登录
  - 支持手动设置cookie登录
- 首页推荐
  - 支持 Web 端推荐算法
  - 支持安卓端推荐算法
  - 支持切换 **登录状态 / 非登录状态** 下的推荐，防止信息茧房
- 阅读回答
- 阅读文章
- 朗读内容
  - 听文章
  - 听回答
- 回答页长按保存图片
- 过滤广告、软文和低质量内容
- 浏览器唤起
- 历史记录
- 收藏夹
- 其他
  - 支持 zse96 v2 签名算法（可以调用99%的网页端API）
  - 支持模拟安卓端 API 调用
- 其他（非知乎）
  - 提供了二维码扫码结果展示和复制功能，可用于提取网址、Wi-Fi密码等信息

### 实现了一半的功能

- 评论区
- 本地推荐

### 计划实现的功能

> [TODO](TODO.md)

- 屏蔽词
- 屏蔽用户（对长期发软文的用户联网屏蔽？）
- 屏蔽话题

WIP
