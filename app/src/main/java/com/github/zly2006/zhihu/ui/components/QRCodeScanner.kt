package com.github.zly2006.zhihu.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.QRCodeScanActivity

@Composable
fun QRCodeLogin(
    modifier: Modifier = Modifier,
    onScanResult: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current

    // Activity 结果启动器
    val scanActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = result.data?.getStringExtra(QRCodeScanActivity.EXTRA_SCAN_RESULT)
            if (scanResult != null) {
                onScanResult?.invoke(scanResult) ?: run {
                    // 默认处理：显示扫描结果
                    Toast.makeText(context, "登录二维码: $scanResult", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 扫码登录按钮
    Button(
        onClick = {
            val intent = Intent(context, QRCodeScanActivity::class.java)
            scanActivityLauncher.launch(intent)
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "扫码登录",
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("扫码登录")
    }
}
