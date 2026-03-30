package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AnnouncementCard(
    modifier: Modifier = Modifier,
    visible: Boolean,
    title: String,
    leadingIcon: @Composable () -> Unit,
    content: String? = null,
    accept: (@Composable () -> Unit)? = null,
    onAccept: () -> Unit = {},
    dismiss: @Composable () -> Unit,
    onDismiss: () -> Unit,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: AnnouncementCardColors = AnnouncementCardDefaults.colors(),
) {
    AnnouncementCard(
        modifier.padding(16.dp, 8.dp),
        visible,
        title,
        leadingIcon,
        content,
        actions = {
            Row(
                Modifier
                    .padding(16.dp, 0.dp, 16.dp, 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors().copy(
                        contentColor = colors.buttonContainerColor,
                    ),
                ) { dismiss() }
                accept?.let {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors().copy(
                            containerColor = colors.buttonContainerColor,
                            contentColor = colors.buttonContentColor,
                        ),
                    ) { accept() }
                }
            }
        },
        shape,
        colors,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnnouncementCard(
    modifier: Modifier = Modifier,
    visible: Boolean,
    title: String,
    leadingIcon: @Composable () -> Unit,
    content: String? = null,
    actions: @Composable () -> Unit,
    shape: Shape = CardDefaults.shape,
    colors: AnnouncementCardColors = AnnouncementCardDefaults.colors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        ),
        exit = shrinkVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        ) + fadeOut(),
    ) {
        Card(
            modifier,
            shape,
            CardDefaults.cardColors(
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
            ),
            elevation,
            border,
        ) {
            Column {
                Row(
                    Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier,
                        color = colors.iconContainerColor,
                        contentColor = colors.iconColor,
                        shape = CircleShape,
                    ) {
                        Box(Modifier.padding(8.dp)) {
                            leadingIcon()
                        }
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (content != null) {
                    Text(
                        text = content,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                actions()
            }
        }
    }
}

object AnnouncementCardDefaults {
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        iconContainerColor: Color = MaterialTheme.colorScheme.inversePrimary,
        iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        buttonContainerColor: Color = MaterialTheme.colorScheme.primary,
        buttonContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    ): AnnouncementCardColors = AnnouncementCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        iconContainerColor = iconContainerColor,
        iconColor = iconColor,
        buttonContainerColor = buttonContainerColor,
        buttonContentColor = buttonContentColor,
    )

    @Composable
    fun colorsVariant(
        containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
        iconContainerColor: Color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
        iconColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
        buttonContainerColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
        buttonContentColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    ): AnnouncementCardColors = AnnouncementCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        iconContainerColor = iconContainerColor,
        iconColor = iconColor,
        buttonContainerColor = buttonContainerColor,
        buttonContentColor = buttonContentColor,
    )

    @Composable
    fun colorsImportant(
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        iconContainerColor: Color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
        iconColor: Color = MaterialTheme.colorScheme.onPrimary,
        buttonContainerColor: Color = MaterialTheme.colorScheme.onPrimary,
        buttonContentColor: Color = MaterialTheme.colorScheme.primary,
    ): AnnouncementCardColors = AnnouncementCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        iconContainerColor = iconContainerColor,
        iconColor = iconColor,
        buttonContainerColor = buttonContainerColor,
        buttonContentColor = buttonContentColor,
    )
}

@Immutable
class AnnouncementCardColors(
    val containerColor: Color,
    val contentColor: Color,
    val iconContainerColor: Color,
    val iconColor: Color,
    val buttonContainerColor: Color,
    val buttonContentColor: Color,
) {
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        iconContainerColor: Color = this.iconContainerColor,
        iconColor: Color = this.iconColor,
        buttonContainerColor: Color = this.buttonContainerColor,
        buttonContentColor: Color = this.buttonContentColor,
    ): AnnouncementCardColors = AnnouncementCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        iconContainerColor = iconContainerColor,
        iconColor = iconColor,
        buttonContainerColor = buttonContainerColor,
        buttonContentColor = buttonContentColor,
    )
}

@Preview
@Composable
fun AnnouncementCardPreview() {
    AnnouncementCard(
        visible = true,
        title = "你好吗",
        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
        content = "我很好",
        dismiss = { Text("不好") },
        onDismiss = { /*TODO*/ },
        modifier = Modifier.width(360.dp),
    )
}
