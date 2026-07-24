-dontwarn com.hrm.latex.parser.tokenizer.LatexTokenizer$Companion

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

-keep interface com.chloemlla.zhplus.viewmodel.PaginationEnvironment {
    *;
}

-keep interface com.chloemlla.zhplus.viewmodel.CollectionContentEnvironment {
    *;
}

-keep interface com.chloemlla.zhplus.viewmodel.NotificationPaginationEnvironment {
    *;
}

-keep class * implements com.chloemlla.zhplus.viewmodel.PaginationEnvironment {
    *;
}

-keep class com.chloemlla.zhplus.viewmodel.filter.*_Impl {
    *;
}

-keep class com.chloemlla.zhplus.viewmodel.local.*_Impl {
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
