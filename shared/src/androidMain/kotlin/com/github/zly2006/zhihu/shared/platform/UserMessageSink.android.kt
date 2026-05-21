package com.github.zly2006.zhihu.shared.platform

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberUserMessageSink(): UserMessageSink {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        UserMessageSink(
            showShortMessage = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            showLongMessage = { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            },
        )
    }
}
