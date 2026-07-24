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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.onboarding.illustration.DynamicColorImageVectors
import com.github.zly2006.zhihu.onboarding.illustration.coder
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.rememberSystemUrlOpener

const val OSS_NOTICE_CONTINUE_TAG = "oss_notice_continue"
const val OSS_NOTICE_SCREEN_TAG = "oss_notice_screen"

/**
 * 首装 / 重开开源说明页（UI 骨架对齐 Seal OpenSourceNoticePage + undraw coder 插画）。
 */
@Composable
fun OpenSourceNoticeScreen(
    onContinue: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    onOpenUrl: ((String) -> Unit)? = null,
) {
    val systemOpen = rememberSystemUrlOpener()
    val credits = remember { curatedOssCredits }
    val coderPainter = rememberVectorPainter(DynamicColorImageVectors.coder())

    if (onDismiss != null) {
        PlatformBackHandler(enabled = true, onBack = onDismiss)
    }

    fun openUrl(url: String) {
        if (onOpenUrl != null) onOpenUrl(url) else systemOpen(url)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag(OSS_NOTICE_SCREEN_TAG),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = coderPainter,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 28.dp),
                    )
                }
            }
            item {
                Text(
                    text = OssNoticeCopy.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = OssNoticeCopy.intro,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                NoticeSectionCard(
                    icon = Icons.Outlined.Link,
                    title = OssNoticeCopy.repoTitle,
                    body = OssNoticeCopy.repoBody,
                    actionLabel = ZHPLUS_REPO_URL,
                    onActionClick = { openUrl(ZHPLUS_REPO_URL) },
                )
            }
            item {
                NoticeSectionCard(
                    icon = Icons.Outlined.Security,
                    title = OssNoticeCopy.freeTitle,
                    body = OssNoticeCopy.freeBody,
                )
            }
            item {
                NoticeSectionCard(
                    icon = Icons.Outlined.Description,
                    title = OssNoticeCopy.projectLicenseTitle,
                    body = OssNoticeCopy.projectLicenseBody,
                    actionLabel = ZHPLUS_PROJECT_LICENSE,
                    onActionClick = { openUrl(ZHPLUS_LICENSE_URL) },
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(
                        Icons.Outlined.VolunteerActivism,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = OssNoticeCopy.creditsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = OssNoticeCopy.creditsBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }
            items(credits) { credit ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (credit.url != null) {
                                    Modifier.clickable { openUrl(credit.url) }
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    CreditRow(
                        title = credit.name,
                        author = credit.author,
                        description = credit.description,
                        license = credit.license,
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        Button(
            onClick = onContinue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag(OSS_NOTICE_CONTINUE_TAG),
        ) {
            Text(OssNoticeCopy.continueLabel)
        }
    }
}

@Composable
private fun NoticeSectionCard(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onActionClick),
                )
            }
        }
    }
}

@Composable
private fun CreditRow(
    title: String,
    author: String?,
    description: String?,
    license: String?,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!author.isNullOrBlank()) {
            Text(
                text = author,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (!license.isNullOrBlank()) {
            Text(
                text = license,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
