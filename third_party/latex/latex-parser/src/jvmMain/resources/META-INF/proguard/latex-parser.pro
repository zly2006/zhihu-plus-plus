# latex-parser ProGuard 嵌入式规则（JVM jar）
#
# 该文件位于 jar 内的 META-INF/proguard/ 路径下，是 ProGuard / R8 的官方约定：
#  - ProGuard 手册：https://www.guardsquare.com/manual/configuration/usage
#  - Android R8 文档：https://developer.android.com/build/shrink-code#library
# 使用本库的 Compose Desktop / 普通 JVM 应用在做 ProGuard 优化时会自动加载本规则，
# 无需任何额外配置。
#
# 内容必须与 consumer-rules.pro（Android AAR 用）保持一致：仅压制 fatal warning，
# 不要 -keep 任何包，避免阻止下游 shrink。
#
# 背景：Kotlin 编译器在 `companion object` + `private inline fun` + `private val`
# 组合下会把私有字段提升到 outer 类的 synthetic 静态字段；某些 Kotlin/KMP 版本
# 组合下，*$Companion.class 仍按原名引用 outer 类的字段，导致 ProGuard 全图静态
# 校验阶段抛 unresolved 并把 warning 升级为 fatal。运行期 JVM 惰性解析不触发，
# 但 ProGuard 构建期会强制失败 —— 故仅在此压制对应 warning。
-dontwarn com.hrm.latex.**
