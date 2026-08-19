<!--
Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
Copyright (C) 2024-2026, zly2006 <i@zly2006.me>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation (version 3 only).

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
-->

# 后台 UI 调试协议

调试器从 stdin 读取 JSON Lines，从 stdout 返回同 id 的 JSON Lines。stderr 只承载应用日志。每个请求必须有 `id` 和 `op`；每个响应包含 `id`、`ok`、`elapsedMs`，失败时包含 `error`。

协议标记为 `ZHPP_BACKGROUND_UI_DEBUG_V1`。

启动器默认直接承载共享 `MacosZhihuMain`，用于页面与导航验收；传入 `--root=login` 才承载登录壳。两种模式都只使用离屏画布。

## 选择器

操作命令使用 `selector`：

```json
{"tag":"article-card","index":0,"useUnmergedTree":true}
```

选择器必须且只能提供 `tag`、`text`、`textContains`、`contentDescription` 之一。`index` 默认为 0，`useUnmergedTree` 默认为 true。禁止坐标字段。执行 `click` 或 `dismiss` 时目标节点自身必须暴露对应语义动作；图标描述位于子节点时，使用 `useUnmergedTree:false` 选择合并后的按钮，不能退回坐标点击。

## 命令

```json
{"id":"1","op":"state"}
{"id":"2","op":"dump","maxDepth":40,"useUnmergedTree":true}
{"id":"3","op":"list_clickables","useUnmergedTree":true}
{"id":"4","op":"click","selector":{"tag":"article-card","index":0}}
{"id":"4a","op":"dismiss","selector":{"text":"评论"}}
{"id":"5","op":"input","selector":{"tag":"search-input"},"text":"Kotlin Native","clear":true}
{"id":"6","op":"scroll","selector":{"tag":"feed-list"},"direction":"up"}
{"id":"6a","op":"key","key":"escape","selector":{"tag":"article_screen_root"}}
{"id":"6b","op":"back"}
{"id":"7","op":"wait","selector":{"textContains":"Kotlin Native"},"exists":true,"timeoutMs":5000}
{"id":"8","op":"wait_clickables","minimumCount":12,"timeoutMs":5000}
{"id":"9","op":"advance","milliseconds":300}
{"id":"10","op":"screenshot","file":"/tmp/zhpp-ui/article.png"}
{"id":"11","op":"quit"}
```

`click` 直接调用节点自身的 Compose `OnClick` 语义动作，不通过节点中心坐标合成鼠标事件；这也让被其他内容覆盖中心点的遮罩保持可测。`dismiss` 同理调用 Compose `Dismiss`，适合关闭弹层或对话框。`key` 只向离屏 Compose 场景发送按键，支持 `escape`、`enter`、`tab`、`space`、`backspace`、`delete`和四个方向；它不使用全局键盘注入。页面存在弹层等多个语义根时必须提供 `selector`，把按键发给明确的页面焦点容器，不能依赖根节点顺序。

`back` 直接驱动当前离屏 Compose 场景自带的 navigation event dispatcher，用于验证与真实窗口 Escape 相同的最内层已启用 `BackHandler`。它不依赖焦点，也不会注入全局按键；测试页面返回、弹层关闭和输入框抢焦点后的返回行为时必须使用 `back`，不能用 `key escape` 冒充系统返回。`scroll.direction` 可为 `up`、`down`、`left`、`right`，表示离屏画布内的手指滑动方向。`screenshot.file` 必须是 `/tmp` 下的显式绝对路径；调试器不会读取桌面，也不会自动选择用户目录。

`dump` 带 `selector` 时只输出目标子树，不带时输出整个根节点。`wait_clickables` 用于等待异步页面真正产出预期数量的可点击节点；`advance` 只推进 Compose 测试时钟，不能代替目标状态断言。

## 页面遍历

每进入一个页面：

1. `dump` 保存页面语义树。
2. `list_clickables` 获取当前可操作节点。
3. 对每个非破坏性节点依次 `click`，用 `wait` 验证目标状态，再走语义化返回入口恢复页面。
4. 对输入框执行输入、清空与提交边界；对滚动容器覆盖四个方向中实际支持的方向。
5. 截取关键状态，并验证 PNG 不为空且含非背景像素。
6. 记录未实现、异常、超时和无状态变化的节点；不能把它们标为通过。

同一页面因分页新增节点时继续遍历，直到节点集合和页面状态稳定。遇到登录、发布、支付、关注、投票或删除等外部副作用时，只在任务明确授权的范围内继续。
