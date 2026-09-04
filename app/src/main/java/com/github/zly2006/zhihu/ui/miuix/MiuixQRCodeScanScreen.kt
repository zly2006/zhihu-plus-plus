/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.github.zly2006.zhihu.CaptureActivity
import com.github.zly2006.zhihu.QRCodeScanActivity.Companion.LOGIN_PREFIX
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.util.clipboardManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * QRCodeScanActivity 的 miuix 表现层。签名与 [com.github.zly2006.zhihu.QRCodeScanScreen] 一致，
 * 业务逻辑（相机权限、ScanContract、登录前缀判断）原样照抄，只换组件。
 */
@Composable
fun MiuixQRCodeScanScreen(
    onBack: () -> Unit,
    onScanResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val showResultSheet = remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf("") }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "需要相机权限才能扫描二维码", Toast.LENGTH_SHORT).show()
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            if (result.contents.startsWith(LOGIN_PREFIX)) {
                onScanResult(result.contents)
            } else {
                scanResult = result.contents
                showResultSheet.value = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "扫码登录",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIconsEmbedded.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "扫码登录",
                modifier = Modifier.size(120.dp),
                tint = MiuixTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "扫描电脑端登录二维码",
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "在电脑浏览器中打开知乎网站，使用此功能扫描登录二维码",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (hasCameraPermission) {
                        val options = ScanOptions().apply {
                            setPrompt("将登录二维码对准扫描框")
                            setBeepEnabled(false)
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setCaptureActivity(CaptureActivity::class.java)
                        }
                        scanLauncher.launch(options)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(0.8f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasCameraPermission) "开始扫描" else "授权相机权限",
                        color = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }

    MiuixQRResultSheet(
        show = showResultSheet,
        result = scanResult,
        onCopy = {
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("QR扫描结果", scanResult))
            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        },
        onConfirm = { onScanResult(scanResult) },
    )
}

@Composable
private fun MiuixQRResultSheet(
    show: MutableState<Boolean>,
    result: String,
    onCopy: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowBottomSheet(
        show = show.value,
        onDismissRequest = { show.value = false },
        title = "扫描结果",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(text = "扫描到的内容：", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(Modifier.height(8.dp))
            Card {
                Text(text = result, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(text = "复制", onClick = onCopy, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        show.value = false
                        onConfirm()
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("确定", color = MiuixTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
