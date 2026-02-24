@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.clipboardManager

/**
 * 根据用户设置处理分享操作
 * @param context Android Context
 * @param content 要分享的内容
 * @param onShowDialog 当需要显示对话框时调用
 */
fun handleShareAction(
    context: Context,
    content: NavDestination,
    onShowDialog: () -> Unit,
) {
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    val shareActionMode = preferences.getString("shareActionMode", "ask") ?: "ask"

    when (shareActionMode) {
        "ask" -> {
            // 显示对话框询问
            onShowDialog()
        }
        "copy" -> {
            // 直接复制链接
            (context as? MainActivity)?.sharedData?.clipboardDestination = content
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", getShareText(content)))
            Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
        }
        "share" -> {
            // 直接调用系统分享
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getShareText(content))
                putExtra(Intent.EXTRA_TITLE, getShareTitle(content))
            }
            val chooserIntent = Intent.createChooser(shareIntent, "分享到")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    }
}

@Composable
fun ShareDialog(
    content: NavDestination,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    context: Context,
    onNavigate: ((NavDestination) -> Unit)? = null,
) {
    @Composable
    fun MenuActionButton(
        icon: ImageVector,
        text: String,
        enabled: Boolean = true,
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick: () -> Unit,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() },
            shape = RoundedCornerShape(12.dp),
            color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showDialog,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismissRequest() },
        ) {
            AnimatedVisibility(
                visible = showDialog,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = EaseOutCubic),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = EaseInCubic),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) { /* 阻止点击穿透 */ },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        RoundedCornerShape(2.dp),
                                    ),
                            )
                        }

                        MenuActionButton(
                            icon = Icons.Filled.Share,
                            text = "分享",
                            onClick = {
                                onDismissRequest()
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                val chooserIntent = Intent.createChooser(shareIntent, "分享到")
                                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooserIntent)
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MenuActionButton(
                            icon = Icons.Filled.ContentCopy,
                            text = "复制链接",
                            onClick = {
                                onDismissRequest()
                                (context as? MainActivity)?.sharedData?.clipboardDestination = content
                                context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", shareText))
                                Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                            },
                        )

                        if (onNavigate != null) {
                            Spacer(modifier = Modifier.height(12.dp))

                            MenuActionButton(
                                icon = Icons.Filled.Settings,
                                text = "分享设置",
                                onClick = {
                                    onDismissRequest()
                                    onNavigate(Account.AppearanceSettings(setting = "shareAction"))
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

fun getShareText(content: NavDestination, title: String = "", authorName: String = ""): String? = when (content) {
    is Article -> {
        when (content.type) {
            ArticleType.Answer -> {
                "https://www.zhihu.com/answer/${content.id}\n【$title - $authorName 的回答】"
            }
            ArticleType.Article -> {
                "https://zhuanlan.zhihu.com/p/${content.id}\n【$title - $authorName 的文章】"
            }
        }
    }
    is Question -> {
        "https://www.zhihu.com/question/${content.questionId}\n【${content.title}】"
    }
    is Pin -> {
        "https://www.zhihu.com/pin/${content.id}"
    }
    else -> null
}

fun getShareTitle(content: NavDestination): String = when (content) {
    is Article -> content.title + when (content.type) {
        ArticleType.Answer -> " - ${content.authorName} 的回答"
        ArticleType.Article -> " - ${content.authorName} 的文章"
    }
    is Question -> content.title
    else -> "分享内容"
}
