package com.github.zly2006.zhihu

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun loadImage(
    lifecycleOwner: LifecycleOwner,
    activity: Activity,
    httpClient: HttpClient,
    url: String,
    consumer: (Bitmap) -> Unit
) {
    GlobalScope.launch {
        try {
            httpClient.get(url).bodyAsChannel().toInputStream().buffered().use {
                val bitmap = BitmapFactory.decodeStream(it)
                activity.runOnUiThread {
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        consumer(bitmap)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("loadImage", "Failed to load image", e)
        }
    }
}
