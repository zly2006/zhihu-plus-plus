package com.github.zly2006.zhihu.nlp

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SentenceEmbeddingManager {
    private val _state = MutableStateFlow<ModelState>(ModelState.Uninitialized)
    val state: StateFlow<ModelState> = _state.asStateFlow()

    fun setDefaultContext(context: Context) {}

    fun ensureModel(context: Context) {}

    suspend fun unload() {}
}
