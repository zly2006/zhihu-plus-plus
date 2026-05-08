/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.github.zly2006.zhihu.R
import java.util.Locale

object LocaleManager {
    const val PREF_KEY_LANGUAGE = "language"
    private const val PREF_LANGUAGE_MANUALLY_SET = "languageManuallySet"

    data class LanguageOption(
        val code: String,
        @StringRes
        val displayNameResId: Int,
        val locale: Locale,
    )

    val SUPPORTED_LANGUAGES: List<LanguageOption> = listOf(
        LanguageOption("en", R.string.language_en, Locale.ENGLISH),
        LanguageOption("zh-CN", R.string.language_zh_cn, Locale.forLanguageTag("zh-CN")),
        LanguageOption("zh-TW", R.string.language_zh_tw, Locale.forLanguageTag("zh-TW")),
    )

    private fun prefs(context: Context) = context.getSharedPreferences(
        "com.github.zly2006.zhihu_preferences",
        Context.MODE_PRIVATE,
    )

    fun getLanguageCode(context: Context): String {
        val p = prefs(context)
        val saved = p.getString(PREF_KEY_LANGUAGE, null)
        if (saved != null) return saved
        // 首次启动：根据系统语言自动选择
        return resolveSystemLanguage()
    }

    private fun resolveSystemLanguage(): String {
        val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.content.res.Resources
                .getSystem()
                .configuration
                .locales[0]
        } else {
            @Suppress("DEPRECATION")
            android.content.res.Resources
                .getSystem()
                .configuration
                .locale
        }
        val language = sysLocale.language
        val country = sysLocale.country
        val script = sysLocale.script
        return when {
            language == "zh" && script == "Hant" -> "zh-TW"
            language == "zh" && script == "Hans" -> "zh-CN"
            language == "zh" && country == "TW" -> "zh-TW"
            language == "zh" && country == "HK" -> "zh-TW"
            language == "zh" && country == "MO" -> "zh-TW"
            language == "zh" -> "zh-CN"
            else -> "en"
        }
    }

    fun getSelectedLocale(context: Context): Locale = codeToLocale(getLanguageCode(context))

    fun setLanguage(context: Context, languageCode: String) {
        applyProcessLocale(codeToLocale(languageCode))
        prefs(context).edit {
            putString(PREF_KEY_LANGUAGE, languageCode)
            putBoolean(PREF_LANGUAGE_MANUALLY_SET, true)
        }
    }

    fun isLanguageManuallySet(context: Context): Boolean =
        prefs(context).getBoolean(PREF_LANGUAGE_MANUALLY_SET, false)

    /**
     * 用于 Activity.attachBaseContext() 中，确保 Activity 使用正确的 locale。
     */
    fun wrapContext(base: Context): Context {
        val locale = getSelectedLocale(base)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(base, locale)
        } else {
            @Suppress("DEPRECATION")
            updateResourcesLegacy(base, locale)
        }
    }

    /**
     * 切换语言后重新创建 Activity 以应用新 locale。
     */
    fun applyAndRecreate(context: Context, languageCode: String) {
        setLanguage(context, languageCode)
        context.findActivity()?.recreate()
    }

    private fun updateResources(context: Context, locale: Locale): Context {
        applyProcessLocale(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        applyProcessLocale(locale)
        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
        return context
    }

    private fun applyProcessLocale(locale: Locale) {
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.setDefault(LocaleList(locale))
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun codeToLocale(code: String): Locale =
        SUPPORTED_LANGUAGES.find { it.code == code }?.locale ?: Locale.ENGLISH
}
