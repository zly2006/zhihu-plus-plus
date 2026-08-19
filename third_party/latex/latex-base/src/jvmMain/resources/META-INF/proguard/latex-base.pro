# latex-base ProGuard 嵌入式规则（JVM jar）
#
# 该文件位于 jar 内的 META-INF/proguard/ 路径下，是 ProGuard / R8 的官方约定：
#  - ProGuard 手册：https://www.guardsquare.com/manual/configuration/usage
#  - Android R8 文档：https://developer.android.com/build/shrink-code#library
# 使用本库的 Compose Desktop / 普通 JVM 应用在做 ProGuard 优化时会自动加载本规则，
# 无需任何额外配置。
#
# 内容必须与 consumer-rules.pro（Android AAR 用）保持一致：仅压制 fatal warning，
# 不要 -keep 任何包，避免阻止下游 shrink。
-dontwarn com.hrm.latex.**
