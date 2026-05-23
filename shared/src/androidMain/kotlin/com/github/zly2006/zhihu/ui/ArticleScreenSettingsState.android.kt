package com.github.zly2006.zhihu.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.github.zly2006.zhihu.shared.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction

@Composable
actual fun rememberArticleScreenSettingsState(): ArticleScreenSettingsState {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }
    val state = remember(preferences) {
        ArticleScreenSettingsState(
            isTitleAutoHide = preferences.getBoolean("titleAutoHide", false),
            autoHideArticleBottomBar = preferences.getBoolean("autoHideArticleBottomBar", false),
            answerSwitchMode = preferences.getString("answerSwitchMode", "vertical") ?: "vertical",
            pinAnswerDate = preferences.getBoolean("pinAnswerDate", false),
            useDuo3ArticleActions = preferences.getBoolean("duo3_article_actions", false),
            buttonSkipAnswer = preferences.getBoolean("buttonSkipAnswer", true),
            autoHideSkipAnswerButton = preferences.getBoolean("autoHideSkipAnswerButton", true),
            answerDoubleTapAction = preferences.answerDoubleTapAction(),
            useWebView = preferences.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false),
            saveAnswerDoubleTapActionPreference = { action ->
                preferences.edit {
                    putString(
                        ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
                        action.preferenceValue,
                    )
                }
            },
        )
    }

    DisposableEffect(preferences, state) {
        val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "titleAutoHide" -> state.isTitleAutoHide = preferences.getBoolean(key, false)
                "autoHideArticleBottomBar" -> {
                    state.autoHideArticleBottomBar = preferences.getBoolean(key, false)
                }

                "buttonSkipAnswer" -> state.buttonSkipAnswer = preferences.getBoolean(key, true)
                "autoHideSkipAnswerButton" -> state.autoHideSkipAnswerButton = preferences.getBoolean(key, true)
                "answerSwitchMode" -> {
                    state.answerSwitchMode = preferences.getString(key, "vertical") ?: "vertical"
                }

                "pinAnswerDate" -> state.pinAnswerDate = preferences.getBoolean(key, false)
                "duo3_article_actions" -> state.useDuo3ArticleActions = preferences.getBoolean(key, false)
                ARTICLE_USE_WEBVIEW_PREFERENCE_KEY -> state.useWebView = preferences.getBoolean(key, false)
                ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY -> {
                    state.answerDoubleTapAction = preferences.answerDoubleTapAction()
                }
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        }
    }

    return state
}

private fun SharedPreferences.answerDoubleTapAction(): AnswerDoubleTapAction =
    AnswerDoubleTapAction.fromPreference(
        getString(
            ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
            AnswerDoubleTapAction.Ask.preferenceValue,
        ),
    )
