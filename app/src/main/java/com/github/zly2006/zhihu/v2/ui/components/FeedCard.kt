@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.v2.viewmodel.BaseFeedViewModel

@Composable
fun FeedCard(
    item: BaseFeedViewModel.FeedDisplayItem,
    onClick: BaseFeedViewModel.FeedDisplayItem.(Feed?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick(item, item.feed) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            if (!item.isFiltered) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.avatarSrc?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Avatar",
                            modifier = Modifier.clip(CircleShape)
                                .size(36.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = item.summary ?: "",
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Companion.Ellipsis,
                modifier = Modifier.padding(
                    top = if (item.isFiltered) 0.dp else 3.dp
                )
            )

            Text(
                text = item.details,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
