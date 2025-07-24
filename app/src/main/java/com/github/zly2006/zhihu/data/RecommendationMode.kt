package com.github.zly2006.zhihu.data

enum class RecommendationMode(val displayName: String, val key: String, val description: String) {
    WEB("Web 端推荐", "server", "使用知乎网页端的推荐算法"),
    ANDROID("安卓端推荐", "android", "使用知乎安卓端的推荐算法（还没做盐选筛选）"),
    LOCAL("本地推荐", "local", "基于本地数据的推荐算法"),
    SIMILARITY("相似度推荐", "similarity", "基于内容相似度的推荐算法 (开发中)")
}
