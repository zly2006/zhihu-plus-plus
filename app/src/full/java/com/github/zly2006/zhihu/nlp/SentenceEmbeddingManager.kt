package com.github.zly2006.zhihu.nlp

import android.content.Context
import com.ml.shubham0204.sentence_embeddings.SentenceEmbedding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 统一管理 Sentence-Transformers 模型的懒加载与状态。
 */
object SentenceEmbeddingManager {
    private const val MODEL_URL = "https://huggingface.co/shibing624/text2vec-base-chinese/resolve/main/model.onnx"
    private const val TOKENIZER_URL = "https://huggingface.co/shibing624/text2vec-base-chinese/resolve/main/tokenizer.json"

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
                val downloadedFiles = ModelManager.downloadModel(
                    resolvedContext,
                    "text2vec-base-chinese",
                    listOf(
                        ModelManager.RemoteFile(MODEL_URL, "model.onnx"),
                        ModelManager.RemoteFile(TOKENIZER_URL, "tokenizer.json"),
                    ),
                ) { progress ->
                    _state.value = ModelState.Downloading(progress)
                }

                _state.value = ModelState.Loading

                val modelFile = downloadedFiles["model.onnx"] ?: throw IllegalStateException("Model file not found")
                val tokenizerFile = downloadedFiles["tokenizer.json"] ?: throw IllegalStateException("Tokenizer file not found")

                val model = SentenceEmbedding()
                model.init(
                    modelFilepath = modelFile.absolutePath,
                    tokenizerBytes = tokenizerFile.readBytes(),
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

    suspend fun encode(text: String, context: Context? = null): FloatArray? {
        if (mutex.isLocked && embedding == null) return null // 模型正在加载，返回 null 表示无法编码
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
}
