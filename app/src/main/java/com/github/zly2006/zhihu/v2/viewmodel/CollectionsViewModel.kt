package com.github.zly2006.zhihu.v2.viewmodel

import com.github.zly2006.zhihu.v2.ui.Collection
import kotlin.reflect.typeOf

class CollectionsViewModel(
    val urlToken: String,
) : PaginationViewModel<Collection>(typeOf<Collection>()) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/people/$urlToken/collections"
}
