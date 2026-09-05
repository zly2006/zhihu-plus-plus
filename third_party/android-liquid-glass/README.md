# Android Liquid Glass 官方组件

来源：https://github.com/Kyant0/AndroidLiquidGlass/tree/1.0.6/catalog/src/main/java/com/kyant/backdrop/catalog

版本：1.0.6，commit `896a94a3ade1cc1a940b92365f942a34971fecda`。

官方 Maven `io.github.kyant0:backdrop:1.0.6` 只发布底层特效，README 明确说明不含高层组件。这里直接编译官方 `LiquidBottomTabs`、`LiquidBottomTab`、`LiquidButton`、`LiquidToggle`、`LiquidSlider` 及其依赖的源码，包名、绘制与弹性动画保留；应用只负责导航、主题环境与系统边距适配。上游源码不参与应用 ktlint 格式化。

授权见 `LICENSE`（Apache-2.0）。`UPSTREAM_SHA256SUMS` 记录原文件；`SHA256SUMS` 校验当前内置文件。升级时需检查并重新验证下述窄补丁。

## 已验证的集成修正

`patches/toggle-gesture-cancellation.patch`：API 35 上从关闭的开关滑块向上滚动设置列表，1.0.6 的手势观察器在 Main pass 看不到随后父列表消费的移动，且取消回调仍提交选择，实际偏好从 false 变为 true。在 Final pass 补查父布局是否消费手势，并为阻尼动画增加默认保持原行为的取消回调，只有 Toggle 在取消时恢复当前选择，不提交设置；其他官方组件不改变取消行为。将 Toggle 的同一个官方手势 modifier 从滑块移至整体容器，使轨道空白和 48 dp 点击区域同样可用，绘制与阻尼参数不变。应用适配层负责提供 64×48 dp 命中区域。

原始对照为提交 `b392a99a`。本地红绿脚本与截图在 `/tmp/zhihu-glass-evidence/`，未为这次验证添加无来源的永久 instrument 测试。

`patches/button-accessibility-height.patch`：200% 系统字体下官方按钮的固定 48 dp 高度截断第二行标签。改为最小高度与垂直内容留白，普通单行仍为 48 dp，多行按真实内容增高。没有修改玻璃绘制、点击或按压动画。风格选择行按 intrinsic height 对齐两个按钮，并居中多行标签。
