package com.github.zly2006.zhihu.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.zly2006.zhihu.v2.theme.ZhihuTheme

class ReadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhihuTheme {
                ZhihuMain()
            }
        }
    }
}

@Composable
fun ZhihuMain(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    false,
                    onClick = {

                    },
                    icon = {
                        Icon(Icons.Filled.Home, contentDescription = "推荐")
                    },
                    label = {
                        Text("推荐")
                    }
                )
                NavigationBarItem(
                    false,
                    onClick = {

                    },
                    icon = {
                        Icon(Icons.Filled.Home, contentDescription = "关注")
                    },
                    label = {
                        Text("关注")
                    }
                )
                NavigationBarItem(
                    false,
                    onClick = {

                    },
                    icon = {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "账号")
                    },
                    label = {
                        Text("账号")
                    }
                )
            }
        }
    ) { innerPadding ->
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ZhihuTheme {
        ZhihuMain()
    }
}
