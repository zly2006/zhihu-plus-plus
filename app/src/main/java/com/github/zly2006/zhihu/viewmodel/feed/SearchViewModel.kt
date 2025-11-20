package com.github.zly2006.zhihu.viewmodel.feed

import java.net.URLEncoder

class SearchViewModel(
    val searchQuery: String,
) : BaseFeedViewModel() {
    override val initialUrl: String
        get() {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            return "https://www.zhihu.com/api/v4/search_v3?gk_version=gz-gaokao&t=general&q=$encodedQuery&correction=1&search_source=Normal&limit=10"
        }

    // Override include to request necessary fields for search results
    override val include = "data[*].highlight,suggest_edit,is_normal,admin_closed_comment,reward_info,is_collapsed,annotation_action,annotation_detail,collapse_reason,is_sticky,collapsed_by,suggest_edit,comment_count,can_comment,content,editable_content,attachment,voteup_count,reshipment_settings,comment_permission,created_time,updated_time,review_info,relevant_info,question,excerpt,is_labeled,paid_info,paid_info_content,reaction_instruction,relationship.is_authorized,is_author,voting,is_thanked,is_nothelp,is_recognized;data[*].mark_infos[*].url;data[*].author.follower_count,vip_info,badge[*].topics;data[*].settings.table_of_contents.enabled"
}
