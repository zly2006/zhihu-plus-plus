package com.github.zly2006.zhihu.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write

/**
 * Custom NavType for [ArticleType] enum.
 *
 * Required because the KMP navigation-compose library does not automatically
 * resolve NavTypes for @Serializable enums on non-Android targets (JVM/desktop).
 * See `NavTypeConverter.nonAndroid.kt` where `parseEnum()` returns `UNKNOWN`.
 */
object ArticleTypeNavType : NavType<ArticleType>(false) {
    override fun put(bundle: SavedState, key: String, value: ArticleType) {
        bundle.write { putString(key, value.name) }
    }

    override fun get(bundle: SavedState, key: String): ArticleType? = bundle.read {
        if (!contains(key) || isNull(key)) {
            null
        } else {
            getString(key)?.let { name -> ArticleType.entries.find { it.name == name } }
        }
    }

    override fun parseValue(value: String): ArticleType = ArticleType.entries.find { it.name == value } ?: ArticleType.Article

    override fun serializeAsValue(value: ArticleType): String = value.name
}
