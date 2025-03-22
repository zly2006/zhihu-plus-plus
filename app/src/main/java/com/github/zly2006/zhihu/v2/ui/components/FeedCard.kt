@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.v2.viewmodel.BaseFeedViewModel

@Composable
fun FeedCard(
    item: BaseFeedViewModel.FeedDisplayItem,
    onClick: (Feed?) -> Unit
) {
    Card(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick(item.feed) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.Companion.padding(8.dp)
        ) {
            if (!item.isFiltered) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Companion.Ellipsis
                )
            }

            Text(
                text = item.summary,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Companion.Ellipsis,
                modifier = Modifier.Companion.padding(top = 3.dp)
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
