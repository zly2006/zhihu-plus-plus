package com.github.zly2006.zhihu.shared.platform

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

fun androidUserMessageSink(context: Context): UserMessageSink {
    val appContext = context.applicationContext
    return UserMessageSink(
        showShortMessage = { message ->
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        },
        showLongMessage = { message ->
            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
        },
    )
}

@Composable
actual fun rememberUserMessageSink(): UserMessageSink {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        androidUserMessageSink(context)
    }
}
