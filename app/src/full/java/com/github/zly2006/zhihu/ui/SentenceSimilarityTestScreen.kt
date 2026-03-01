package com.github.zly2006.zhihu.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.nlp.ModelState
import com.github.zly2006.zhihu.nlp.SentenceEmbeddingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceSimilarityTestScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val modelState by SentenceEmbeddingManager.state.collectAsState()

    LaunchedEffect(context) {
        SentenceEmbeddingManager.setDefaultContext(context.applicationContext)
    }

    var sentence1 by remember { mutableStateOf("我喜欢研究自然语言处理。") }
    var sentence2 by remember { mutableStateOf("自然语言任务总是让我很兴奋。") }
    var similarity by remember { mutableStateOf<Float?>(null) }
    var inferenceTimeMs by remember { mutableStateOf<Long?>(null) }
    var computeError by remember { mutableStateOf<String?>(null) }
    var isComputing by remember { mutableStateOf(false) }

    val isModelReady = modelState is ModelState.Ready
    val isModelLoading = modelState is ModelState.Loading
    val modelError = (modelState as? ModelState.Error)?.message

    val canCompute = isModelReady &&
        !isComputing &&
        sentence1.isNotBlank() &&
        sentence2.isNotBlank()

    fun loadModel() {
        coroutineScope.launch {
            try {
                SentenceEmbeddingManager.ensureModel(context)
                computeError = null
            } catch (e: Exception) {
                Log.e("SentenceSimilarityTest", "Failed to load model", e)
                computeError = e.localizedMessage ?: e.toString()
            }
        }
    }

    fun unloadModel() {
        coroutineScope.launch {
            SentenceEmbeddingManager.unload()
        }
    }

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
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    when (val modelState = modelState) {
                        ModelState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .height(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        is ModelState.Downloading -> {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "下载中 ${(modelState.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(end = 16.dp),
                                )
                                LinearProgressIndicator(
                                    progress = { (modelState as ModelState.Downloading).progress },
                                    modifier = Modifier.padding(end = 16.dp).width(100.dp),
                                )
                            }
                        }
                        is ModelState.Error -> {
                            Text(
                                text = "加载失败",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(end = 16.dp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        ModelState.Ready -> {
                            Text(
                                text = "MiniLM",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        }
                        ModelState.Uninitialized -> {
                            Text(
                                text = "未加载",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        }
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
                text = "这个页面基于 text2vec-base-chinese 模型，实现与参考 Demo 类似的语义相似度计算，方便在设备上快速验证算法效果。",
                style = MaterialTheme.typography.bodyMedium,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val statusText = when (val state = modelState) {
                    ModelState.Uninitialized -> "当前状态：未加载"
                    ModelState.Loading -> "当前状态：正在加载模型……"
                    is ModelState.Downloading -> "当前状态：正在下载模型 ${(state.progress * 100).toInt()}%"
                    ModelState.Ready -> "当前状态：模型已就绪"
                    is ModelState.Error -> "当前状态：加载失败"
                }
                Text(statusText, style = MaterialTheme.typography.bodyMedium)
                if (modelError != null) {
                    Text(
                        text = modelError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { loadModel() },
                        enabled = !isModelReady && !isModelLoading,
                    ) {
                        Text(if (isModelLoading) "加载中..." else "加载模型")
                    }
                    TextButton(
                        onClick = { unloadModel() },
                        enabled = isModelReady && !isComputing,
                    ) {
                        Text("卸载模型")
                    }
                }
            }

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
                    similarity = null
                    inferenceTimeMs = null
                    computeError = null
                    coroutineScope.launch {
                        isComputing = true
                        try {
                            val start = System.currentTimeMillis()
                            val (first, second) = withContext(Dispatchers.Default) {
                                val firstTask = async { SentenceEmbeddingManager.encode(sentence1)!! }
                                val secondTask = async { SentenceEmbeddingManager.encode(sentence2)!! }
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

            computeError?.let { error ->
                Text(
                    text = "计算失败：$error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
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
