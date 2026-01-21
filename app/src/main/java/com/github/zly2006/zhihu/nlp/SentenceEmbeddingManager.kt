package com.github.zly2006.zhihu.nlp

import android.content.Context
import com.ml.shubham0204.sentence_embeddings.SentenceEmbedding
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 统一管理 Sentence-Transformers 模型的懒加载与状态。
 */
object SentenceEmbeddingManager {
    private const val MODEL_ASSET_PATH = "multilingual-MiniLM/model.onnx"
    private const val TOKENIZER_ASSET_PATH = "multilingual-MiniLM/tokenizer.json"

    sealed interface ModelState {
        data object Uninitialized : ModelState
        data object Loading : ModelState
        data object Ready : ModelState
        data class Error(val message: String) : ModelState
    }

    private val mutex = Mutex()
    private val _state = MutableStateFlow<ModelState>(ModelState.Uninitialized)
    val state: StateFlow<ModelState> = _state.asStateFlow()

    @Volatile
    private var defaultContext: Context? = null

    @Volatile
    private var embedding: SentenceEmbedding? = null

    fun setDefaultContext(context: Context) {
        defaultContext = context.applicationContext
    }

    suspend fun ensureModel(context: Context? = null): SentenceEmbedding {
        embedding?.let { return it }
        val resolvedContext = context?.applicationContext ?: defaultContext ?: throw IllegalStateException("SentenceEmbeddingManager default context not set")
        return mutex.withLock {
            embedding?.let { return it }
            _state.value = ModelState.Loading
            try {
                val model = SentenceEmbedding()
                val modelPath = ensureAssetFile(resolvedContext, MODEL_ASSET_PATH)
                val tokenizerBytes = readAssetBytes(resolvedContext, TOKENIZER_ASSET_PATH)
                model.init(
                    modelFilepath = modelPath,
                    tokenizerBytes = tokenizerBytes,
                    useTokenTypeIds = true,
                    outputTensorName = "last_hidden_state",
                    normalizeEmbeddings = true,
                )
                embedding = model
                _state.value = ModelState.Ready
                model
            } catch (e: Exception) {
                _state.value = ModelState.Error(e.message ?: "模型加载失败")
                embedding?.close()
                embedding = null
                throw e
            }
        }
    }

    suspend fun encode(text: String, context: Context? = null): FloatArray {
        val model = ensureModel(context)
        return model.encode(text)
    }

    suspend fun unload() {
        mutex.withLock {
            embedding?.close()
            embedding = null
            _state.value = ModelState.Uninitialized
        }
    }

    private suspend fun ensureAssetFile(
        context: Context,
        assetPath: String,
    ): String = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, assetPath)
        if (!targetFile.exists()) {
            targetFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        targetFile.absolutePath
    }

    private suspend fun readAssetBytes(
        context: Context,
        assetPath: String,
    ): ByteArray = withContext(Dispatchers.IO) {
        context.assets.open(assetPath).use { it.readBytes() }
    }
}
