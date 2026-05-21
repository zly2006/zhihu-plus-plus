package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable

enum class UserMessageDuration {
    Short,
    Long,
}

data class UserMessageSink(
    val showShortMessage: (String) -> Unit,
    val showLongMessage: (String) -> Unit = showShortMessage,
) {
    fun showMessage(
        message: String,
        duration: UserMessageDuration = UserMessageDuration.Short,
    ) {
        when (duration) {
            UserMessageDuration.Short -> showShortMessage(message)
            UserMessageDuration.Long -> showLongMessage(message)
        }
    }
}

@Composable
expect fun rememberUserMessageSink(): UserMessageSink
