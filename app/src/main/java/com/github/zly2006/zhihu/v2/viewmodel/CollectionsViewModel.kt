package com.github.zly2006.zhihu.v2.viewmodel

import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.v2.ui.CollectionItem
import kotlin.reflect.typeOf

class CollectionsViewModel : PaginationViewModel<CollectionItem>(typeOf<CollectionItem>()) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/people/${AccountData.data.self!!.url_token!!}/collections"
}
