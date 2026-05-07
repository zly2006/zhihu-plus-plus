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
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.edit
import java.util.Locale

object LocaleManager {
    const val PREF_KEY_LANGUAGE = "language"
    private const val PREF_LANGUAGE_MANUALLY_SET = "languageManuallySet"

    data class LanguageOption(
        val code: String,
        val displayNameResId: Int,
        val locale: Locale,
    )

    val SUPPORTED_LANGUAGES: List<LanguageOption> = listOf(
        LanguageOption("en", android.R.string.no, Locale.ENGLISH),
        LanguageOption("zh-CN", android.R.string.no, Locale.SIMPLIFIED_CHINESE),
        LanguageOption("zh-TW", android.R.string.no, Locale.TRADITIONAL_CHINESE),
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
            android.content.res.Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            android.content.res.Resources.getSystem().configuration.locale
        }
        return when {
            sysLocale.language == "zh" && sysLocale.country == "TW" -> "zh-TW"
            sysLocale.language == "zh" && sysLocale.country == "HK" -> "zh-TW"
            sysLocale.language == "zh" -> "zh-CN"
            else -> "en"
        }
    }

    fun getSelectedLocale(context: Context): Locale {
        return codeToLocale(getLanguageCode(context))
    }

    fun setLanguage(context: Context, languageCode: String) {
        prefs(context).edit {
            putString(PREF_KEY_LANGUAGE, languageCode)
            putBoolean(PREF_LANGUAGE_MANUALLY_SET, true)
        }
    }

    fun isLanguageManuallySet(context: Context): Boolean {
        return prefs(context).getBoolean(PREF_LANGUAGE_MANUALLY_SET, false)
    }

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
    fun applyAndRecreate(activity: Activity, languageCode: String) {
        setLanguage(activity, languageCode)
        activity.recreate()
    }

    private fun updateResources(context: Context, locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
        return context
    }

    private fun codeToLocale(code: String): Locale {
        return SUPPORTED_LANGUAGES.find { it.code == code }?.locale ?: Locale.ENGLISH
    }
}
