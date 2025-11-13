package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.encryption.ZseEncryption
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Activity to verify the Kotlin implementation of ZSE encryption
 * against the original JavaScript implementation
 */
class ZseVerificationActivity : ComponentActivity() {
    
    private lateinit var webview: WebView
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup WebView for JavaScript execution
        webview = WebView(this)
        webview.setupUpWebviewClient()
        webview.settings.javaScriptEnabled = true
        webview.loadUrl("https://zhihu-plus.internal/assets/zse.html")
        
        setContent {
            ZhihuTheme {
                ZseVerificationScreen()
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ZseVerificationScreen() {
        var inputText by remember { mutableStateOf("test123456") }
        var kotlinResult by remember { mutableStateOf("") }
        var jsResult by remember { mutableStateOf("") }
        var comparisonResult by remember { mutableStateOf("") }
        var isVerifying by remember { mutableStateOf(false) }
        var resultColor by remember { mutableStateOf(Color.Gray) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ZSE加密验证") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "输入测试字符串",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isVerifying,
                            singleLine = true,
                            label = { Text("测试字符串") }
                        )
                        Button(
                            onClick = {
                                performVerification(
                                    inputText,
                                    onKotlinResult = { kotlinResult = it },
                                    onJsResult = { jsResult = it },
                                    onComparison = { result, color ->
                                        comparisonResult = result
                                        resultColor = color
                                    },
                                    onVerifyingChange = { isVerifying = it }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isVerifying && inputText.isNotEmpty()
                        ) {
                            Text(if (isVerifying) "验证中..." else "开始验证")
                        }
                    }
                }
                
                // Kotlin result
                if (kotlinResult.isNotEmpty()) {
                    ResultCard(
                        title = "Kotlin实现结果",
                        content = kotlinResult,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                
                // JavaScript result
                if (jsResult.isNotEmpty()) {
                    ResultCard(
                        title = "JavaScript实现结果",
                        content = jsResult,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                
                // Comparison result
                if (comparisonResult.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = resultColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "比对结果",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = comparisonResult,
                                style = MaterialTheme.typography.bodyMedium,
                                color = resultColor
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun ResultCard(title: String, content: String, color: Color) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = color
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
    
    /**
     * Perform verification by comparing Kotlin and JavaScript encryption results
     */
    private fun performVerification(
        input: String,
        onKotlinResult: (String) -> Unit,
        onJsResult: (String) -> Unit,
        onComparison: (String, Color) -> Unit,
        onVerifyingChange: (Boolean) -> Unit
    ) {
        onVerifyingChange(true)
        onKotlinResult("Computing...")
        onJsResult("Computing...")
        onComparison("Comparing...", Color.Gray)
        
        kotlinx.coroutines.MainScope().launch {
            try {
                // Get Kotlin result
                val kotlinResult = withContext(Dispatchers.Default) {
                    val startTime = System.currentTimeMillis()
                    val result = ZseEncryption.encrypt(input)
                    val endTime = System.currentTimeMillis()
                    Triple(result, endTime - startTime, result.length)
                }
                
                onKotlinResult(
                    "时间: ${kotlinResult.second}ms\n" +
                    "长度: ${kotlinResult.third} 字符\n" +
                    "结果: ${kotlinResult.first}"
                )
                
                // Get JavaScript result
                val jsResult = getJavaScriptResult(input)
                onJsResult(
                    "时间: ${jsResult.second}ms\n" +
                    "长度: ${jsResult.third} 字符\n" +
                    "结果: ${jsResult.first}"
                )
                
                // Compare results
                val match = kotlinResult.first == jsResult.first
                onComparison(
                    if (match) {
                        "✓ 验证通过！\n\n两种实现产生完全相同的加密结果。\n" +
                        "这证明Kotlin实现正确地复制了JavaScript的加密逻辑。"
                    } else {
                        "✗ 验证失败\n\n加密结果不一致：\n\n" +
                        "Kotlin长度: ${kotlinResult.third}\n" +
                        "JavaScript长度: ${jsResult.third}\n\n" +
                        "差异可能源于算法实现细节。"
                    },
                    if (match) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                
            } catch (e: Exception) {
                onComparison(
                    "错误: ${e.message}\n\n${e.stackTraceToString()}",
                    Color(0xFFF44336)
                )
            } finally {
                onVerifyingChange(false)
            }
        }
    }
    
    /**
     * Execute JavaScript encryption and return result
     */
    private suspend fun getJavaScriptResult(input: String): Triple<String, Long, Int> = withContext(Dispatchers.Main) {
        val future = CompletableDeferred<Triple<String, Long, Int>>()
        val startTime = System.currentTimeMillis()
        
        webview.evaluateJavascript("exports.encrypt('$input')") { result ->
            val endTime = System.currentTimeMillis()
            val cleanResult = result.trim('"')
            future.complete(Triple(cleanResult, endTime - startTime, cleanResult.length))
        }
        
        future.await()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webview.destroy()
    }
}
