package com.github.zly2006.zhihu

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

fun loadImage(
    lifecycleOwner: LifecycleOwner,
    activity: Activity,
    httpClient: HttpClient,
    url: String,
    consumer: (Bitmap) -> Unit
) {
    GlobalScope.launch(activity.mainExecutor.asCoroutineDispatcher()) {
        httpClient.get(url).bodyAsChannel().toInputStream().buffered().use {
            val bitmap = BitmapFactory.decodeStream(it)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                consumer(bitmap)
            }
        }
    }
}
