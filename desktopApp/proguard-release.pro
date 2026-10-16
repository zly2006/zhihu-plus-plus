-dontwarn com.hrm.latex.parser.tokenizer.LatexTokenizer$Companion

# JavaFX 的工具包和原生窗口栈只能反射加载（javafx.toolkit 属性默认指向
# com.sun.javafx.tk.quantum.QuantumToolkit，glass/prism 同理），静态分析不可达；
# 缺失时运行时报 "No toolkit found"。JavaFX 内部反射遍布，按整包保留。
-keep class javafx.** { *; }
-keep class com.sun.javafx.** { *; }
-keep class com.sun.glass.** { *; }
-keep class com.sun.prism.** { *; }
-keep class com.sun.scenario.** { *; }
-keep class com.sun.pisces.** { *; }
-keep class com.sun.media.** { *; }
-keep class com.sun.openjfx.** { *; }
-keep class netscape.javascript.** { *; }

# JavaFX WebView 的 WebKit 桥接类由 JNI 和 NativeLibLoader 在运行时按名查找，静态分析不可达。
# 缺少它们时 release 版创建 WebView 会抛 NoClassDefFoundError（实测 com.sun.webkit.FileSystem），
# jfxwebkit.dll 无法装载，网页登录和所有 WebView 页面都无法打开。整包保留。
-keep class com.sun.webkit.** { *; }

# Coil 通过 ServiceLoader 创建网络下载器；服务描述仍在但 provider 被裁剪时，所有网络图片都会加载失败。
-keep class coil3.network.ktor3.internal.KtorNetworkFetcherServiceLoaderTarget { *; }

-keep class * implements io.ktor.client.HttpClientEngineContainer {
    *;
}

-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider {
    *;
}

-keep class io.ktor.client.engine.cio.CIOEngineContainer {
    *;
}

-keep class io.ktor.client.engine.java.JavaHttpEngineContainer {
    *;
}

-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.PaginationEnvironment {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.CollectionContentEnvironment {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.NotificationPaginationEnvironment {
    *;
}

-keep class * implements com.github.zly2006.zhihu.viewmodel.PaginationEnvironment {
    *;
}

-keep class com.github.zly2006.zhihu.viewmodel.filter.*_Impl {
    *;
}

-keep class com.github.zly2006.zhihu.viewmodel.local.*_Impl {
    *;
}

-keep class * extends androidx.room.RoomDatabase {
    *;
}

-keep @androidx.room.Database class * {
    *;
}

-keep @androidx.room.Dao class * {
    *;
}

-keep class androidx.sqlite.driver.bundled.** {
    *;
}

-keepclasseswithmembernames class * {
    native <methods>;
}
