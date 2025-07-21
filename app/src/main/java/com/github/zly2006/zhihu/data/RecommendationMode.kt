package com.github.zly2006.zhihu.data

enum class RecommendationMode(val displayName: String, val key: String, val description: String) {
    SERVER("服务器推荐", "server", "使用知乎服务器的推荐算法"),
    LOCAL("本地推荐", "local", "基于本地数据的推荐算法"),
    SIMILARITY("相似度推荐", "similarity", "基于内容相似度的推荐算法 (开发中)")
}
