/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.onboarding.illustration.DynamicColorImageVectors
import com.github.zly2006.zhihu.onboarding.illustration.coder
import kotlinx.coroutines.launch

const val PRODUCT_ONBOARDING_SCREEN_TAG = "product_onboarding_screen"
const val PRODUCT_ONBOARDING_NEXT_TAG = "product_onboarding_next"
const val PRODUCT_ONBOARDING_SKIP_TAG = "product_onboarding_skip"

private data class OnboardingPageContent(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val bullets: List<String> = emptyList(),
    val showIllustration: Boolean = false,
)

/**
 * 首次安装产品用户须知（UI 骨架对齐 Seal OnboardingPage：HorizontalPager + 圆点）。
 */
@Composable
fun ProductOnboardingScreen(onComplete: () -> Unit) {
    val pages = remember { productOnboardingPages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag(PRODUCT_ONBOARDING_SCREEN_TAG),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isLastPage) {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag(PRODUCT_ONBOARDING_SKIP_TAG),
                ) {
                    Text("跳过")
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) { page ->
            OnboardingPageBody(content = pages[page])
        }

        Row(
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pages.size) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier =
                        Modifier
                            .size(if (active) 8.dp else 6.dp)
                            .background(
                                color =
                                    if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                shape = CircleShape,
                            ),
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                ) {
                    Text("上一步")
                }
            } else {
                Spacer(Modifier.size(1.dp))
            }

            if (isLastPage) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.testTag(PRODUCT_ONBOARDING_NEXT_TAG),
                ) {
                    Text("开始使用")
                }
            } else {
                FilledTonalButton(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.testTag(PRODUCT_ONBOARDING_NEXT_TAG),
                ) {
                    Text("下一步")
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageBody(content: OnboardingPageContent) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (content.showIllustration) {
            Image(
                painter = rememberVectorPainter(DynamicColorImageVectors.coder()),
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(160.dp),
            )
            Spacer(Modifier.height(20.dp))
        } else {
            Icon(
                imageVector = content.icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(28.dp))
        }
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = content.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (content.bullets.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content.bullets.forEach { bullet ->
                    Text(
                        text = "•  $bullet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun productOnboardingPages(): List<OnboardingPageContent> =
    listOf(
        OnboardingPageContent(
            icon = Icons.Outlined.WavingHand,
            title = "欢迎使用 Zhihu++",
            body = "隐私增强的知乎客户端：本地推荐、广告屏蔽与内容过滤，把阅读体验还给你。",
            bullets =
                listOf(
                    "非官方客户端，源码在 GitHub 公开维护。",
                    "核心能力可在设置中随时调整。",
                    "继续前请阅读后续用户须知。",
                ),
            showIllustration = true,
        ),
        OnboardingPageContent(
            icon = Icons.Outlined.GppMaybe,
            title = "非官方客户端边界",
            body = "使用第三方客户端存在账号与合规风险，请自行评估。",
            bullets =
                listOf(
                    "请遵守知乎用户协议与当地法律法规。",
                    "请勿用于批量抓取、滥用接口或其他破坏服务的行为。",
                    "因使用本客户端导致的账号限制，需由用户自行承担。",
                ),
        ),
        OnboardingPageContent(
            icon = Icons.Outlined.VerifiedUser,
            title = "只信任官方更新来源",
            body = "Zhihu++ 永久免费。请勿购买所谓破解、VIP 或第三方收费包。",
            bullets =
                listOf(
                    "唯一源码与发行渠道：$ZHPLUS_REPO_URL",
                    "请通过仓库 Release / Nightly 安装，避免未知重打包。",
                    "任何收费分发都可能是诈骗。",
                ),
        ),
        OnboardingPageContent(
            icon = Icons.Outlined.Cloud,
            title = "本地数据与遥测",
            body = "浏览与过滤数据默认保存在本机；可选遥测用于改进体验。",
            bullets =
                listOf(
                    "账号凭证仅保存在本机存储。",
                    "可在系统与更新设置中关闭自动检查更新或遥测。",
                    "开发者选项默认关闭，连点版本号可开启。",
                ),
        ),
        OnboardingPageContent(
            icon = Icons.Outlined.Info,
            title = "准备就绪",
            body = "你已了解必须知道的事项。开始阅读吧。",
            bullets =
                listOf(
                    "账号页可随时重开「开源说明与鸣谢」。",
                    "完整第三方许可证见「开源许可」。",
                    "每次新构建可能展示「本次更新说明」。",
                ),
        ),
    )
