# Zhihu++ AI UI Design Guide

这份文档给 AI agent 快速建立 UI、导航、按钮和设置项的共同语义。改代码前仍然要读对应源码；这里负责告诉你先读哪里、状态从哪里来、一个开关会影响哪些界面。

## 先读顺序

1. `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation/NavDestination.kt`
2. `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt`
3. Android: `app/src/main/java/com/github/zly2006/zhihu/MainActivity.kt` 和 `app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMainAndroidState.kt`
4. Desktop: `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/DesktopZhihuMain.kt`
5. 设置页: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/*SettingsScreen.kt`
6. 复用组件: `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components/SettingItem.kt`、`FeedCard.kt`、`DraggableRefreshButton.kt`

UI 改动不要只看目标页面。至少用 `rg "<preferenceKey>" shared app` 查设置项的读取点，用 `rg "<NavDestinationName>" shared app` 查导航入口和测试。

## 导航模型

`NavDestination` 是所有可导航目标的源头，使用 Kotlin Serialization 作为 typed route。新增页面或参数先改 `NavDestination.kt`，再注册 `NavHost` route。

主壳是 `MainTabs`。`Home`、`Follow`、`HotList`、`Daily`、`OnlineHistory`、`Account` 是 `TopLevelDestination`，用于底栏和主 pager 选页；不要把旧 top-level tab 当作新的独立页面 push，除非源码已经明确注册了独立 route。`Follow` 对应两个相邻 pager page: 推荐和动态，并记住上一次选择。

底栏显示只在 `MainTabs` route 上出现，判断点是 `isTopLevelDest(navEntry)`。内容页、设置页、搜索页、通知页等已注册独立目的地会离开主壳，依赖返回栈回退。

Android 导航由 `MainActivity.navigate()` 承接，负责历史记录、剪贴板/外链跳转、视频特殊处理、内容打开来源埋点。Desktop 在 `DesktopZhihuMain.navigate()` 中实现同一语义。跨平台 UI 内部应优先用 `LocalNavigator.current.onNavigate(...)`，不要直接持有 Activity 或平台 NavController。

URL 解析集中在 `resolveContent()`。支持知乎问题、回答、文章、用户、视频、想法、搜索、`link.zhihu.com?target=` 和 `zhihu://` scheme。新增 deep link 必须同时考虑 Android intent、剪贴板解析和桌面打开路径。

## 主界面结构

`ZhihuMain` 是共同 UI 外壳。它读取 `ZhihuMainPreferenceState`，生成底栏项、主 pager 页、自动隐藏状态和 `NavHost` route。Android 与 Desktop 分别用平台 adapter 注入文章页、NLP 管理页、视频打开和转场。

底栏项顺序固定为: 主页、关注、热榜、日报、历史、账号。实际显示由 `bottom_bar_items` 决定，并经过 `normalizeBottomBarSelection()` 兜底。`duo3_home_account` 开启后，账号入口迁到主页头像，底栏选择规则会改变；这类改动必须一起检查主页账号入口、账号页历史快捷方式和底栏启动页。

`startDestination` 不是 NavHost 的 start route，而是主 pager 初始页。NavHost start route 始终是 `MainTabs`。如果用户选的启动页不在底栏里，`resolveValidStartDestinationKey()` 会回落到当前可用项。

## 按钮和交互约定

设置列表使用 `SettingItemGroup` 包组，单项用 `SettingItem`，开关用 `SettingItemWithSwitch`，总开关用 `SettingItemOverall`。需要从账号页跳入并高亮某个设置时，给目标项传 `settingKey`，导航参数使用 `Account.AppearanceSettings(setting = "...")` 或 `Account.RecommendSettings(setting = "...")`。

返回入口通常放在当前页面顶栏的 `navigationIcon`：设置页多用 `LargeTopAppBar`，普通列表页多用 `TopAppBar`。返回图标优先用自动镜像箭头，`contentDescription = "返回"`。列表页容器通常是 `Scaffold` 加可滚动内容，具体背景色和滚动容器以现有页面体系为准。

底栏按钮测试 tag 是 `nav_tab_${destination.name.lowercase()}`。设置页和账号页已有显式 test tag，新增可测试入口优先加稳定 tag，而不是让 UI 测试用坐标点。

分享按钮的行为由 `shareActionMode` 决定。`ask` 打开分享弹窗，`copy` 直接复制链接，`share` 直接调系统分享；弹窗的设置入口会跳到 `Account.AppearanceSettings(setting = "shareAction")`。

可拖动刷新 FAB 使用 `DraggableRefreshButton`，位置保存在 `"<preferenceName>-x"` 和 `"<preferenceName>-y"`。不要另写一套拖动定位逻辑；需要多个 FAB 时改 `preferenceName` 防止位置互相覆盖。

## 视觉体系

当前主线 UI 以 Material 3 为默认实现。改共同行为时优先放在 common runtime、状态或 shared component；改视觉时先确认影响的是共享组件、单个页面还是平台 adapter，避免只改入口而漏掉复用路径。

主题状态集中在 `ThemeManager` 和平台 `ThemeSettingsRuntime`。`themeMode` 控制明暗，`useDynamicColor` 控制 Material You 动态取色，`customThemeColor` 在动态取色关闭后生效，`backgroundColorLight` / `backgroundColorDark` 控制背景色，`luotianyi_color` 控制应用内浏览器工具栏色。

信息流卡片由 `FeedCard` 读取 `showFeedThumbnail`、`feedCardStyle`、`duo3_card_appearance`、`duo3_card_layout`、`duo3_card_large_title`。改这些 key 的语义时要同时查主页、关注、热榜、历史和搜索结果等复用卡片的页面。

文章、问题详情和想法正文会根据 `ARTICLE_USE_WEBVIEW_PREFERENCE_KEY` 在 WebView 与 Compose Markdown 之间切换；该常量当前值是 `webviewRender`。WebView 正文渲染只作为废弃路径保留，不再接受新功能；阅读体验新能力只接入 Compose Markdown 路径。Compose Markdown 路径依赖 `RenderMarkdown` 和 `SegmentedText` 的字号、行高和段间距设置。

## 设置项影响图

### 外观与阅读体验

| preference key | 入口 | 主要影响 | 注意 |
| --- | --- | --- | --- |
| `themeMode` | 主题模式 | 应用明暗主题 | 通过 `ThemeManager` 立即影响顶层主题 |
| `useDynamicColor` | Material You 动态取色 | Android 12+ 取系统壁纸色 | 关闭后自定义主题色才明显生效 |
| `customThemeColor` | 自定义主题色 | Material 3 主色 | 不要和动态取色同时假设生效 |
| `backgroundColorLight`, `backgroundColorDark` | 自定义背景颜色 | 明暗模式背景 | 按当前明暗模式写入不同 key |
| `contentFontSize`, `contentLineHeight`, `contentBlockSpacing` | 字号、行高、段间距 | 正文字号/行高、分段文本样式和 Markdown 正文块间距 | 查 `SegmentedText` 和 Markdown 渲染路径 |
| `showFeedThumbnail` | Feed 卡片缩略图 | 信息流卡片是否显示图 | 由复用 `FeedCard` 的页面读取 |
| `showRefreshFab` | 刷新 FAB | 首页/列表可拖动刷新按钮显示 | 123Duo3 总开关会关闭它 |
| `feedCardStyle` | 信息流样式 | `card` 或 `divider` | 影响 FeedCard 外层布局 |
| `webviewRender` | 使用 WebView 显示文章 | 文章、问题详情、想法正文渲染路径 | 常量名是 `ARTICLE_USE_WEBVIEW_PREFERENCE_KEY` |
| `webviewCustomFontName` | WebView 自定义字体 | WebView 注入字体 | 仅 WebView 路径 |
| `webviewHardwareAcceleration` | WebView 硬件加速 | Android WebView layer type | 兼容性/性能相关 |
| `titleAutoHide` | 自动隐藏回答标题 | 文章页顶部标题栏 | 查 `rememberArticleScreenSettingsState()` |
| `autoHideArticleBottomBar` | 自动隐藏回答底部按钮 | 文章页底部操作栏 | 与滚动方向有关 |
| `buttonSkipAnswer` | 显示跳转下一个回答按钮 | 文章页快速跳转按钮 | 123Duo3 总开关会关闭它 |
| `autoHideSkipAnswerButton` | 自动隐藏跳转按钮 | 跳转按钮滚动隐藏 | 仅 `buttonSkipAnswer` 开启时可见 |
| `pinAnswerDate` | 置顶回答日期 | 回答日期位置 | 影响文章正文布局 |
| `answerSwitchMode` | 回答切换手势 | `off` / `vertical` / `horizontal` | 会影响转场方向和手势冲突 |
| `answerDoubleTapAction` | 双击回答动作 | 双击正文后的动作 | 可在文章页内动态保存 |
| `bottom_bar_items` | 底栏页面选择 | 主 pager 页集合和底栏按钮 | 始终经 `normalizeBottomBarSelection()` |
| `startDestination` | 应用启动默认页面 | 主 pager 初始页 | 只允许选择已显示的底栏项 |
| `bottomBarTapScrollToTop` | 点击底栏回到顶部/刷新 | 选中 tab 再点时触发 scroll/refresh | 双击由 UI 测试用 `&&` 连续 adb 才可靠 |
| `autoHideTopBar` | 滚动时自动隐藏顶栏 | 主 tab 页顶栏可见性 | 复用底栏滚动信号 |
| `autoHideBottomBar` | 滚动时自动隐藏底栏 | 主 tab 页底栏可见性 | 只在 `MainTabs` 显示 |
| `shareActionMode` | 分享操作 | 分享按钮默认行为 | `ask` / `copy` / `share` |
| `showSearchHotSearch` | 搜索界面显示热搜 | 空搜索页内容 | 搜索页空查询状态读取 |
| `showSearchHistory` | 搜索历史 | 是否记录和展示新搜索 | 关闭后不再记录新的搜索 |
| `use_custom_nav_host` | 自定义导航 | 持久化的技术性导航开关 | 当前主要在设置页读写；实现效果前先 `rg` 运行时读取点 |
| `enable_predictive_back` | 预测性返回 | 持久化的 Android 14+ 返回动画开关 | 当前主要在设置页读写；实现效果前先 `rg` 运行时读取点 |

### 推荐系统与内容过滤

| preference key | 入口 | 主要影响 | 注意 |
| --- | --- | --- | --- |
| `recommendationMode` | 推荐算法 | Web、Android、本地、混合推荐 | 枚举在 `RecommendationMode.kt` |
| `loginForRecommendation` | 推荐内容时登录 | 获取推荐时是否带登录凭证 | 影响服务端推荐结果 |
| `enableQualityFilter` | 质量过滤规则 | 按赞同数、关注数等指标过滤 | 查 content filter settings/runtime |
| `enableContentFilter` | 智能内容过滤 | 过滤重复出现但未点击内容 | 关闭时相关子统计/开关应弱化 |
| `filterFollowedUserContent` | 过滤已关注用户内容 | 是否过滤关注用户内容 | 仅智能过滤开启时可操作 |
| `enableKeywordBlocking` | 关键词屏蔽 | 命中关键词时过滤 | 管理入口在 Blocklist |
| `enableUserBlocking` | 用户屏蔽 | 命中用户时过滤 | Feed 卡片更多菜单可新增屏蔽 |
| `enableTopicBlocking` | 主题屏蔽 | 命中主题时过滤 | 阈值项只在开启时显示 |
| `topicBlockingThreshold` | 主题屏蔽阈值 | 命中主题数量下限 | 必须为大于 0 的整数 |
| `blockZhihuAdPlatform` | 屏蔽知乎广告平台内容 | 匹配 `xg.zhihu.com` | 推广过滤 |
| `blockZhihuSchool` | 屏蔽知乎学堂内容 | 匹配 `d.zhihu.com` 或教育卡片 | 推广/课程过滤 |
| `blockWeChatOfficialAccount` | 屏蔽微信公众号文章 | 匹配微信外链 | 外链过滤 |
| `blockPaidContent` | 屏蔽知乎盐选付费内容 | 过滤会员付费内容 | mixed 推荐默认语义也提到盐选 |
| `reverseBlock` | 反向屏蔽 | 只保留广告和付费内容 | 调试/整活性质，别当普通过滤开关 |

### 系统、通知和开发者

| key/store | 入口 | 主要影响 | 注意 |
| --- | --- | --- | --- |
| `githubToken` | GitHub Token | 更新检查 API 限速 | 不要打印或提交真实 token |
| `autoCheckUpdates` | 自动检查更新 | 启动后后台检查 | 通过 update runtime 存取 |
| `checkNightlyUpdates` | Nightly 更新 | 是否检查每日构建 | Android updater 和 Desktop runtime 都读 |
| `allowTelemetry` | 遥测统计 | 匿名使用统计 | 不影响核心功能 |
| `continuousUsageReminderIntervalMinutes` | 防沉迷提醒 | 连续使用提醒间隔 | 0 表示关闭 |
| `developer` | 开发者模式 | 账号页显示开发者选项 | 账号页点击版本 5 次开启 |
| `enableSwipeReaction` | 开发者选项: 滑动反馈 | Feed 卡片左右滑动触发喜欢/不喜欢 | 由 `FeedCard` 读取，需要调用方提供喜欢/不喜欢回调 |
| `enableScrollEndHaptic` | 开发者选项: 滚动到底震动 | 滚动边界反馈行为开关 | 改前查具体 overScroll 使用点 |
| `showDebugOverlay` | 开发者选项: 调试悬浮窗 | 调试 Feed 详情显示 | 如果 `rg` 只命中设置页，先补运行时读取点 |
| `zse96_key` | 开发者签名请求 | 调试签名相关请求 | 只在开发者页处理 |
| NotificationSettingsStore | 通知设置 | 系统通知、应用内显示、自动已读、未读红点 | 不走普通 `SettingsStore` key 命名 |

## 123Duo3 UI/UX 开关

`duo3_all` 是批量开关。开启时会写入 `duo3_home_account`、`duo3_nav_style`、`duo3_card_appearance`、`duo3_card_layout`、`duo3_card_large_title`、`duo3_article_bar`、`duo3_article_actions`，并关闭 `showRefreshFab` 和 `buttonSkipAnswer`。

各子开关影响:

| key | 影响 |
| --- | --- |
| `duo3_home_account` | 主页头像承接账号入口，底栏账号项规则变化，账号页可能显示历史快捷方式 |
| `duo3_nav_style` | 底栏高度、标签显示、图标和选中样式变化 |
| `duo3_card_appearance` | Feed 卡片圆角、背景和阴影变化 |
| `duo3_card_layout` | Feed 卡片作者、图片、摘要行数和字体排版变化 |
| `duo3_card_large_title` | `duo3_card_layout` 开启后控制标题字号 |
| `duo3_article_bar` | 文章页顶/底栏框架开关；若 `rg` 只命中设置页，不要声称已有运行时效果 |
| `duo3_article_actions` | 文章页底部操作按钮样式变化 |

改任意 Duo3 开关时，至少检查 `AppearanceSettingsScreen`、`ZhihuMain`、`HomeScreen`、`AccountSettingScreen`、`FeedCard` 和 `ArticleScreen`。

## 新增 UI 的检查清单

1. 新 route: 先在 `NavDestination.kt` 定义 typed destination，再在 `ZhihuMain` 注册 route；涉及平台能力时同步补 Android/Desktop adapter。
2. 新主 tab: 同时改 `TopLevelDestination`、`MainTabPage`、底栏 `allBottomBarItems`、设置页的 `topLevelDestinationsInOrder`、默认选择和测试。
3. 新设置项: 记录 preference key、默认值、读取点、是否实时生效、是否需要重启、对应 test tag 和从账号页跳转高亮的 `settingKey`。
4. 新按钮: 优先复用 Material 3 组件和现有图标库，补稳定 test tag，描述点击后影响的状态或导航目标。
5. 新正文/卡片渲染逻辑: 同时确认 Compose Markdown、WebView、共享组件、平台 adapter、lite/full variant 差异。
6. 新手势: 明确方向、阈值、和现有回答切换/底栏自动隐藏/图片查看/Feed 滑动反馈的冲突关系。

## 验证入口

UI 代码改动按 `CLAUDE.md` 的 AVD 和 UI 双代理流程执行。文档或纯注释改动不需要启动 AVD，但仍应做 Markdown 自查、关键路径 `rg` 复核和 `git diff --check`。

常用定位命令:

```bash
rg "Account\\.AppearanceSettings|Account\\.RecommendSettings|NavDestination|MainTabs" shared app
rg "rememberSettingsStore|putBoolean|putString|putInt|getBoolean|getString|getInt" shared app
rg "testTag\\(|nav_tab_|ACCOUNT_SETTINGS_|APPEARANCE_SETTINGS_" shared app
```
