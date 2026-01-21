package com.github.zly2006.zhihu.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ml.shubham0204.sentence_embeddings.SentenceEmbedding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

private const val MODEL_ASSET_PATH = "multilingual-MiniLM/model.onnx"
private const val TOKENIZER_ASSET_PATH = "multilingual-MiniLM/tokenizer.json"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceSimilarityTestScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sentenceEmbedding by remember { mutableStateOf<SentenceEmbedding?>(null) }
    var isModelLoading by remember { mutableStateOf(true) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }

    var sentence1 by remember { mutableStateOf("我喜欢研究自然语言处理。") }
    var sentence2 by remember { mutableStateOf("自然语言任务总是让我很兴奋。") }
    var similarity by remember { mutableStateOf<Float?>(null) }
    var inferenceTimeMs by remember { mutableStateOf<Long?>(null) }
    var computeError by remember { mutableStateOf<String?>(null) }
    var isComputing by remember { mutableStateOf(false) }

    LaunchedEffect(reloadToken) {
        isModelLoading = true
        modelError = null
        sentenceEmbedding?.close()
        sentenceEmbedding = null
        try {
            val embedding = SentenceEmbedding()
            val modelPath = copyAssetToInternalStorage(context, MODEL_ASSET_PATH)
            val tokenizerBytes = readAssetBytes(context, TOKENIZER_ASSET_PATH)
            embedding.init(
                modelFilepath = modelPath,
                tokenizerBytes = tokenizerBytes,
                useTokenTypeIds = true,
                outputTensorName = "last_hidden_state",
                normalizeEmbeddings = true,
            )
            sentenceEmbedding = embedding
        } catch (e: Exception) {
            modelError = e.localizedMessage ?: e.toString()
        } finally {
            isModelLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sentenceEmbedding?.close()
        }
    }

    val canCompute = !isModelLoading &&
        modelError == null &&
        !isComputing &&
        sentence1.isNotBlank() &&
        sentence2.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "句子相似度测试",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isModelLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .height(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = if (modelError == null) "MiniLM" else "加载失败",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "这个页面基于 Sentence-Transformers multilingual MiniLM 模型，实现与参考 Demo 类似的语义相似度计算，方便在设备上快速验证算法效果。",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (isModelLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("正在加载模型，大约需要几秒钟……", style = MaterialTheme.typography.bodySmall)
            } else if (modelError != null) {
                Text(
                    text = "模型加载失败：$modelError",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = {
                    reloadToken++
                }) {
                    Text("重试加载模型")
                }
            } else {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = sentence1,
                    onValueChange = { sentence1 = it },
                    label = { Text("第一句话") },
                    maxLines = 4,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = sentence2,
                    onValueChange = { sentence2 = it },
                    label = { Text("第二句话") },
                    maxLines = 4,
                )

                Button(
                    onClick = {
                        val embedding = sentenceEmbedding ?: return@Button
                        similarity = null
                        inferenceTimeMs = null
                        computeError = null
                        coroutineScope.launch {
                            isComputing = true
                            try {
                                val start = System.currentTimeMillis()
                                val (first, second) = withContext(Dispatchers.Default) {
                                    val firstTask = async { embedding.encode(sentence1) }
                                    val secondTask = async { embedding.encode(sentence2) }
                                    Pair(firstTask.await(), secondTask.await())
                                }
                                val score = cosineSimilarity(first, second)
                                similarity = score
                                inferenceTimeMs = System.currentTimeMillis() - start
                            } catch (e: Exception) {
                                Log.e("SentenceSimilarityTest", "Compute similarity failed", e)
                                computeError = e.localizedMessage ?: e.toString()
                            } finally {
                                isComputing = false
                            }
                        }
                    },
                    enabled = canCompute,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isComputing) "计算中..." else "计算相似度")
                }

                if (computeError != null) {
                    SelectionContainer {
                        Text(
                            text = "计算失败：$computeError",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                similarity?.let { score ->
                    val normalized = ((score + 1f) / 2f).coerceIn(0f, 1f)
                    Text("余弦相似度：${"%.4f".format(score)}", style = MaterialTheme.typography.titleMedium)
                    inferenceTimeMs?.let { Text("推理耗时：$it ms", style = MaterialTheme.typography.bodyMedium) }
                    LinearProgressIndicator(
                        progress = { normalized },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        val temp = sentence1
                        sentence1 = sentence2
                        sentence2 = temp
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("交换输入")
                }
            }
        }
    }
}

private suspend fun copyAssetToInternalStorage(
    context: Context,
    assetPath: String,
): String = withContext(Dispatchers.IO) {
    val outFile = File(context.filesDir, assetPath)
    if (!outFile.exists()) {
        outFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    outFile.absolutePath
}

private suspend fun readAssetBytes(
    context: Context,
    assetPath: String,
): ByteArray = withContext(Dispatchers.IO) {
    context.assets.open(assetPath).use { it.readBytes() }
}

private fun cosineSimilarity(
    first: FloatArray,
    second: FloatArray,
): Float {
    var dot = 0f
    var magFirst = 0f
    var magSecond = 0f
    for (i in first.indices) {
        dot += first[i] * second[i]
        magFirst += first[i] * first[i]
        magSecond += second[i] * second[i]
    }
    val denominator = (sqrt(magFirst) * sqrt(magSecond)).takeIf { it != 0f } ?: return 0f
    return dot / denominator
}
