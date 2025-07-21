package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.delay

class LocalHomeFeedViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = error("LocalHomeFeedViewModel should not be used directly. Use LocalFeedViewModel instead.")

    override suspend fun fetchFeeds(context: Context) {
        repeat(10) {
            delay(1000)
            displayItems.add(
                FeedDisplayItem(
                    title = "如果人类没有了性别之分是不是会少很多问题？",
                    summary = "不是。当前地球上没有选择压促使人变得没有性别。当前地球上没有以孤雌生殖延续的哺乳动物物种、没有以分裂之类无性生殖延续的多细胞哺乳动物物种。这问题的补充说明大概意味着提问者看了一些不准确的或故意错误的来源、对进化产生了很多误会。",
                    details = "因为您关注了用户 赵泠，这是来自TA的回答",
                    feed = null,
                    isFiltered = false
                )
            )
        }

        isLoading = false
    }
}
