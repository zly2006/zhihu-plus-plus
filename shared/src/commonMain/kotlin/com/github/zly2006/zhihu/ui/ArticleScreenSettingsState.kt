package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction

class ArticleScreenSettingsState(
    isTitleAutoHide: Boolean,
    autoHideArticleBottomBar: Boolean,
    answerSwitchMode: String,
    pinAnswerDate: Boolean,
    useDuo3ArticleActions: Boolean,
    buttonSkipAnswer: Boolean,
    autoHideSkipAnswerButton: Boolean,
    answerDoubleTapAction: AnswerDoubleTapAction,
    useWebView: Boolean,
    private val saveAnswerDoubleTapActionPreference: (AnswerDoubleTapAction) -> Unit,
) {
    var isTitleAutoHide by mutableStateOf(isTitleAutoHide)
    var autoHideArticleBottomBar by mutableStateOf(autoHideArticleBottomBar)
    var answerSwitchMode by mutableStateOf(answerSwitchMode)
    var pinAnswerDate by mutableStateOf(pinAnswerDate)
    var useDuo3ArticleActions by mutableStateOf(useDuo3ArticleActions)
    var buttonSkipAnswer by mutableStateOf(buttonSkipAnswer)
    var autoHideSkipAnswerButton by mutableStateOf(autoHideSkipAnswerButton)
    var answerDoubleTapAction by mutableStateOf(answerDoubleTapAction)
    var useWebView by mutableStateOf(useWebView)

    fun saveAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        answerDoubleTapAction = action
        saveAnswerDoubleTapActionPreference(action)
    }
}

@Composable
fun rememberArticleScreenSettingsState(): ArticleScreenSettingsState {
    val settings = rememberSettingsStore()
    val state = remember(settings) {
        ArticleScreenSettingsState(
            isTitleAutoHide = settings.getBoolean("titleAutoHide", false),
            autoHideArticleBottomBar = settings.getBoolean("autoHideArticleBottomBar", false),
            answerSwitchMode = settings.getString("answerSwitchMode", "vertical"),
            pinAnswerDate = settings.getBoolean("pinAnswerDate", false),
            useDuo3ArticleActions = settings.getBoolean("duo3_article_actions", false),
            buttonSkipAnswer = settings.getBoolean("buttonSkipAnswer", true),
            autoHideSkipAnswerButton = settings.getBoolean("autoHideSkipAnswerButton", true),
            answerDoubleTapAction = settings.answerDoubleTapAction(),
            useWebView = settings.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false),
            saveAnswerDoubleTapActionPreference = { action ->
                settings.putString(
                    ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
                    action.preferenceValue,
                )
            },
        )
    }

    DisposableEffect(settings, state) {
        val unregister = settings.observeKeyChanges { key ->
            when (key) {
                "titleAutoHide" -> state.isTitleAutoHide = settings.getBoolean(key, false)
                "autoHideArticleBottomBar" -> {
                    state.autoHideArticleBottomBar = settings.getBoolean(key, false)
                }

                "buttonSkipAnswer" -> state.buttonSkipAnswer = settings.getBoolean(key, true)
                "autoHideSkipAnswerButton" -> state.autoHideSkipAnswerButton = settings.getBoolean(key, true)
                "answerSwitchMode" -> {
                    state.answerSwitchMode = settings.getString(key, "vertical")
                }

                "pinAnswerDate" -> state.pinAnswerDate = settings.getBoolean(key, false)
                "duo3_article_actions" -> state.useDuo3ArticleActions = settings.getBoolean(key, false)
                ARTICLE_USE_WEBVIEW_PREFERENCE_KEY -> state.useWebView = settings.getBoolean(key, false)
                ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY -> {
                    state.answerDoubleTapAction = settings.answerDoubleTapAction()
                }
            }
        }
        onDispose(unregister)
    }

    return state
}

private fun SettingsStore.answerDoubleTapAction(): AnswerDoubleTapAction =
    AnswerDoubleTapAction.fromPreference(
        getString(
            ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
            AnswerDoubleTapAction.Ask.preferenceValue,
        ),
    )
