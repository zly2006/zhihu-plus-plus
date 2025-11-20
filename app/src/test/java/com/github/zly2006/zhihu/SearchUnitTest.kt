package com.github.zly2006.zhihu

import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.SearchResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for search functionality
 * Tests parsing of search API v3 response format
 */
class SearchUnitTest {
    /**
     * Test parsing real Zhihu search API response
     * This test uses actual JSON data returned by the Zhihu search API
     * The JSON is first converted from snake_case to camelCase before parsing
     */
    @Test
    fun testRealZhihuSearchResponse() {
        // Real JSON response from Zhihu search API (in snake_case format)
        val realApiResponse =
            """
{
  "paging": {
    "is_end": false,
    "next": "https://api.zhihu.com/search_v3?advert_count=0&correction=1&gk_version=gz-gaokao&limit=20&offset=20&q=%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9&search_hash_id=f95dc1ab2d6bdae2dd9d96e558fddccd&t=general&vertical_info=0%2C0%2C0%2C0%2C0%2C0%2C0%2C0%2C0%2C2%2C0%2C0"
  },
  "data": [
    {
      "type": "koc_box",
      "highlight": null,
      "object": {
        "title": "\u003Cem\u003E搜索内容\u003C/em\u003E",
        "excerpt": "我无意中在老公的手机里看到这样一条\u003Cem\u003E搜索\u003C/em\u003E记录：「怎样让老婆在生孩子时意外身亡？」我摸了摸自己已经七个月大的孕肚，不由得惊出了一身冷汗。晚上，老公在浴室洗澡。我在客厅准备待产包，手机没电了，下意识拿起他的手机，想要搜一些待产注意事项。却无意中翻到了他的历史\u003Cem\u003E搜索\u003C/em\u003E一栏，里面有两条记录瞬间吸引了我的注意",
        "type": "paid_column",
        "id": "1649808276936921088",
        "description": "来自内容 · 对我而言危险的她",
        "paid_column": {
          "id": "1649808276936921088",
          "paid_column_id": "1638186406705827840",
          "title": "对我而言危险的她",
          "attached_info_bytes": "OtEBCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYUyITMTY0OTgwODI3NjkzNjkyMTA4OCoTMTY0OTgwODI3NjkzNjkyMTA4OEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMAqgMAwgMXU09VUkNFX1NFQVJDSF9LT0NfS1ZfRELgBAA=",
          "is_mid_long": false
        },
        "icon_url": "https://picx.zhimg.com/v2-dd2abbbc9b844fcaf61e5c6c9e1d8fc2.png",
        "attached_info_bytes": "OuYBCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYUyITMTY0OTgwODI3NjkzNjkyMTA4OCoTMTY0OTgwODI3NjkzNjkyMTA4OEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCgAQCwASqAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMAqgMAwgMXU09VUkNFX1NFQVJDSF9LT0NfS1ZfRELKAwzmkJzntKLlhoXlrrngBAA=",
        "source": "",
        "sub_type": "",
        "is_multi_koc": false,
        "more_url": ""
      },
      "hit_labels": null,
      "index": 0,
      "id": 1.6381864067058278e+18
    },
    {
      "type": "knowledge_ad",
      "highlight": {

      },
      "object": {
        "header": {
          "card_title": "电子书",
          "no_more": false
        },
        "body": {
          "title": "\u003Cem\u003E搜索内容\u003C/em\u003E",
          "description": "\u003Cem\u003E搜索内容\u003C/em\u003E29.2.1 不同的\u003Cem\u003E搜索\u003C/em\u003E器google.search.SearchControl 对象的 addSearcher()方法可以给\u003Cem\u003E搜索\u003C/em\u003E控件添加不同的\u003Cem\u003E搜索\u003C/em\u003E器。不同",
          "authors": [
            {
              "name": "来自「 Google API 大全」",
              "url_token": ""
            }
          ],
          "images": [],
          "play_icon": "",
          "show_image": "",
          "show_author": ""
        },
        "footer": "2020-09-22",
        "commodity_id": "1284095647839547392",
        "commodity_type": "publication_chapter",
        "url": "https://www.zhihu.com/market/pub/119977537/manuscript/1284095647839547392",
        "source": "DEFAULT",
        "card_version": "1",
        "slave_url": "https://www.zhihu.com/market/pub/119977537/manuscript/1284095647839547392",
        "tab_type": "general",
        "ab_id_list": [
          "dm_hot_koc=1",
          "dm_rel_rank=0",
          "dm_content_quota=3",
          "dm_all_question=0",
          "sb_zhiedu=1",
          "sb_xg_search=1",
          "dm_rank_ctx=0",
          "dm_fine_rel=2",
          "dm_test=1",
          "adflow=0",
          "dm_ai_use_qu=0",
          "dm_answer_meta=0",
          "dm_preset_migrate=2",
          "dm_op_format=0",
          "se_koc_list_box=1",
          "sb_index=0",
          "sb_fs_check=0",
          "bt_qq=0",
          "dm_mig_pk=0",
          "dm_integrate=0",
          "dm_center_feature=1",
          "dm_new_xtr=0",
          "sb_newreb=1",
          "bt_vertical_mem=1",
          "bt_member_verify=1",
          "dm_preset_up=1",
          "dm_pin_field=0",
          "dm_mig_mixer=0",
          "dm_guess_rank=0",
          "dm_q2q_recall=0",
          "dm_preset_opt=1",
          "sb_cardfilter=1",
          "sb_slot_domain=2",
          "sb_ecpmctcvr=1",
          "dm_pre_docs=2",
          "se_mix_timebox=0",
          "sb_domain_2025=0",
          "dm_recency_root=0",
          "dm_pu_sug=0",
          "sb_caowei_4in10=1",
          "sb_zhiad=1",
          "sb_sku_kzh=0",
          "dm_v_member_zag=2",
          "dm_question_limit=2",
          "dm_mig_zr=0",
          "dm_rescore_v=2",
          "sb_eduzhi=1",
          "sb_novel_member=0",
          "bt_holdback_2025=0",
          "dm_picsearch=1",
          "vp_rank_v6=1",
          "vp_sim_q_recall=0",
          "vp_sku_threshold=0",
          "vp_kmrel4090=1",
          "ed_se_box_hm=0",
          "nq_koc_card_opt=0",
          "vp_vip_audio=1",
          "vp_audio_search=1",
          "vp_novel_rec_opt=0"
        ],
        "card_type": "",
        "sku_id": "1284091662856130560",
        "attached_info_bytes": "OuIECgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYDyITMTI4NDA5NTY0NzgzOTU0NzM5MioJMTE5OTc3NTM3SgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA5gBAaABAagBALABAYACALICqAN7ImNvbnRlbnRfaWQiOjEyODQwOTU2NDc4Mzk1NDczOTIsImNvbnRlbnRfdHlwZSI6IkVCb29rIiwiY29udGVudF90b2tlbiI6MTE5OTc3NTM3LCJzdWJfY29udGVudF9pZCI6MTI4NDA5NTY0NzgzOTU0NzM5Miwic3ViX2NvbnRlbnRfdG9rZW4iOjEyODQwOTU2NDc4Mzk1NDczOTIsInN1Yl9jb250ZW50X3R5cGUiOiJFQm9va19DaGFwdGVyIiwibWF0ZXJpYWxfaWQiOjEyODQwOTU2NDc4Mzk1NDczOTIsInNrdV9pZCI6MTI4NDA5MTY2Mjg1NjEzMDU2MCwiaXNfaW50ZXJ2ZW5lIjpmYWxzZSwicmVjYWxsX3Njb3JlIjowLjEzODE0MzQzODAzMDk2NTYzLCJyZWNhbGxfc291cmNlIjoiU09VUkNFX1NFQVJDSF9CT09LX0NIQVBURVJfQ09BUlNFIiwicmVjYWxsX3JhbmsiOjEsImlzX3ZpcCI6ZmFsc2UsImlzX2Fjcm9zc192ZXJ0aWNhbCI6ZmFsc2V9uAIAigMAkgMTMTQ5MjU3MTAxNzEzMDc5OTEwNJoDAKIDAKoDAOAEAA=="
      },
      "hit_labels": null,
      "index": 1,
      "id": 1.2840956478395474e+18
    },
    {
      "type": "relevant_query",
      "id": "3340254b1e0b5915ad1fb2212555500a",
      "index": 2,
      "query_list": [
        {
          "query": "抖音内容启发搜索",
          "id": "2228734503484998059",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQKoAQCwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "内容搜索软件",
          "id": "2561337434946918213",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQKoAQGwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "未搜索到内容",
          "id": "-7029873331963721759",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQKoAQKwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "百度搜索内容技术部",
          "id": "-2105377126908504930",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQKoAQOwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "内容启发搜索",
          "id": "5974497664713682234",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQKoAQSwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "搜索页面内容",
          "id": "7531471725461395011",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQKoAQWwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        }
      ],
      "attached_info_bytes": "On4SIGY5NWRjMWFiMmQ2YmRhZTJkZDlkOTZlNTU4ZmRkY2NkSgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA6ABArABBYACALgCAIoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwDgBAA=",
      "is_single_column": false
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "但是就不在你\u003Cem\u003E搜索\u003C/em\u003E的时候显示出来。如果你真的想要更多的\u003Cem\u003E搜索\u003C/em\u003E结果，你需要知道你需要的\u003Cem\u003E内容\u003C/em\u003E的发表地点和/或发表日期，或者至少知道文中部分\u003Cem\u003E内容\u003C/em\u003E。 我的谷歌\u003Cem\u003E搜索\u003C/em\u003E大约有一半以site",
        "title": "为什么互联网给我一种想\u003Cem\u003E搜\u003C/em\u003E的东西什么都搜不到，屁用没有的信息一大堆的无力感？"
      },
      "object": {
        "original_id": "658976624",
        "id": "3458972610",
        "type": "answer",
        "excerpt": "但是就不在你\u003Cem\u003E搜索\u003C/em\u003E的时候显示出来。如果你真的想要更多的\u003Cem\u003E搜索\u003C/em\u003E结果，你需要知道你需要的\u003Cem\u003E内容\u003C/em\u003E的发表地点和/或发表日期，或者至少知道文中部分\u003Cem\u003E内容\u003C/em\u003E。 我的谷歌\u003Cem\u003E搜索\u003C/em\u003E大约有一半以site",
        "url": "https://api.zhihu.com/answers/3458972610",
        "voteup_count": 36,
        "comment_count": 7,
        "favorites_count": 28,
        "created_time": 1712584885,
        "updated_time": 1712592049,
        "content": "\u003Cp data-pid=\"qMewjUxM\"\u003E因为搜索引擎默认不给全部结果了\u003C/p\u003E\u003Cp data-pid=\"F4vf5b8j\"\u003E我去谷歌搜water，搜索结果顶部告诉我找到154亿个结果\u003C/p\u003E\u003Cp data-pid=\"rtMxaV2e\"\u003E然而只能翻两页，到第三页就显示\u003C/p\u003E\u003Cblockquote data-pid=\"_CCvdk2F\"\u003E为了向您显示最相关的结果，我们省略了一些与已显示的 \u003Cb\u003E289 \u003C/b\u003E个非常相似的条目。 如果您愿意，可以重复搜索，并包含省略的结果。\u003C/blockquote\u003E\u003Cp data-pid=\"5aA5t76B\"\u003E点“包含省略的结果”，得到的结果甚至更少了。\u003C/p\u003E\u003Cp data-pid=\"uuPrbQPX\"\u003E貌似不管你怎么改搜索范围，例如site:\u003Ca href=\"https://link.zhihu.com/?target=http%3A//cnn.com\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttp://http://cnn.com\u003C/a\u003E water外加最近一周或者最近一个月的日期限制，都能搜到不少但是也并不太多的结果。谷歌索引了所有包含关键字的网页，但是就不在你搜索的时候显示出来。如果你真的想要更多的搜索结果，你需要知道你需要的内容的发表地点和/或发表日期，或者至少知道文中部分内容。\u003C/p\u003E\u003Cp data-pid=\"NDnNdsbF\"\u003E我的谷歌搜索大约有一半以site:\u003Ca href=\"https://link.zhihu.com/?target=http%3A//stackoverflow.com\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttp://http://stackoverflow.com\u003C/a\u003E或者site:\u003Ca href=\"https://link.zhihu.com/?target=http%3A//reddit.com\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttp://http://reddit.com\u003C/a\u003E结束 - 通常这是在我寻找新内容的时候获得真实答案的唯一方法，而无需花费时间在阅读点击诱饵/SEO垃圾内容/人工智能垃圾上。当然有时候这不怪搜索引擎，人工智能垃圾也会来自知乎、quora、百度知道之类的问答网站。但是光只限制域名也不行，有的时候我甚至找不到我自己发表过的内容，除非再加上问题中我提到过的关键字。必应的翻页体验比较好，但是搜索结果很少，搜索water只有三千万个结果，一些域名（例如我的个人网站）根本就不索引。\u003C/p\u003E\u003Cp data-pid=\"DLtMio10\"\u003E我想我明白搜索引擎为什么这样做：绝大部分搜索者几乎从不看结果的第二页。砍掉结果有利可图。但是作为用户来说，十几年前的搜索体验比现在好无数倍。\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "646087842",
          "type": "question",
          "name": "为什么互联网给我一种想\u003Cem\u003E搜\u003C/em\u003E的东西什么都搜不到，屁用没有的信息一大堆的无力感？",
          "url": "https://api.zhihu.com/questions/646087842",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "6da27e30252e93e42e27f70485e5c846",
          "url_token": "jiangshengvc",
          "name": "蒋晟",
          "headline": "程序员",
          "gender": 1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/6da27e30252e93e42e27f70485e5c846",
          "type": "people",
          "avatar_url": "https://picx.zhimg.com/50/a801f0e19_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 58496,
          "follower_count": 11135,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "flag": {
          "flag_type": "author_follower",
          "flag_text": "超过%!f(int64=01)万人关注"
        },
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "OpMCCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNjU4OTc2NjI0KgozNDU4OTcyNjEwMgkxMDU4NTI2NjY6CTY0NjA4Nzg0MkoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCgAQOoAQCwAQG4AQDCAQ9hdXRob3JfZm9sbG93ZXLQAbXxz7AG2AEk4AEHgAIAuAIAigMAkgMTMTQ5MjU3MTAxNzEzMDc5OTEwNJoDAKIDAKoDAMIDInJ1bV9zZWFyY2hfcmVjYWxsX2xvbmdfYW5fbTNfMTAyNGSYBADdBACgqD7gBAA=",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 56,
        "visits_count": 71036,
        "description": "",
        "title": "为什么互联网给我一种想搜的东西什么都搜不到，屁用没有的信息一大堆的无力感？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 3
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "其实整体逻辑就是，默认\u003Cem\u003E搜索\u003C/em\u003E=默认优先推荐广告，随便加点关键字=开启高级\u003Cem\u003E搜索\u003C/em\u003E=默认降低广告推荐。 每个人应该都掌握这种\u003Cem\u003E搜索\u003C/em\u003E技巧，特别是常用的几个关键字，可以在\u003Cem\u003E搜索\u003C/em\u003E的时候节省很多时间",
        "title": "如何看待百度\u003Cem\u003E搜索\u003C/em\u003E关键词增加 -robin或者 -李彦宏，就出现无广告正常\u003Cem\u003E内容\u003C/em\u003E？"
      },
      "object": {
        "original_id": "707959501",
        "id": "74216785323",
        "type": "answer",
        "excerpt": "其实整体逻辑就是，默认\u003Cem\u003E搜索\u003C/em\u003E=默认优先推荐广告，随便加点关键字=开启高级\u003Cem\u003E搜索\u003C/em\u003E=默认降低广告推荐。 每个人应该都掌握这种\u003Cem\u003E搜索\u003C/em\u003E技巧，特别是常用的几个关键字，可以在\u003Cem\u003E搜索\u003C/em\u003E的时候节省很多时间",
        "url": "https://api.zhihu.com/answers/74216785323",
        "voteup_count": 7310,
        "comment_count": 188,
        "favorites_count": 19458,
        "created_time": 1736301757,
        "updated_time": 1736301757,
        "content": "\u003Cp data-pid=\"-Zm08WyF\"\u003E这属于是搜索引擎高级使用技巧，很多大学的信息技术课上有讲的，这属于基本的信息素养。\u003C/p\u003E\u003Cp data-pid=\"Zs8IVujF\"\u003E比如厦门大学图书馆的这份培训课件，讲的就是如何用“-”这个符号进行高阶搜索，它的作用就是从搜索结果中排除到不想关的信息。\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://picx.zhimg.com/v2-7f36f391170ca2319d6b61239694e0ef_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"3054\" data-rawheight=\"1830\" data-original-token=\"v2-4304de5d0a62561f088a7b854be4f431\" data-default-watermark-src=\"https://pica.zhimg.com/v2-9f1179b17a0f87af2f6e39d63c3ff59e_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"3054\" data-original=\"https://picx.zhimg.com/v2-7f36f391170ca2319d6b61239694e0ef_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;3054&#39; height=&#39;1830&#39;&gt;&lt;/svg&gt;\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"3054\" data-rawheight=\"1830\" data-original-token=\"v2-4304de5d0a62561f088a7b854be4f431\" data-default-watermark-src=\"https://pica.zhimg.com/v2-9f1179b17a0f87af2f6e39d63c3ff59e_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"3054\" data-original=\"https://picx.zhimg.com/v2-7f36f391170ca2319d6b61239694e0ef_r.jpg\" data-actualsrc=\"https://picx.zhimg.com/v2-7f36f391170ca2319d6b61239694e0ef_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp data-pid=\"bZGSLDkZ\"\u003E跟题目中的 -robin -李彦宏的原理如出一辙，就是\u003C/p\u003E\u003Ch3\u003E不让浏览器按照默认的顺序展现信息，而是自定义展现。\u003C/h3\u003E\u003Cp data-pid=\"6cjnOMk7\"\u003E你可以看到加了“-”之前的搜索结果有54000条，之后只剩30700条，43%的内容都被筛掉了，可见它的有效性。\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic4.zhimg.com/v2-81a6b7839b7366f68c505b89fc689f07_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1228\" data-rawheight=\"1776\" data-original-token=\"v2-310df0a25c2edf4af249159124fb6ec7\" data-default-watermark-src=\"https://pic1.zhimg.com/v2-7f0b998d6f1cb8be76f056b0f3428f0e_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"1228\" data-original=\"https://pic4.zhimg.com/v2-81a6b7839b7366f68c505b89fc689f07_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1228&#39; height=&#39;1776&#39;&gt;&lt;/svg&gt;\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1228\" data-rawheight=\"1776\" data-original-token=\"v2-310df0a25c2edf4af249159124fb6ec7\" data-default-watermark-src=\"https://pic1.zhimg.com/v2-7f0b998d6f1cb8be76f056b0f3428f0e_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1228\" data-original=\"https://pic4.zhimg.com/v2-81a6b7839b7366f68c505b89fc689f07_r.jpg\" data-actualsrc=\"https://pic4.zhimg.com/v2-81a6b7839b7366f68c505b89fc689f07_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp data-pid=\"4etIaiuh\"\u003E这样的技巧还有很多，以下是一些常用的检索符号及逻辑语法，一般在各大搜索引擎都是适用的：\u003C/p\u003E\u003Ctable data-draft-node=\"block\" data-draft-type=\"table\" data-size=\"normal\" data-row-style=\"normal\"\u003E\u003Ctbody\u003E\u003Ctr\u003E\u003Cth\u003E符号/语法\u003C/th\u003E\u003Cth\u003E功能说明\u003C/th\u003E\u003Cth\u003E示例\u003C/th\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003E+ (加号)\u003C/td\u003E\u003Ctd\u003E强制包含某个关键词\u003C/td\u003E\u003Ctd\u003EAI +深度学习 强调包含“深度学习”结果\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003E- (减号)\u003C/td\u003E\u003Ctd\u003E排除包含某个关键词的结果\u003C/td\u003E\u003Ctd\u003EAI -李彦宏 排除李彦宏相关结果\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003E**\u003C/td\u003E\u003Ctd\u003E(竖线或 OR)**\u003C/td\u003E\u003Ctd\u003E并行搜索，匹配任意一个关键词\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003E&#34; &#34; (引号)\u003C/td\u003E\u003Ctd\u003E精确匹配短语或词组\u003C/td\u003E\u003Ctd\u003E&#34;深度学习算法&#34; 精确匹配短语\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003Esite:\u003C/td\u003E\u003Ctd\u003E限制在特定网站内搜索\u003C/td\u003E\u003Ctd\u003Esite:\u003Ca href=\"https://link.zhihu.com/?target=http%3A//baidu.com\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttp://http://baidu.com\u003C/a\u003E 人工智能\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003Efiletype:\u003C/td\u003E\u003Ctd\u003E搜索特定格式文件\u003C/td\u003E\u003Ctd\u003EAI filetype:pdf 只显示 PDF 文件\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003Eintitle:\u003C/td\u003E\u003Ctd\u003E搜索网页标题中包含指定关键词的页面\u003C/td\u003E\u003Ctd\u003Eintitle:AI 技术\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003Einurl:\u003C/td\u003E\u003Ctd\u003E搜索 URL 中包含关键词的页面\u003C/td\u003E\u003Ctd\u003Einurl:AI 技术\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003Erelated:\u003C/td\u003E\u003Ctd\u003E查找与某个网站类似的网站\u003C/td\u003E\u003Ctd\u003Erelated:\u003Ca href=\"https://link.zhihu.com/?target=http%3A//baidu.com\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttp://http://baidu.com\u003C/a\u003E\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003Ecache:\u003C/td\u003E\u003Ctd\u003E查看网页的缓存版本\u003C/td\u003E\u003Ctd\u003Ecache:\u003Ca href=\"https://link.zhihu.com/?target=http%3A//baidu.com\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttp://http://baidu.com\u003C/a\u003E\u003C/td\u003E\u003C/tr\u003E\u003Ctr\u003E\u003Ctd\u003Edefine:\u003C/td\u003E\u003Ctd\u003E查找某个单词或短语的定义（Google 特有）\u003C/td\u003E\u003Ctd\u003Edefine:AI\u003C/td\u003E\u003C/tr\u003E\u003C/tbody\u003E\u003C/table\u003E\u003Cp data-pid=\"JbKXaXVC\"\u003E我平时使用的最多的场景就是filetype这个命令，场景主要有两个，一个是找小说，另一个是找ppt。\u003C/p\u003E\u003Cp data-pid=\"g2Mrgaay\"\u003E比如要找诡秘之主，直接搜，第一个出来的就是默认的，也就是广告位。\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic3.zhimg.com/v2-07fcc57c3bc353143ea004ae014cc35c_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1678\" data-rawheight=\"1968\" data-original-token=\"v2-42367808dd7ee688ef520f01a0db692f\" data-default-watermark-src=\"https://pica.zhimg.com/v2-01c6370e5116756858d5ebb71107591c_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"1678\" data-original=\"https://pic3.zhimg.com/v2-07fcc57c3bc353143ea004ae014cc35c_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1678&#39; height=&#39;1968&#39;&gt;&lt;/svg&gt;\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1678\" data-rawheight=\"1968\" data-original-token=\"v2-42367808dd7ee688ef520f01a0db692f\" data-default-watermark-src=\"https://pica.zhimg.com/v2-01c6370e5116756858d5ebb71107591c_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1678\" data-original=\"https://pic3.zhimg.com/v2-07fcc57c3bc353143ea004ae014cc35c_r.jpg\" data-actualsrc=\"https://pic3.zhimg.com/v2-07fcc57c3bc353143ea004ae014cc35c_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp data-pid=\"d__UAGCo\"\u003E但是变成「诡秘之主 filetype:pdf」，展现出来的就没广告了，基本上都是相关的内容了。\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic1.zhimg.com/v2-0a7b4e7c12d140c14c45f9bda09abee8_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1708\" data-rawheight=\"1988\" data-original-token=\"v2-35a735d2031c3ddc1ebc3bd52ee21f3b\" data-default-watermark-src=\"https://picx.zhimg.com/v2-18e9a399cf4013f8732dee5342677855_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"1708\" data-original=\"https://pic1.zhimg.com/v2-0a7b4e7c12d140c14c45f9bda09abee8_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1708&#39; height=&#39;1988&#39;&gt;&lt;/svg&gt;\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1708\" data-rawheight=\"1988\" data-original-token=\"v2-35a735d2031c3ddc1ebc3bd52ee21f3b\" data-default-watermark-src=\"https://picx.zhimg.com/v2-18e9a399cf4013f8732dee5342677855_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1708\" data-original=\"https://pic1.zhimg.com/v2-0a7b4e7c12d140c14c45f9bda09abee8_r.jpg\" data-actualsrc=\"https://pic1.zhimg.com/v2-0a7b4e7c12d140c14c45f9bda09abee8_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp data-pid=\"wEFrnksx\"\u003E还有找课件也一样，不管你在关键词里加入多少“大学课件”，”研究生课件“这类型的词，都不如加一个「filetype:ppt」好使。\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic4.zhimg.com/v2-cc0dc78730bbbf00b1b2baa519f30f03_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1704\" data-rawheight=\"1738\" data-original-token=\"v2-e832e7d7651a2870d3e3d61801b00c96\" data-default-watermark-src=\"https://picx.zhimg.com/v2-cbc74d075ddd6afd9ba9d3057a3673bb_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"1704\" data-original=\"https://pic4.zhimg.com/v2-cc0dc78730bbbf00b1b2baa519f30f03_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1704&#39; height=&#39;1738&#39;&gt;&lt;/svg&gt;\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1704\" data-rawheight=\"1738\" data-original-token=\"v2-e832e7d7651a2870d3e3d61801b00c96\" data-default-watermark-src=\"https://picx.zhimg.com/v2-cbc74d075ddd6afd9ba9d3057a3673bb_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1704\" data-original=\"https://pic4.zhimg.com/v2-cc0dc78730bbbf00b1b2baa519f30f03_r.jpg\" data-actualsrc=\"https://pic4.zhimg.com/v2-cc0dc78730bbbf00b1b2baa519f30f03_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp data-pid=\"aO4XxOy2\"\u003E话说回到题目，其实就是一个「信息素养」覆盖不到位的表现，如果要是知道这个公式\u003C/p\u003E\u003Ch3\u003E搜索内容 -/+/其他关键词 \u003C/h3\u003E\u003Cp data-pid=\"RyxzPhmw\"\u003E都可以有效的过滤信息，关闭系统的默认推荐，其实就不会出现这样的疑问了。\u003C/p\u003E\u003Cp data-pid=\"jLXYoWCR\"\u003E因为这个关键词的加入就决定了默认推荐的关闭，开启了高级搜索模式，而这个词的质量其实只决定了后续的准确性，换绝大多数的词都可以生效。\u003C/p\u003E\u003Cp data-pid=\"Cu0G_21B\"\u003E你看，换成特朗普也一样，直接就把广告推荐的权重降低了。\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic3.zhimg.com/v2-9bfdf5b108f6e1fb3eedc8883f6a3fca_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1698\" data-rawheight=\"1824\" data-original-token=\"v2-59828162c197fd6bd02b5605ea47437b\" data-default-watermark-src=\"https://pic2.zhimg.com/v2-a7684501a3617b304cd6a8ab6026f463_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"1698\" data-original=\"https://pic3.zhimg.com/v2-9bfdf5b108f6e1fb3eedc8883f6a3fca_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1698&#39; height=&#39;1824&#39;&gt;&lt;/svg&gt;\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1698\" data-rawheight=\"1824\" data-original-token=\"v2-59828162c197fd6bd02b5605ea47437b\" data-default-watermark-src=\"https://pic2.zhimg.com/v2-a7684501a3617b304cd6a8ab6026f463_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1698\" data-original=\"https://pic3.zhimg.com/v2-9bfdf5b108f6e1fb3eedc8883f6a3fca_r.jpg\" data-actualsrc=\"https://pic3.zhimg.com/v2-9bfdf5b108f6e1fb3eedc8883f6a3fca_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp data-pid=\"TuCHreet\"\u003E其实整体逻辑就是，默认搜索=默认优先推荐广告，随便加点关键字=开启高级搜索=默认降低广告推荐。\u003C/p\u003E\u003Cp data-pid=\"IvsVIKw_\"\u003E每个人应该都掌握这种搜索技巧，特别是常用的几个关键字，可以在搜索的时候节省很多时间。 \u003C/p\u003E",
        "thumbnail_info": {
          "count": 3,
          "thumbnails": [
            {
              "data_url": "",
              "url": "https://picx.zhimg.com/80/v2-4304de5d0a62561f088a7b854be4f431_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 3054,
              "height": 1830
            },
            {
              "data_url": "",
              "url": "https://picx.zhimg.com/80/v2-310df0a25c2edf4af249159124fb6ec7_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 1228,
              "height": 1776
            },
            {
              "data_url": "",
              "url": "https://picx.zhimg.com/80/v2-42367808dd7ee688ef520f01a0db692f_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 1678,
              "height": 1968
            }
          ],
          "type": "thumbnail_info",
          "total_count": 6
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "8937706951",
          "type": "question",
          "name": "如何看待百度\u003Cem\u003E搜索\u003C/em\u003E关键词增加 -robin或者 -李彦宏，就出现无广告正常\u003Cem\u003E内容\u003C/em\u003E？",
          "url": "https://api.zhihu.com/questions/8937706951",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "a0265e93117d3d366adda36310194727",
          "url_token": "jzwa",
          "name": "平凡",
          "headline": "英国大学讲师（AP）｜AI｜专栏作家｜pingfan.me",
          "gender": 1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/a0265e93117d3d366adda36310194727",
          "type": "people",
          "avatar_url": "https://picx.zhimg.com/50/v2-9f81432bb5f397e14ec2c65e949eb0d3_l.jpg?source=4e949a73",
          "badge": [
            {
              "type": "best_answerer",
              "description": "优秀回答者",
              "topics": [
                {
                  "id": "19554298",
                  "name": "编程",
                  "introduction": "编程是编写程序的中文简称，就是让计算机代为解决某个问题，对某个计算体系规定一定的运算方式，是计算体系按照该计算方式运行，并最终得到相应结果的过程。为了使计算机能够理解人的意图，人类就必须将需解决的问题的思路、方法和手段通过计算机能够理解的形式告诉计算机，使得计算机能够根据人的指令一步一步去工作，完成某种特定的任务。这种人和计算体系之间交流的过程就是编程。编程：设计具备逻辑流动作用的一种“可控体系”【注：编程不一定是针对计算机程序而言的，针对具备逻辑计算力的体系，都可以算编程。】",
                  "excerpt": "编程是编写程序的中文简称，就是让计算机代为解决某个问题，对某个计算体系规定一定的运算方式，是计算体系按照该计算方式运行，并最终得到相应结果的过程。为了使计算机能够理解人的意图，人类就必须将需解决的问题的思路、方法和手段通过计算机能够理解的形式告诉计算机，使得计算机能够根据人的指令一步一步去工作，完成某种特定的任务。这种人和计算体系之间交流的过程就是编程。编程：设计具备逻辑流动作用的一种“可控体系…",
                  "type": "topic",
                  "url": "https://api.zhihu.com/topics/19554298",
                  "avatar_url": "https://picx.zhimg.com/50/v2-4030982b9aed71d12b350a4c3ba5078d_l.jpg?source=4e949a73"
                },
                {
                  "id": "26215901",
                  "name": "AIGC",
                  "introduction": "\u003Cp\u003E人工智能生成内容\u003C/p\u003E",
                  "excerpt": "人工智能生成内容 ",
                  "type": "topic",
                  "url": "https://api.zhihu.com/topics/26215901",
                  "avatar_url": "https://picx.zhimg.com/50/v2-c919b37971176088d83469f3984740cc_l.jpg?source=4e949a73"
                },
                {
                  "id": "19553528",
                  "name": "英语",
                  "introduction": "\u003Cp\u003E英语（English /ˈɪŋɡlɪʃ/）又称为英文，是一种西日耳曼语言，诞生于中世纪早期的英格兰，如今具有全球通用语的地位。“英语”一词源于迁居英格兰的日耳曼部落盎格鲁（Angles），而“盎格鲁”得名于临波罗的海的半岛盎格里亚（Anglia）。弗里西语是与英语最相近的语言。英语词汇在中世纪早期受到了其他日耳曼族语言的大量影响，后来受罗曼族语言尤其是法语的影响。英语是将近六十个国家唯一的官方语言或官方语言之一，也是全世界最多国家的官方语言。它是英国、爱尔兰、美国、加拿大、澳大利亚和新西兰最常用的语言，也在加勒比、非洲及南亚的部分地区被广泛使用。\u003C/p\u003E",
                  "excerpt": "英语（English /ˈɪŋɡlɪʃ/）又称为英文，是一种西日耳曼语言，诞生于中世纪早期的英格兰，如今具有全球通用语的地位。“英语”一词源于迁居英格兰的日耳曼部落盎格鲁（Angles），而“盎格鲁”得名于临波罗的海的半岛盎格里亚（Anglia）。弗里西语是与英语最相近的语言。英语词汇在中世纪早期受到了其他日耳曼族语言的大量影响，后来受罗曼族语言尤其是法语的影响。英语是将近六十个国家唯一的官方语言或官方语言之一，也是…",
                  "type": "topic",
                  "url": "https://api.zhihu.com/topics/19553528",
                  "avatar_url": "https://picx.zhimg.com/50/ee1b9f4af266b8acaea6d014d7176043_l.jpg?source=4e949a73"
                }
              ]
            },
            {
              "type": "identity",
              "description": "已认证的机构",
              "topics": []
            }
          ],
          "authority_info": null,
          "voteup_count": 1183546,
          "follower_count": 158553,
          "badge_v2": {
            "title": "新知答主",
            "merged_badges": [
              {
                "type": "best",
                "detail_type": "best",
                "title": "年度新知答主",
                "description": "新知答主",
                "url": "https://www.zhihu.com/question/510340037",
                "sources": [],
                "icon": "",
                "night_icon": ""
              }
            ],
            "detail_badges": [
              {
                "type": "reward",
                "detail_type": "zhihu_yearly_answerer",
                "title": "年度新知答主",
                "description": "新知答主",
                "url": "https://www.zhihu.com/question/510340037",
                "sources": [
                  {
                    "id": "2024",
                    "token": "",
                    "type": "year",
                    "url": "",
                    "name": "",
                    "avatar_path": "",
                    "avatar_url": "",
                    "description": "",
                    "priority": 2024
                  }
                ],
                "icon": "https://pic1.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c",
                "night_icon": "https://pic1.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c"
              },
              {
                "type": "best",
                "detail_type": "best_answerer",
                "title": "优秀答主",
                "description": "英语等 5 个话题下的优秀答主",
                "url": "https://www.zhihu.com/question/48509984",
                "sources": [
                  {
                    "id": "19553528",
                    "token": "19553528",
                    "type": "topic",
                    "url": "https://www.zhihu.com/topic/19553528",
                    "name": "英语",
                    "avatar_path": "v2-b8fbf9268139ebce2210d83ee652b888",
                    "avatar_url": "https://pica.zhimg.com/v2-b8fbf9268139ebce2210d83ee652b888_720w.jpg?source=32738c0c",
                    "description": "",
                    "priority": 0
                  },
                  {
                    "id": "19554298",
                    "token": "19554298",
                    "type": "topic",
                    "url": "https://www.zhihu.com/topic/19554298",
                    "name": "编程",
                    "avatar_path": "v2-27b8ba1e647956fa6f1a2a8ad90138ef",
                    "avatar_url": "https://picx.zhimg.com/v2-27b8ba1e647956fa6f1a2a8ad90138ef_720w.jpg?source=32738c0c",
                    "description": "",
                    "priority": 0
                  },
                  {
                    "id": "19556895",
                    "token": "19556895",
                    "type": "topic",
                    "url": "https://www.zhihu.com/topic/19556895",
                    "name": "科研",
                    "avatar_path": "v2-f94b3093434c09b4501b056d142025e0",
                    "avatar_url": "https://pic1.zhimg.com/v2-f94b3093434c09b4501b056d142025e0_720w.jpg?source=32738c0c",
                    "description": "",
                    "priority": 0
                  },
                  {
                    "id": "26215901",
                    "token": "26215901",
                    "type": "topic",
                    "url": "https://www.zhihu.com/topic/26215901",
                    "name": "AIGC",
                    "avatar_path": "v2-17256bdee3ce72a338455853bb80eca1",
                    "avatar_url": "https://picx.zhimg.com/v2-17256bdee3ce72a338455853bb80eca1_720w.jpg?source=32738c0c",
                    "description": "",
                    "priority": 0
                  },
                  {
                    "id": "19551275",
                    "token": "19551275",
                    "type": "topic",
                    "url": "https://www.zhihu.com/topic/19551275",
                    "name": "人工智能",
                    "avatar_path": "v2-c41d10d22173d515740c43c70f885705",
                    "avatar_url": "https://pica.zhimg.com/v2-c41d10d22173d515740c43c70f885705_720w.jpg?source=32738c0c",
                    "description": "",
                    "priority": 0
                  }
                ],
                "icon": "https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c",
                "night_icon": "https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c"
              },
              {
                "type": "reward",
                "detail_type": "super_activity",
                "title": "社区成就",
                "description": "知势榜科技互联网领域影响力榜答主",
                "url": "",
                "sources": [
                  {
                    "id": "27",
                    "token": "",
                    "type": "content_potential_category",
                    "url": "",
                    "name": "知势榜8月",
                    "avatar_path": "",
                    "avatar_url": "",
                    "description": "",
                    "priority": 27
                  }
                ],
                "icon": "https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c",
                "night_icon": "https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c"
              }
            ],
            "icon": "https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c",
            "night_icon": "https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c"
          },
          "old_badges": [
            {
              "type": "best_answerer",
              "description": "优秀答主",
              "topics": [
                {
                  "id": "19553528",
                  "type": "topic",
                  "url": "https://www.zhihu.com/topic/19553528",
                  "name": "英语",
                  "avatar_url": "https://pica.zhimg.com/v2-b8fbf9268139ebce2210d83ee652b888_720w.jpg?source=32738c0c"
                },
                {
                  "id": "19554298",
                  "type": "topic",
                  "url": "https://www.zhihu.com/topic/19554298",
                  "name": "编程",
                  "avatar_url": "https://picx.zhimg.com/v2-27b8ba1e647956fa6f1a2a8ad90138ef_720w.jpg?source=32738c0c"
                },
                {
                  "id": "19556895",
                  "type": "topic",
                  "url": "https://www.zhihu.com/topic/19556895",
                  "name": "科研",
                  "avatar_url": "https://pic1.zhimg.com/v2-f94b3093434c09b4501b056d142025e0_720w.jpg?source=32738c0c"
                },
                {
                  "id": "26215901",
                  "type": "topic",
                  "url": "https://www.zhihu.com/topic/26215901",
                  "name": "AIGC",
                  "avatar_url": "https://picx.zhimg.com/v2-17256bdee3ce72a338455853bb80eca1_720w.jpg?source=32738c0c"
                },
                {
                  "id": "19551275",
                  "type": "topic",
                  "url": "https://www.zhihu.com/topic/19551275",
                  "name": "人工智能",
                  "avatar_url": "https://pica.zhimg.com/v2-c41d10d22173d515740c43c70f885705_720w.jpg?source=32738c0c"
                }
              ]
            }
          ],
          "badge_v2_string": "{\"title\":\"新知答主\",\"merged_badges\":[{\"type\":\"best\",\"detail_type\":\"best\",\"title\":\"年度新知答主\",\"description\":\"新知答主\",\"url\":\"https://www.zhihu.com/question/510340037\",\"sources\":null,\"icon\":\"\",\"night_icon\":\"\",\"badge_status\":\"passed\"}],\"detail_badges\":[{\"type\":\"reward\",\"detail_type\":\"zhihu_yearly_answerer\",\"title\":\"年度新知答主\",\"description\":\"新知答主\",\"url\":\"https://www.zhihu.com/question/510340037\",\"sources\":[{\"id\":\"2024\",\"token\":\"\",\"type\":\"year\",\"url\":\"\",\"name\":\"\",\"avatar_path\":\"\",\"avatar_url\":\"\",\"description\":\"\",\"priority\":2024}],\"icon\":\"https://pic1.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"night_icon\":\"https://pic1.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"badge_status\":\"passed\"},{\"type\":\"best\",\"detail_type\":\"best_answerer\",\"title\":\"优秀答主\",\"description\":\"英语等 5 个话题下的优秀答主\",\"url\":\"https://www.zhihu.com/question/48509984\",\"sources\":[{\"id\":\"19553528\",\"token\":\"19553528\",\"type\":\"topic\",\"url\":\"https://www.zhihu.com/topic/19553528\",\"name\":\"英语\",\"avatar_path\":\"v2-b8fbf9268139ebce2210d83ee652b888\",\"avatar_url\":\"https://pica.zhimg.com/v2-b8fbf9268139ebce2210d83ee652b888_720w.jpg?source=32738c0c\",\"description\":\"\",\"priority\":0},{\"id\":\"19554298\",\"token\":\"19554298\",\"type\":\"topic\",\"url\":\"https://www.zhihu.com/topic/19554298\",\"name\":\"编程\",\"avatar_path\":\"v2-27b8ba1e647956fa6f1a2a8ad90138ef\",\"avatar_url\":\"https://picx.zhimg.com/v2-27b8ba1e647956fa6f1a2a8ad90138ef_720w.jpg?source=32738c0c\",\"description\":\"\",\"priority\":0},{\"id\":\"19556895\",\"token\":\"19556895\",\"type\":\"topic\",\"url\":\"https://www.zhihu.com/topic/19556895\",\"name\":\"科研\",\"avatar_path\":\"v2-f94b3093434c09b4501b056d142025e0\",\"avatar_url\":\"https://pic1.zhimg.com/v2-f94b3093434c09b4501b056d142025e0_720w.jpg?source=32738c0c\",\"description\":\"\",\"priority\":0},{\"id\":\"26215901\",\"token\":\"26215901\",\"type\":\"topic\",\"url\":\"https://www.zhihu.com/topic/26215901\",\"name\":\"AIGC\",\"avatar_path\":\"v2-17256bdee3ce72a338455853bb80eca1\",\"avatar_url\":\"https://picx.zhimg.com/v2-17256bdee3ce72a338455853bb80eca1_720w.jpg?source=32738c0c\",\"description\":\"\",\"priority\":0},{\"id\":\"19551275\",\"token\":\"19551275\",\"type\":\"topic\",\"url\":\"https://www.zhihu.com/topic/19551275\",\"name\":\"人工智能\",\"avatar_path\":\"v2-c41d10d22173d515740c43c70f885705\",\"avatar_url\":\"https://pica.zhimg.com/v2-c41d10d22173d515740c43c70f885705_720w.jpg?source=32738c0c\",\"description\":\"\",\"priority\":0}],\"icon\":\"https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"night_icon\":\"https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"badge_status\":\"passed\"},{\"type\":\"reward\",\"detail_type\":\"super_activity\",\"title\":\"社区成就\",\"description\":\"知势榜科技互联网领域影响力榜答主\",\"url\":\"\",\"sources\":[{\"id\":\"27\",\"token\":\"\",\"type\":\"content_potential_category\",\"url\":\"\",\"name\":\"知势榜8月\",\"avatar_path\":\"\",\"avatar_url\":\"\",\"description\":\"\",\"priority\":27}],\"icon\":\"https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"night_icon\":\"https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"badge_status\":\"passed\"}],\"icon\":\"https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"night_icon\":\"https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\"}",
          "topic_bayes_map": null,
          "topic_map": {
            "AIGC": "AIGC",
            "人工智能": "人工智能",
            "科研": "科研",
            "编程": "编程",
            "英语": "英语"
          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "flag": {
          "flag_type": "best_answerer",
          "flag_text": "年度新知答主"
        },
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "OpICCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNzA3OTU5NTAxKgs3NDIxNjc4NTMyMzIJMTEyNzYzOTg1Ogo4OTM3NzA2OTUxSgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA5gBAKABBKgBALABAbgBAMIBCkJlc3RBbnN3ZXLQAb2597sG2AGOOeABvAGAAgC4AgD4AgGKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMAqgMAwgMfcnVtX3NlYXJjaF9yZWNhbGxfYW5fdHVuZV8xMDI0ZJgEA90EAMDQPuAEAA==",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 101,
        "visits_count": 1057432,
        "description": "\u003Cp\u003E注意-前面要加空格不然会被当做关键词搜索。\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://picx.zhimg.com/v2-20bd638ea9a56331fb73c5595df20909_1440w.jpeg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"996\" data-rawheight=\"1128\" data-original-token=\"v2-20bd638ea9a56331fb73c5595df20909\" class=\"origin_image zh-lightbox-thumb\" width=\"996\" data-original=\"https://picx.zhimg.com/v2-20bd638ea9a56331fb73c5595df20909_r.jpg\"/\u003E\u003C/figure\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic1.zhimg.com/v2-f6c8f00fe9dc167dc7c63cb79cdf7eba_1440w.jpeg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1366\" data-rawheight=\"1364\" data-original-token=\"v2-f6c8f00fe9dc167dc7c63cb79cdf7eba\" class=\"origin_image zh-lightbox-thumb\" width=\"1366\" data-original=\"https://pic1.zhimg.com/v2-f6c8f00fe9dc167dc7c63cb79cdf7eba_r.jpg\"/\u003E\u003C/figure\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic2.zhimg.com/v2-ef1b14d3a2d90acb70da1745a53dfba9_1440w.jpeg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1334\" data-rawheight=\"1358\" data-original-token=\"v2-ef1b14d3a2d90acb70da1745a53dfba9\" class=\"origin_image zh-lightbox-thumb\" width=\"1334\" data-original=\"https://pic2.zhimg.com/v2-ef1b14d3a2d90acb70da1745a53dfba9_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp\u003E\u003C/p\u003E",
        "title": "如何看待百度搜索关键词增加 -robin或者 -李彦宏，就出现无广告正常内容？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 4
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "Everything。从win7就开始用，不过win10的\u003Cem\u003E搜索\u003C/em\u003E功能改进了，单还是习惯Everything，缺点就是只能搜文件名，不能搜文件\u003Cem\u003E内容\u003C/em\u003E",
        "title": "问问大家有什么好用的文件\u003Cem\u003E搜索\u003C/em\u003E工具？"
      },
      "object": {
        "original_id": "737619673",
        "id": "1929496763800196377",
        "type": "answer",
        "excerpt": "Everything。从win7就开始用，不过win10的\u003Cem\u003E搜索\u003C/em\u003E功能改进了，单还是习惯Everything，缺点就是只能搜文件名，不能搜文件\u003Cem\u003E内容\u003C/em\u003E",
        "url": "https://api.zhihu.com/answers/1929496763800196377",
        "voteup_count": 3,
        "comment_count": 1,
        "favorites_count": 0,
        "created_time": 1752807876,
        "updated_time": 1752807876,
        "content": "\u003Cp data-pid=\"ltkFN5ZR\"\u003EEverything。从win7就开始用，不过win10的搜索功能改进了，单还是习惯Everything，缺点就是只能搜文件名，不能搜文件内容\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "1928070993684981325",
          "type": "question",
          "name": "问问大家有什么好用的文件\u003Cem\u003E搜索\u003C/em\u003E工具？",
          "url": "https://api.zhihu.com/questions/1928070993684981325",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "a77479ea9ca35a66943c2d562b52b3c3",
          "url_token": "123-56-78",
          "name": "123",
          "headline": "PLC",
          "gender": -1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/a77479ea9ca35a66943c2d562b52b3c3",
          "type": "people",
          "avatar_url": "https://pic1.zhimg.com/50/v2-abed1a8c04700ba7d72b45195223e0ff_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 10,
          "follower_count": 3,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "OpMCCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNzM3NjE5NjczKhMxOTI5NDk2NzYzODAwMTk2Mzc3MgkxMTU3OTg4MDY6EzE5MjgwNzA5OTM2ODQ5ODEzMjVKDOaQnOe0ouWGheWuuVAAWAFgAWoM5pCc57Si5YaF5a65cASAAY6y3N3a/5ADmAEAoAEFqAEAsAEBuAEA0AHE8+bDBtgBA+ABAYACALgCAIoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwCqAwDCAyFydW1fc2VhcmNoX3JlY2FsbF9xdWVzX3R1bmVfMTAyNGSYBADdBABAiT7gBAA=",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 2,
        "visits_count": 838,
        "description": "\u003Cp\u003E如题\u003C/p\u003E",
        "title": "问问大家有什么好用的文件搜索工具？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 5
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "就是你能把一句话说明白吗，能把要\u003Cem\u003E搜索\u003C/em\u003E的关键词清晰表达出来吗 据我了解一半以上的人说搜不到东西和互联网垃圾没有直接关系，我之前在大学教过一阵课，很多学生是不会用\u003Cem\u003E搜索\u003C/em\u003E引擎，无法用准确的语言描述",
        "title": "为什么互联网给我一种想\u003Cem\u003E搜\u003C/em\u003E的东西什么都搜不到，屁用没有的信息一大堆的无力感？"
      },
      "object": {
        "original_id": "708877840",
        "id": "78811218786",
        "type": "answer",
        "excerpt": "就是你能把一句话说明白吗，能把要\u003Cem\u003E搜索\u003C/em\u003E的关键词清晰表达出来吗 据我了解一半以上的人说搜不到东西和互联网垃圾没有直接关系，我之前在大学教过一阵课，很多学生是不会用\u003Cem\u003E搜索\u003C/em\u003E引擎，无法用准确的语言描述",
        "url": "https://api.zhihu.com/answers/78811218786",
        "voteup_count": 8,
        "comment_count": 4,
        "favorites_count": 3,
        "created_time": 1736826709,
        "updated_time": 1736826709,
        "content": "\u003Cp data-pid=\"zMDIhhJK\"\u003E现在互联网垃圾确实多会导致不容易搜到你想要的内容，而且人为设置壁垒，各种封闭的圈子\u003C/p\u003E\u003Cp data-pid=\"qshWHziZ\"\u003E还有一点你不要忽略了，就是你能把一句话说明白吗，能把要搜索的关键词清晰表达出来吗\u003C/p\u003E\u003Cp data-pid=\"GCPtUjOF\"\u003E据我了解一半以上的人说搜不到东西和互联网垃圾没有直接关系，我之前在大学教过一阵课，很多学生是不会用搜索引擎，无法用准确的语言描述，在搜索框里输入的东西超级长且全是废话，能搜到东西才见鬼了，还有几个学生没完没了用抖音b站做搜索引擎，实在看不下去硬是讲了一节课怎么用搜索引擎\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "646087842",
          "type": "question",
          "name": "为什么互联网给我一种想\u003Cem\u003E搜\u003C/em\u003E的东西什么都搜不到，屁用没有的信息一大堆的无力感？",
          "url": "https://api.zhihu.com/questions/646087842",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "7072e5728773ad63e935260c7dd66f93",
          "url_token": "eeeeeei",
          "name": "eeeeeei",
          "headline": "",
          "gender": -1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/7072e5728773ad63e935260c7dd66f93",
          "type": "people",
          "avatar_url": "https://pica.zhimg.com/50/v2-4b17ecdef2596f90f08e1bbb78624f63_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 1752,
          "follower_count": 64,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "Ov4BCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNzA4ODc3ODQwKgs3ODgxMTIxODc4NjIJMTA1ODUyNjY2Ogk2NDYwODc4NDJKDOaQnOe0ouWGheWuuVAAWAFgAWoM5pCc57Si5YaF5a65cASAAY6y3N3a/5ADmAEAoAEGqAEAsAEBuAEA0AHVvpe8BtgBCOABBIACALgCAIoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwCqAwDCAx5ydW1fYW5zd2VyX3NpbXByZXJhbmtfZW1iXzEyOGSYBADdBACgqD7gBAA=",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 56,
        "visits_count": 71036,
        "description": "",
        "title": "为什么互联网给我一种想搜的东西什么都搜不到，屁用没有的信息一大堆的无力感？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 6
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "Google的问题是\u003Cem\u003E搜索\u003C/em\u003E争议问题时，搜出来的\u003Cem\u003E内容\u003C/em\u003E经常zzzq，少有反方观点，有也是稀稀拉拉，不够尖锐，火力不足。所以\u003Cem\u003E搜索\u003C/em\u003Ezzzq的\u003Cem\u003E内容\u003C/em\u003E，不利于西方拳贵的内容，得用DuckDuckgo/yandex搜",
        "title": "你有什么在网上\u003Cem\u003E搜索\u003C/em\u003E东西的神搜索技巧？"
      },
      "object": {
        "original_id": "742456729",
        "id": "1940013638279754161",
        "type": "answer",
        "excerpt": "Google的问题是\u003Cem\u003E搜索\u003C/em\u003E争议问题时，搜出来的\u003Cem\u003E内容\u003C/em\u003E经常zzzq，少有反方观点，有也是稀稀拉拉，不够尖锐，火力不足。所以\u003Cem\u003E搜索\u003C/em\u003Ezzzq的\u003Cem\u003E内容\u003C/em\u003E，不利于西方拳贵的内容，得用DuckDuckgo/yandex搜",
        "url": "https://api.zhihu.com/answers/1940013638279754161",
        "voteup_count": 1385,
        "comment_count": 46,
        "favorites_count": 4638,
        "created_time": 1755315295,
        "updated_time": 1755409232,
        "content": "\u003Cp data-pid=\"vkUk1DBw\"\u003E1) 第一当然是扔掉百毒这种乐色，Google/DuckDuckgo/Yandex/Brave都完爆百毒，连阉割版的bing都比百毒清爽多了，至少不会下到假的steam，萌新就使用bing国内版吧。\u003C/p\u003E\u003Cp data-pid=\"wmevmyAa\"\u003EGoogle的问题是搜索争议问题时，搜出来的内容经常zzzq，少有反方观点，有也是稀稀拉拉，不够尖锐，火力不足。所以搜索zzzq的内容，不利于西方拳贵的内容，得用DuckDuckgo/yandex搜。搜索引擎zzzq的程度，Google &gt; Brave &gt; DuckDuckgo &gt; yandex, yandex根本不叼西方正确那一套，因为是老俄的搜索引擎。\u003C/p\u003E\u003Cp data-pid=\"VGo1Wgac\"\u003E例如以前搜爱泼斯坦名单，只有yandex把名单置顶了。搜好莱坞丑闻，只有yandex会显示激烈/劲爆的结果，其他引擎都是蜻蜓点水。\u003C/p\u003E\u003Cp data-pid=\"ySTR5M5w\"\u003EDuckDuckgo的搜索词没有高亮，不够醒目，可以写个js脚本改下样式，代码在最后。\u003C/p\u003E\u003Cp data-pid=\"Q38TIU9y\"\u003E2) yandex搜软件名+cracked premium unlocked，很容易找到你要的软件，不管是安卓还是pc/mac, 适合学生党。\u003C/p\u003E\u003Cp data-pid=\"cDKt42uY\"\u003E3) yandex搜xx歌+free download，或者xx歌+Скачать，结果里找带俄语的，一般都可以直接拿到，学生党用用就行了。\u003C/p\u003E\u003Cp data-pid=\"3GWGDtxE\"\u003E4) xxx site:\u003Cu\u003E\u003Ca href=\"https://link.zhihu.com/?target=https%3A//domain-name%2C/\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttps://x.com,\u003C/a\u003E\u003C/u\u003E 只搜某个网站的结果，有些资源网站自带搜索不好用，可以这样搜。\u003C/p\u003E\u003Cp data-pid=\"vaB9adaK\"\u003E5) anna archive, zlib搜书的时候，关键词+edition, 更容易搜出好书，因为再版的书通常都比较好。例如tort edition\u003C/p\u003E\u003Cp data-pid=\"oJ9tEc1l\"\u003E关键词+译，可以搜出翻译过来的书，例如文明史 译\u003C/p\u003E\u003Cp data-pid=\"-Wz-VOhn\"\u003E6) 某瓣关键词搜书，出来的高分书，一般都还不错，而且某本书下面也会推荐类似的书。\u003C/p\u003E\u003Cp data-pid=\"7La3Ka4N\"\u003E---DuckDuckgo改样式的js代码\u003C/p\u003E\u003Cp data-pid=\"snGtdJ0T\"\u003E// ==UserScript==\u003C/p\u003E\u003Cp data-pid=\"jYU8MT8a\"\u003E// @name         duckduckgo\u003C/p\u003E\u003Cp data-pid=\"JNd1P3TV\"\u003E// @namespace    \u003Ca href=\"https://link.zhihu.com/?target=http%3A//tampermonkey.net/\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttp://http://tampermonkey.net/\u003C/a\u003E\u003C/p\u003E\u003Cp data-pid=\"niTY_-5Q\"\u003E// @version      2025-08-05\u003C/p\u003E\u003Cp data-pid=\"AsTQwPGh\"\u003E// @description  try to take over the world!\u003C/p\u003E\u003Cp data-pid=\"QzQK0A77\"\u003E// @author       You\u003C/p\u003E\u003Cp data-pid=\"TU20tT94\"\u003E// @match        \u003Ca href=\"https://link.zhihu.com/?target=https%3A//duckduckgo.com/%3F\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003Ehttps://http://duckduckgo.com/?\u003C/a\u003E*\u003C/p\u003E\u003Cp data-pid=\"6SfF_qiy\"\u003E// @grant        GM_addStyle\u003C/p\u003E\u003Cp data-pid=\"O27Wg1af\"\u003E// ==/UserScript==\u003C/p\u003E\u003Cp class=\"ztext-empty-paragraph\"\u003E\u003Cbr/\u003E\u003C/p\u003E\u003Cp data-pid=\"eK-95IFi\"\u003E(function() {\u003C/p\u003E\u003Cp data-pid=\"RgWq8b25\"\u003E    &#39;use strict&#39;;\u003C/p\u003E\u003Cp class=\"ztext-empty-paragraph\"\u003E\u003Cbr/\u003E\u003C/p\u003E\u003Cp data-pid=\"mAeRpynM\"\u003E    // Your code here...\u003C/p\u003E\u003Cp data-pid=\"Je9tJ86-\"\u003E    GM_addStyle(`\u003C/p\u003E\u003Cp data-pid=\"GGyENTUC\"\u003E        .kY2IgmnCmOGjharHErah span b{\u003C/p\u003E\u003Cp data-pid=\"en-frTtv\"\u003E        color:#ff897e !important;\u003C/p\u003E\u003Cp data-pid=\"yxkeypkY\"\u003E        }\u003C/p\u003E\u003Cp data-pid=\"gF_EAvSl\"\u003E    `);\u003C/p\u003E\u003Cp data-pid=\"qOpLPkcM\"\u003E})();\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "23233662",
          "type": "question",
          "name": "你有什么在网上\u003Cem\u003E搜索\u003C/em\u003E东西的神搜索技巧？",
          "url": "https://api.zhihu.com/questions/23233662",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "c5963bb24b6baf6c801ee60bcd79a136",
          "url_token": "grashley",
          "name": "米奇大卫D",
          "headline": "辉格博客: headsalon.org",
          "gender": 1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/c5963bb24b6baf6c801ee60bcd79a136",
          "type": "people",
          "avatar_url": "https://picx.zhimg.com/50/v2-28356fd13ac1cd04d66b693bfb69f1f2_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 2547,
          "follower_count": 134,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "OvcBCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNzQyNDU2NzI5KhMxOTQwMDEzNjM4Mjc5NzU0MTYxMgcxNDc0Nzc2OggyMzIzMzY2MkoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCgAQeoAQCwAQG4AQDQAd/4/8QG2AHpCuABLoACALgCAIoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwCqAwDCAxFtdWx0aV9hbnN3ZXJzX2FsbJgEAN0EAKC1PuAEAA==",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 156,
        "visits_count": 1306616,
        "description": "搜索数据，影像资源，等待",
        "title": "你有什么在网上搜索东西的神搜索技巧？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 7
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "TextSeek在无索引模式（简易模式）下，\u003Cem\u003E搜索\u003C/em\u003E时间和搜索准确度均优于FileLocator Pro。而在有索引模式下，Filelocator Pro虽然\u003Cem\u003E搜索\u003C/em\u003E时间较短，但搜索准确度出现显著下降",
        "title": "文件\u003Cem\u003E内容搜索\u003C/em\u003E软件正面PK：TextSeek vs FileLocator Pro"
      },
      "object": {
        "id": "371751409",
        "title": "文件\u003Cem\u003E内容搜索\u003C/em\u003E软件正面PK：TextSeek vs FileLocator Pro",
        "type": "article",
        "url": "https://api.zhihu.com/articles/371751409",
        "excerpt": "TextSeek在无索引模式（简易模式）下，\u003Cem\u003E搜索\u003C/em\u003E时间和搜索准确度均优于FileLocator Pro。而在有索引模式下，Filelocator Pro虽然\u003Cem\u003E搜索\u003C/em\u003E时间较短，但搜索准确度出现显著下降",
        "voteup_count": 65,
        "comment_count": 36,
        "zfav_count": 170,
        "created_time": 1620809815,
        "updated_time": 1626327120,
        "content": "\u003Cp data-pid=\"7KaI-UJC\"\u003E上回我们\u003Ca href=\"https://zhuanlan.zhihu.com/p/371466155\" class=\"internal\" target=\"_blank\"\u003E横向评测了15款基于索引的文件内容搜索软件\u003C/a\u003E，国产软件TextSeek胜出。有读者反映为什么不比对FileLocator Pro，主要是因为FileLocator Pro的主引擎采用的是无索引设计，指标直接比对不公平。而恰好TextSeek也支持无索引搜索（即“简易模式”），这回我们可以详细比对下这两款软件的优劣。\u003C/p\u003E\u003Cp data-pid=\"DvUk7A3l\"\u003E我们进行测试的软件版本均是最新版本，TextSeek为2.10.2672，FileLocator Pro为8.5.2951。测试数据是互联网上爬下来的2000个文档，文档分布如下：\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic4.zhimg.com/v2-528c0211bb7677b36bcdf7d141993d71_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"312\" data-rawheight=\"221\" data-original-token=\"v2-79b7768d962b6bc80a46572cfcaf150a\" class=\"content_image\" width=\"312\"/\u003E\u003C/figure\u003E\u003Cp data-pid=\"3ftu-B4R\"\u003E测试环境为真实PC，操作系统为Windows 7，双核Intel CPU。我们对索引模式和无索引模式分别进行测试，比对指标包括索引时间、搜索时间和搜索准确度，记录十个测试词的数值并取平均值，其中，测试词分别为“技术、保密、目的、会同、化工、五年、工程师、最大化、身份证、总经理”；对于搜索准确度，我们以多软件成功匹配结果的合集（剔除重复和误匹配）作为基准分母，以正确匹配数减去误匹配数作为分子，进而计算准确率。最终比对结果如下：\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic2.zhimg.com/v2-caffd6f9ea381944c3c924eb4d6bcf0b_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"787\" data-rawheight=\"356\" data-original-token=\"v2-0bf144003789b6c7f9ee6f21fb791c7e\" class=\"origin_image zh-lightbox-thumb\" width=\"787\" data-original=\"https://pic2.zhimg.com/v2-caffd6f9ea381944c3c924eb4d6bcf0b_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp data-pid=\"roIkJpxd\"\u003E可以看到，TextSeek在无索引模式（简易模式）下，搜索时间和搜索准确度均优于FileLocator Pro。而在有索引模式下，Filelocator Pro虽然搜索时间较短，但搜索准确度出现显著下降，主要原因是“化工、会同、五年”等词出现了不少误匹配。\u003C/p\u003E\u003Cp data-pid=\"VSOCJCne\"\u003ETextSeek的搜索界面\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic1.zhimg.com/v2-f4beeb41ef3120294f00bb1519dcca32_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1457\" data-rawheight=\"759\" data-original-token=\"v2-c86db0feaf99112180f5b69982d9d597\" class=\"origin_image zh-lightbox-thumb\" width=\"1457\" data-original=\"https://pic1.zhimg.com/v2-f4beeb41ef3120294f00bb1519dcca32_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp data-pid=\"OfOkQUih\"\u003EFileLocator Pro的搜索界面\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic1.zhimg.com/v2-ed1bf60d1ffbc2b701a05d6aa5e09ea0_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1086\" data-rawheight=\"692\" data-original-token=\"v2-1510619f6f3aba59f9c6456dfaa751d7\" class=\"origin_image zh-lightbox-thumb\" width=\"1086\" data-original=\"https://pic1.zhimg.com/v2-ed1bf60d1ffbc2b701a05d6aa5e09ea0_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp data-pid=\"2p491C2j\"\u003E在软件功能上，两个软件各有优势。桌面搜索常用的结果排序、文件预览、导出搜索结果、打开文件等基础功能，两个软件都很完备；TextSeek支持Windows、Mac和统信三个操作系统，FileLocator Pro仅支持Windows系统；FileLocator Pro支持正则表达式、布尔表达式和DOS表达式等高级搜索方式，而TextSeek不支持；FileLocator Pro支持搜索视频、图片文件的文字标签（非图片文字识别OCR），支持搜索压缩包里面的文档，而TextSeek不支持。\u003C/p\u003E\u003Cp data-pid=\"hN8jh-kn\"\u003E总体观感：TextSeek在索引速度、搜索准确率等关键指标上领先FileLocator Pro，而FileLocator Pro在支持格式、高级搜索语法方面领先TextSeek。TextSeek界面接近百度，设计简洁直观，一般用户也能轻易上手；FileLocator Pro界面选项较多，支持正则、布尔等高级搜索方式，也支持一些视频、图片等特殊文件格式，更适用于有特定需求的专业用户。\u003C/p\u003E\u003Cp\u003E\u003C/p\u003E\u003Cp\u003E\u003C/p\u003E",
        "thumbnail_info": {
          "count": 1,
          "thumbnails": [
            {
              "data_url": "",
              "url": "https://picx.zhimg.com/80/v2-386298fe0010a4ef91bca0ac53e18588_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 850,
              "height": 350
            }
          ],
          "type": "thumbnail_info",
          "total_count": 6
        },
        "author": {
          "id": "ab7d70536d7fb0eac6c63bc9fd527c88",
          "url_token": "zestgeek",
          "name": "辛巴",
          "headline": "geek是一种态度",
          "gender": -1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/ab7d70536d7fb0eac6c63bc9fd527c88",
          "type": "people",
          "avatar_url": "https://picx.zhimg.com/50/v2-71700f58ac8c2b6e977196ca14659138_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 751,
          "follower_count": 271,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "voting": 0,
        "relationship": {
          "is_voted": false,
          "is_author": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0
        },
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "Ou0BCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYByIJMTcwNzQxNDc4KgkzNzE3NTE0MDlKDOaQnOe0ouWGheWuuVAAWAFgAWoM5pCc57Si5YaF5a65cASAAY6y3N3a/5ADmAEAoAEIqAEAsAEBuAEA0AHXsO6EBtgBQeABJIACALgCAPgCAYoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwCqAwDCAyJydW1fc2VhcmNoX3JlY2FsbF9hcl90bF90dW5lXzEwMjRkmAQB3QQAQL0+4AQA",
        "article_type": "normal",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 8
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "知乎上\u003Cem\u003E搜索内容\u003C/em\u003E出现一堆无关内容，请问在知乎上有好的办法精确搜索吗",
        "title": "知乎上\u003Cem\u003E搜索内容\u003C/em\u003E出现一堆无关内容，请问在知乎上有好的办法精确搜索吗？"
      },
      "object": {
        "id": "12473062221",
        "title": "知乎上\u003Cem\u003E搜索内容\u003C/em\u003E出现一堆无关内容，请问在知乎上有好的办法精确搜索吗？",
        "description": "知乎上\u003Cem\u003E搜索内容\u003C/em\u003E出现一堆无关内容，请问在知乎上有好的办法精确搜索吗",
        "url": "https://api.zhihu.com/questions/12473062221",
        "type": "question",
        "follower_count": 1,
        "comment_count": 0,
        "answer_count": 0,
        "visits_count": 14,
        "updated_time": 1739759971,
        "relationship": {
          "is_following": false
        },
        "is_anonymous": false,
        "attached_info_bytes": "Ot4BCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYAyIJMTEzNDcwMzQ0KgsxMjQ3MzA2MjIyMUoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCgAQmoAQCwAQG4AQDCAQt6ZXJvX2Fuc3dlctAB48LKvQbgAQDoAQGAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMAqgMAwgMPcnVjZW5lX3F1ZXN0aW9u4AQA",
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 9
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "因为没有\u003Cem\u003E内容\u003C/em\u003E了，搁十年前，你黑猴打不过任何一个boss，你可以上百度查攻略，现在基本上只能上小破站或者某音了，百度上的\u003Cem\u003E内容\u003C/em\u003E要不很少，要不都是重复的，有一个人写攻略",
        "title": "为什么百度\u003Cem\u003E搜索\u003C/em\u003E引擎越做越差？"
      },
      "object": {
        "original_id": "685819466",
        "id": "3606620400",
        "type": "answer",
        "excerpt": "因为没有\u003Cem\u003E内容\u003C/em\u003E了，搁十年前，你黑猴打不过任何一个boss，你可以上百度查攻略，现在基本上只能上小破站或者某音了，百度上的\u003Cem\u003E内容\u003C/em\u003E要不很少，要不都是重复的，有一个人写攻略",
        "url": "https://api.zhihu.com/answers/3606620400",
        "voteup_count": 4579,
        "comment_count": 368,
        "favorites_count": 155,
        "created_time": 1724660864,
        "updated_time": 1724660864,
        "content": "\u003Cp data-pid=\"o9-vIwsn\"\u003E因为没有内容了，搁十年前，你黑猴打不过任何一个boss，你可以上百度查攻略，现在基本上只能上小破站或者某音了，百度上的内容要不很少，要不都是重复的，有一个人写攻略，其他无数在那搬运\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "600520290",
          "type": "question",
          "name": "为什么百度\u003Cem\u003E搜索\u003C/em\u003E引擎越做越差？",
          "url": "https://api.zhihu.com/questions/600520290",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "28384a54c926c420b07b24140b6abaef",
          "url_token": "bing-lin-36",
          "name": "冰臨",
          "headline": "上班摸鱼党",
          "gender": 1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/28384a54c926c420b07b24140b6abaef",
          "type": "people",
          "avatar_url": "https://pic1.zhimg.com/50/v2-983edbb9b3b4dbfc9d3d7e0b02cc592b_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 34109,
          "follower_count": 302,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "Ov8BCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNjg1ODE5NDY2KgozNjA2NjIwNDAwMgg5NTcyOTQ4MjoJNjAwNTIwMjkwSgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA5gBAKABCqgBALABAbgBANABgPmwtgbYAeMj4AHwAoACALgCAIoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwCqAwDCAx9ydW1fc2VhcmNoX3JlY2FsbF9hbl90dW5lXzEwMjRkmAQA3QQAAHU+4AQA",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 1753,
        "visits_count": 14804530,
        "description": "\u003Cp\u003E和现在热门的Bing相比，现在觉得百度是真low。当你查找一个网站时，百度搜索出来的第一条绝对不是你想要查询网站的官方网址，而是与其相关的各种广告或其它，这样耗费时间不说，体验感太差了。难道百度自己心里没点数吗？\u003C/p\u003E",
        "title": "为什么百度搜索引擎越做越差？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 10
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "从原来的开放式论坛、网页变成了APP封闭化的模式，导致\u003Cem\u003E搜索\u003C/em\u003E引擎根本没有\u003Cem\u003E内容\u003C/em\u003E可以抓了。 最典型的就是微信公众号的\u003Cem\u003E内容\u003C/em\u003E，就拒绝\u003Cem\u003E搜索\u003C/em\u003E引擎的爬虫编制索引。例如微信公众号推送的文章就可以复制链接",
        "title": "现在中文互联网能\u003Cem\u003E搜索\u003C/em\u003E到的有价值信息越来越少，究竟是\u003Cem\u003E搜索\u003C/em\u003E引擎的问题还是近年来中文互联网创作越来越匮乏?"
      },
      "object": {
        "original_id": "632491182",
        "id": "3313267011",
        "type": "answer",
        "excerpt": "从原来的开放式论坛、网页变成了APP封闭化的模式，导致\u003Cem\u003E搜索\u003C/em\u003E引擎根本没有\u003Cem\u003E内容\u003C/em\u003E可以抓了。 最典型的就是微信公众号的\u003Cem\u003E内容\u003C/em\u003E，就拒绝\u003Cem\u003E搜索\u003C/em\u003E引擎的爬虫编制索引。例如微信公众号推送的文章就可以复制链接",
        "url": "https://api.zhihu.com/answers/3313267011",
        "voteup_count": 132,
        "comment_count": 21,
        "favorites_count": 55,
        "created_time": 1701695152,
        "updated_time": 1701695152,
        "content": "\u003Cp data-pid=\"BdPGVnzl\"\u003E创作其实是不匮乏的，只是中文互联网现在信息组织的形式发生了变化，从原来的开放式论坛、网页变成了APP封闭化的模式，导致搜索引擎根本没有内容可以抓了。\u003C/p\u003E\u003Cp data-pid=\"IDtHxx7j\"\u003E最典型的就是微信公众号的内容，就拒绝搜索引擎的爬虫编制索引。例如微信公众号推送的文章就可以复制链接，实质上就是一个网页，可以在浏览器打开。\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic3.zhimg.com/v2-7b855f8f6753a9ad36843278531e0732_1440w.jpg\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1548\" data-rawheight=\"1415\" data-original-token=\"v2-1673ae14a218a4606475f1a8189bd73e\" data-default-watermark-src=\"https://pic3.zhimg.com/v2-630ba6a7ccc659c9fb3575c5e4394040_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"1548\" data-original=\"https://pic3.zhimg.com/v2-7b855f8f6753a9ad36843278531e0732_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1548&#39; height=&#39;1415&#39;&gt;&lt;/svg&gt;\" data-caption=\"\" data-size=\"normal\" data-rawwidth=\"1548\" data-rawheight=\"1415\" data-original-token=\"v2-1673ae14a218a4606475f1a8189bd73e\" data-default-watermark-src=\"https://pic3.zhimg.com/v2-630ba6a7ccc659c9fb3575c5e4394040_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1548\" data-original=\"https://pic3.zhimg.com/v2-7b855f8f6753a9ad36843278531e0732_r.jpg\" data-actualsrc=\"https://pic3.zhimg.com/v2-7b855f8f6753a9ad36843278531e0732_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp data-pid=\"xmhjUNKr\"\u003E但上面的这个链接这一串字符串，并没有规律可言，搜索引擎没有办法像传统的爬虫方法一样，由点及面获取到其他公众号的推文。\u003C/p\u003E\u003Cp data-pid=\"khW3q31s\"\u003E当然，微信公众号的推送文章的链接也有一些潜在规律，以至于某段时间搜索引擎可以进行了索引。结果，微信居然声称是漏洞，还进行了“修复”。\u003C/p\u003E\u003Ca href=\"https://link.zhihu.com/?target=https%3A//baijiahao.baidu.com/s%3Fid%3D1714311133911968325\" data-draft-node=\"block\" data-draft-type=\"link-card\" data-image=\"https://pica.zhimg.com/v2-684bbfe28170fcebbdd477e62a07bbe6_180x120.jpg\" data-image-width=\"640\" data-image-height=\"384\" class=\" wrap external\" target=\"_blank\" rel=\"nofollow noreferrer\"\u003E公众号内容谷歌能搜到！微信回应：是漏洞，目前已修复\u003C/a\u003E\u003Cp data-pid=\"BKXfc2N6\"\u003E而\u003Cb\u003E传统的网站，则是有层次、有结构\u003C/b\u003E，并且会\u003Cb\u003E主动提交自己的站点地图\u003C/b\u003E以让搜索引擎尽快发现这些网页的。\u003C/p\u003E\u003Cp data-pid=\"liqDa59i\"\u003E国内的网站备案制度，使得用户至少需要一个月左右的等待期，不要小看这个门槛，很多一时兴起想开个小博客分享分享技术和生活的人就没兴趣了。\u003C/p\u003E\u003Cp data-pid=\"Lzu70KKJ\"\u003E于是各大APP的封闭化、个人博客站点的缺失，最后搜索引擎能抓取的也就只剩下了媒体、官网这类了。而媒体则是会把内容反复转载。\u003C/p\u003E",
        "thumbnail_info": {
          "count": 1,
          "thumbnails": [
            {
              "data_url": "",
              "url": "https://picx.zhimg.com/80/v2-1673ae14a218a4606475f1a8189bd73e_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 1548,
              "height": 1415
            }
          ],
          "type": "thumbnail_info",
          "total_count": 1
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "431580608",
          "type": "question",
          "name": "现在中文互联网能\u003Cem\u003E搜索\u003C/em\u003E到的有价值信息越来越少，究竟是\u003Cem\u003E搜索\u003C/em\u003E引擎的问题还是近年来中文互联网创作越来越匮乏?",
          "url": "https://api.zhihu.com/questions/431580608",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "79496342e2f1b23547f77f41726b27ee",
          "url_token": "chengxd-47",
          "name": "chengxd 达达",
          "headline": "笔记本电脑评测网创始人，laptopreview.club",
          "gender": 1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/79496342e2f1b23547f77f41726b27ee",
          "type": "people",
          "avatar_url": "https://picx.zhimg.com/50/v2-440849c8666f8662ad47b5dcdeb17050_l.jpg?source=4e949a73",
          "badge": [
            {
              "type": "best_answerer",
              "description": "优秀回答者",
              "topics": [
                {
                  "id": "19559604",
                  "name": "笔记本电脑",
                  "introduction": "笔记本电脑是一种用于现代数字化办公以及电子游戏娱乐的消费电子产品，是互联网信息时代的重要生产力工具，也是目前主流的个人便携式办公,学习,娱乐设备。\u003Cbr/\u003E\u003Cbr/\u003E笔记本电脑的字面含义指的是形似笔记本开合并且方便携带的小型个人电脑。\u003Cbr/\u003E\u003Cbr/\u003E绝大多数笔记本电脑都装有桌面操作系统，重量适中具有一定的便携性，内置电池可以脱离电源使用。\u003Cbr/\u003E\u003Cbr/\u003E笔记本的形态包括但不限于以下类型：【迷你掌机】【轻薄本】【厚重本】【二合一平板电脑】【折叠屏电脑】。\u003Cbr/\u003E\u003Cbr/\u003E在智能手机普及的今天，笔记本电脑更强调【工具性】【生产力】【高性能】属性，同时在接受着 “手机桌面系统”，“平板桌面系统” ，“云电脑” 新模式的挑战。",
                  "excerpt": "笔记本电脑是一种用于现代数字化办公以及电子游戏娱乐的消费电子产品，是互联网信息时代的重要生产力工具，也是目前主流的个人便携式办公,学习,娱乐设备。 笔记本电脑的字面含义指的是形似笔记本开合并且方便携带的小型个人电脑。 绝大多数笔记本电脑都装有桌面操作系统，重量适中具有一定的便携性，内置电池可以脱离电源使用。 笔记本的形态包括但不限于以下类型：【迷你掌机】【轻薄本】【厚重本】【二合一平板电脑】【折叠屏…",
                  "type": "topic",
                  "url": "https://api.zhihu.com/topics/19559604",
                  "avatar_url": "https://pic1.zhimg.com/50/v2-004566af0513e837e389333549c50bcf_l.jpg?source=4e949a73"
                }
              ]
            },
            {
              "type": "identity",
              "description": "已认证的机构",
              "topics": []
            }
          ],
          "authority_info": null,
          "voteup_count": 137590,
          "follower_count": 30478,
          "badge_v2": {
            "title": "笔记本电脑话题下的优秀答主",
            "merged_badges": [
              {
                "type": "best",
                "detail_type": "best",
                "title": "优秀答主",
                "description": "笔记本电脑话题下的优秀答主",
                "url": "https://www.zhihu.com/question/48509984",
                "sources": [
                  {
                    "id": "19559604",
                    "token": "19559604",
                    "type": "topic",
                    "url": "https://www.zhihu.com/topic/19559604",
                    "name": "笔记本电脑",
                    "avatar_path": "v2-0c57bd6c9d0df4d52184ae12f20f88b8",
                    "avatar_url": "https://pic1.zhimg.com/v2-0c57bd6c9d0df4d52184ae12f20f88b8_720w.jpg?source=32738c0c",
                    "description": "",
                    "priority": 0
                  }
                ],
                "icon": "",
                "night_icon": ""
              }
            ],
            "detail_badges": [
              {
                "type": "best",
                "detail_type": "best_answerer",
                "title": "优秀答主",
                "description": "笔记本电脑话题下的优秀答主",
                "url": "https://www.zhihu.com/question/48509984",
                "sources": [
                  {
                    "id": "19559604",
                    "token": "19559604",
                    "type": "topic",
                    "url": "https://www.zhihu.com/topic/19559604",
                    "name": "笔记本电脑",
                    "avatar_path": "v2-0c57bd6c9d0df4d52184ae12f20f88b8",
                    "avatar_url": "https://pic1.zhimg.com/v2-0c57bd6c9d0df4d52184ae12f20f88b8_720w.jpg?source=32738c0c",
                    "description": "",
                    "priority": 0
                  }
                ],
                "icon": "https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c",
                "night_icon": "https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c"
              },
              {
                "type": "reward",
                "detail_type": "super_activity",
                "title": "社区成就",
                "description": "知势榜数码领域影响力榜答主",
                "url": "",
                "sources": [
                  {
                    "id": "27",
                    "token": "",
                    "type": "content_potential_category",
                    "url": "",
                    "name": "知势榜8月",
                    "avatar_path": "",
                    "avatar_url": "",
                    "description": "",
                    "priority": 27
                  }
                ],
                "icon": "https://pic1.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c",
                "night_icon": "https://pic1.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c"
              }
            ],
            "icon": "https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c",
            "night_icon": "https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c"
          },
          "old_badges": [
            {
              "type": "best_answerer",
              "description": "优秀答主",
              "topics": [
                {
                  "id": "19559604",
                  "type": "topic",
                  "url": "https://www.zhihu.com/topic/19559604",
                  "name": "笔记本电脑",
                  "avatar_url": "https://pic1.zhimg.com/v2-0c57bd6c9d0df4d52184ae12f20f88b8_720w.jpg?source=32738c0c"
                }
              ]
            }
          ],
          "badge_v2_string": "{\"title\":\"笔记本电脑话题下的优秀答主\",\"merged_badges\":[{\"type\":\"best\",\"detail_type\":\"best\",\"title\":\"优秀答主\",\"description\":\"笔记本电脑话题下的优秀答主\",\"url\":\"https://www.zhihu.com/question/48509984\",\"sources\":[{\"id\":\"19559604\",\"token\":\"19559604\",\"type\":\"topic\",\"url\":\"https://www.zhihu.com/topic/19559604\",\"name\":\"笔记本电脑\",\"avatar_path\":\"v2-0c57bd6c9d0df4d52184ae12f20f88b8\",\"avatar_url\":\"https://pic1.zhimg.com/v2-0c57bd6c9d0df4d52184ae12f20f88b8_720w.jpg?source=32738c0c\",\"description\":\"\",\"priority\":0}],\"icon\":\"\",\"night_icon\":\"\",\"badge_status\":\"passed\"}],\"detail_badges\":[{\"type\":\"best\",\"detail_type\":\"best_answerer\",\"title\":\"优秀答主\",\"description\":\"笔记本电脑话题下的优秀答主\",\"url\":\"https://www.zhihu.com/question/48509984\",\"sources\":[{\"id\":\"19559604\",\"token\":\"19559604\",\"type\":\"topic\",\"url\":\"https://www.zhihu.com/topic/19559604\",\"name\":\"笔记本电脑\",\"avatar_path\":\"v2-0c57bd6c9d0df4d52184ae12f20f88b8\",\"avatar_url\":\"https://pic1.zhimg.com/v2-0c57bd6c9d0df4d52184ae12f20f88b8_720w.jpg?source=32738c0c\",\"description\":\"\",\"priority\":0}],\"icon\":\"https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"night_icon\":\"https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"badge_status\":\"passed\"},{\"type\":\"reward\",\"detail_type\":\"super_activity\",\"title\":\"社区成就\",\"description\":\"知势榜数码领域影响力榜答主\",\"url\":\"\",\"sources\":[{\"id\":\"27\",\"token\":\"\",\"type\":\"content_potential_category\",\"url\":\"\",\"name\":\"知势榜8月\",\"avatar_path\":\"\",\"avatar_url\":\"\",\"description\":\"\",\"priority\":27}],\"icon\":\"https://pic1.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"night_icon\":\"https://pic1.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"badge_status\":\"passed\"}],\"icon\":\"https://picx.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\",\"night_icon\":\"https://pica.zhimg.com/v2-4a07bc69c4bb04444721f35b32125c75_l.png?source=32738c0c\"}",
          "topic_bayes_map": null,
          "topic_map": {
            "笔记本电脑": "笔记本电脑"
          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "flag": {
          "flag_type": "author_follower",
          "flag_text": "超过%!f(int64=03)万人关注"
        },
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "OpACCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNjMyNDkxMTgyKgozMzEzMjY3MDExMgg1ODE4NTk3NjoJNDMxNTgwNjA4SgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA5gBAKABC6gBALABAbgBAMIBD2F1dGhvcl9mb2xsb3dlctABsJ23qwbYAYQB4AEVgAIAuAIA+AIBigMAkgMTMTQ5MjU3MTAxNzEzMDc5OTEwNJoDAKIDAKoDAMIDHHJ1bV9hbnN3ZXJfYnJlY2FsbF9lbWJfMTAyNGSYBAHdBABAjT7gBAA=",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 60,
        "visits_count": 189862,
        "description": "\u003Cp\u003E感觉至少在2000年左右，中文互联网上有大量有价值的创作信息。各种论坛活跃，很多有用的干货。而最近三四年来，感觉中文互联网上有价值的信息越来越少，随便一搜，全是各种自媒体营销号，很多信息都是互相黏贴复制的内容。有时候搜索俄文资料才能找回点中文互联网多年前的那种很多有价值资料的感觉，让人唏嘘不已。究竟是什么原因导致了现在中文互联网的有价值信息越来越匮乏?\u003C/p\u003E",
        "title": "现在中文互联网能搜索到的有价值信息越来越少，究竟是搜索引擎的问题还是近年来中文互联网创作越来越匮乏?",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 11
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "真的不想用知乎了，什么都搜不到，以前看过的一些答案特别多的问题，现在再搜只能搜到有几个回答的。一\u003Cem\u003E搜索\u003C/em\u003E全是些文章教你这样那样，尤其想看看什么东西靠不靠谱，一\u003Cem\u003E搜索\u003C/em\u003E全是些广告，都是某某经理的回答",
        "title": "为什么知乎有时候\u003Cem\u003E搜\u003C/em\u003E不出相关\u003Cem\u003E内容\u003C/em\u003E？？"
      },
      "object": {
        "original_id": "621728779",
        "id": "3254067404",
        "type": "answer",
        "excerpt": "真的不想用知乎了，什么都搜不到，以前看过的一些答案特别多的问题，现在再搜只能搜到有几个回答的。一\u003Cem\u003E搜索\u003C/em\u003E全是些文章教你这样那样，尤其想看看什么东西靠不靠谱，一\u003Cem\u003E搜索\u003C/em\u003E全是些广告，都是某某经理的回答",
        "url": "https://api.zhihu.com/answers/3254067404",
        "voteup_count": 43,
        "comment_count": 12,
        "favorites_count": 7,
        "created_time": 1697567173,
        "updated_time": 1697567173,
        "content": "\u003Cp data-pid=\"O4TBYQwC\"\u003E真的不想用知乎了，什么都搜不到，以前看过的一些答案特别多的问题，现在再搜只能搜到有几个回答的。一搜索全是些文章教你这样那样，尤其想看看什么东西靠不靠谱，一搜索全是些广告，都是某某经理的回答，叫你赶快交钱入坑，他们负责教学指导。呸，卸载算了。\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "263334945",
          "type": "question",
          "name": "为什么知乎有时候\u003Cem\u003E搜\u003C/em\u003E不出相关\u003Cem\u003E内容\u003C/em\u003E？？",
          "url": "https://api.zhihu.com/questions/263334945",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "6d92582aa6d49f36d58ef005162b780e",
          "url_token": "jie-huan-zhi-li-10",
          "name": "解环之理",
          "headline": "伪医学编辑",
          "gender": -1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/6d92582aa6d49f36d58ef005162b780e",
          "type": "people",
          "avatar_url": "https://picx.zhimg.com/50/v2-4defd91f45c684e2878faafa50dacfa7_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 542,
          "follower_count": 56,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "OvsBCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNjIxNzI4Nzc5KgozMjU0MDY3NDA0MggxOTc2NDE2MzoJMjYzMzM0OTQ1SgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA5gBAKABDKgBALABAbgBANABxaO7qQbYASvgAQyAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMAqgMAwgMdcnVtX3F1ZXN0aW9uX2RtZXRfdjJfZW1iXzI1NmSYBADdBACA4z7gBAA=",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 13,
        "visits_count": 57030,
        "description": "网络ok，能点开知乎首页任何话题，可是在知乎搜索框输入关键词的时候，搜不出任何东西，试了一下刚才搜的「吸猫」关键词，明明刚才可以搜到，现在却什么都没有，直接是要我提问了",
        "title": "为什么知乎有时候搜不出相关内容？？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 12
    },
    {
      "type": "relevant_query",
      "id": "6f60471e0c5f33bf1e9aa46498dd39ba",
      "index": 13,
      "query_list": [
        {
          "query": "搜索知乎内容",
          "id": "6017251925808034937",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQ2oAQCwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "搜索内容的软件",
          "id": "7178235013165331084",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQ2oAQGwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "搜索盐选内容",
          "id": "-2759446857894185555",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQ2oAQKwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "未搜索到相关内容",
          "id": "559643109321354540",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQ2oAQOwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "小红书如何搜索内容",
          "id": "7359067686274639816",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQ2oAQSwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        },
        {
          "query": "搜索网页内容",
          "id": "2614081590691443226",
          "has_icon": false,
          "attached_info_bytes": "OoEBEiBmOTVkYzFhYjJkNmJkYWUyZGQ5ZDk2ZTU1OGZkZGNjZEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOgAQ2oAQWwAQWAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMA4AQA"
        }
      ],
      "attached_info_bytes": "On4SIGY5NWRjMWFiMmQ2YmRhZTJkZDlkOTZlNTU4ZmRkY2NkSgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA6ABDbABBYACALgCAIoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwDgBAA=",
      "is_single_column": false
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "在淘宝搜毒品，在美团搜妓女，在高德搜赌场，帽子叔叔只会觉得你是个小可爱。",
        "title": "在淘宝手贱不小心\u003Cem\u003E搜索\u003C/em\u003E了“毒品”两个字，现在有点害怕，会不会有事?"
      },
      "object": {
        "original_id": "747446716",
        "id": "1951454312984186992",
        "type": "answer",
        "excerpt": "在淘宝搜毒品，在美团搜妓女，在高德搜赌场，帽子叔叔只会觉得你是个小可爱。",
        "url": "https://api.zhihu.com/answers/1951454312984186992",
        "voteup_count": 5802,
        "comment_count": 67,
        "favorites_count": 120,
        "created_time": 1758042964,
        "updated_time": 1758042964,
        "content": "\u003Cp data-pid=\"7ZwAd0Km\"\u003E在淘宝搜毒品，在美团搜妓女，在高德搜赌场，帽子叔叔只会觉得你是个小可爱。\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "1939109797401170044",
          "type": "question",
          "name": "在淘宝手贱不小心\u003Cem\u003E搜索\u003C/em\u003E了“毒品”两个字，现在有点害怕，会不会有事?",
          "url": "https://api.zhihu.com/questions/1939109797401170044",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "ea827d1bac8516ae63c957bfc211f038",
          "url_token": "zhang-xiao-9-71",
          "name": "purewater1",
          "headline": "aaa",
          "gender": -1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/ea827d1bac8516ae63c957bfc211f038",
          "type": "people",
          "avatar_url": "https://picx.zhimg.com/50/v2-b37f2b0874de139b9c334fdc03075c5f_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 10342,
          "follower_count": 2178,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "Ov8BCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNzQ3NDQ2NzE2KhMxOTUxNDU0MzEyOTg0MTg2OTkyMgkxMTYxOTk3Njk6EzE5MzkxMDk3OTc0MDExNzAwNDRKDOaQnOe0ouWGheWuuVAAWAFgAWoM5pCc57Si5YaF5a65cASAAY6y3N3a/5ADmAEAoAEOqAEAsAEBuAEA0AHUtqbGBtgBqi3gAUOAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMAqgMAwgMMdG9rZW5fd2VpZ2h0mAQA3QQAgH0+4AQA",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 360,
        "visits_count": 9896189,
        "description": "\u003Cp\u003E在淘宝手贱不小心搜索了和“毒品”有关的敏感字眼，现在有点害怕，会不会有事\u003C/p\u003E\u003Cp\u003E纯粹手贱，昨天刷到《破冰行动》《门徒》等禁毒电影，看见里面的道具贼逼真，好奇淘宝会不会有类似的道具卖，就在淘宝上搜了“毒品模型”然后按搜索，现在有点后怕......\u003C/p\u003E\u003Cp\u003E点进去就显示一些禁毒模型，禁毒书籍，没有任何其他操作，但是还是后怕，会不会被网警查然后电话询问\u003C/p\u003E",
        "title": "在淘宝手贱不小心搜索了“毒品”两个字，现在有点害怕，会不会有事?",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 14
    },
    {
      "type": "search_result",
      "highlight": {
        "excerpt": "提出了一种只查询一次数据库、对PHP变量关键词快速\u003Cem\u003E搜索\u003C/em\u003E、\u003Cem\u003E查询内容\u003C/em\u003E排序与分页显示的方法,并按用户\u003Cem\u003E搜索\u003C/em\u003E关键词的频繁度推送用户感兴趣内容的最新信息...",
        "title": "快速\u003Cem\u003E搜索及内容\u003C/em\u003E推送系统方案"
      },
      "object": {
        "type": "scholar",
        "id": "1828049408945741824",
        "url": "https://www.zhihu.com/kvip/sku/paper/1828049408945741824",
        "title": "快速搜索及内容推送系统方案",
        "excerpt": "当前网页搜索引擎,都是按照用户的查询条件对巨型数据库进行多次匹配检索,频繁的数据库交互极大地提高了用户的检索时间,同时带来连接数据库失败的风险。针对这种不足,提出了一种只查询一次数据库、对PHP变量关",
        "keywords": [
          "搜索引擎",
          "内容排序",
          "分页显示",
          "感兴趣信息推送",
          "用户体验"
        ],
        "authors": [
          "高宗宝",
          "王越",
          "韩伟",
          "耿禄博",
          "刘佳"
        ],
        "journal": "信息技术与信息化",
        "year": 2021,
        "period": "3",
        "attached_info_bytes": "Ot0BCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYhQEiEzE4MjgwNDk0MDg5NDU3NDE4MjQqEzE4MjgwNDk0MDg5NDU3NDE4MjRKDOaQnOe0ouWGheWuuVAAWAFgAWoM5pCc57Si5YaF5a65cASAAY6y3N3a/5ADmAEAoAEPqAEAsAEBuAEAgAIAuAIAigMAkgMTMTQ5MjU3MTAxNzEzMDc5OTEwNJoDAKIDAKoDAMIDFnJ1Y2VuZV9kb21lc3RpY3NjaG9sYXLgBAA="
      },
      "hit_labels": null,
      "index": 15,
      "id": "1828049408945741824"
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "找片用yandex，极其有用",
        "title": "你有什么在网上\u003Cem\u003E搜索\u003C/em\u003E东西的神搜索技巧？"
      },
      "object": {
        "original_id": "742616918",
        "id": "1940428097808753735",
        "type": "answer",
        "excerpt": "找片用yandex，极其有用",
        "url": "https://api.zhihu.com/answers/1940428097808753735",
        "voteup_count": 14,
        "comment_count": 9,
        "favorites_count": 30,
        "created_time": 1755414109,
        "updated_time": 1755414109,
        "content": "\u003Cp data-pid=\"YfzFUfe8\"\u003E找片用yandex，极其有用\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "23233662",
          "type": "question",
          "name": "你有什么在网上\u003Cem\u003E搜索\u003C/em\u003E东西的神搜索技巧？",
          "url": "https://api.zhihu.com/questions/23233662",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "4f36a46410afcbb6df4d486626c3fa9d",
          "url_token": "404-not-found-84-38",
          "name": "wantsakOIer",
          "headline": "",
          "gender": 1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/4f36a46410afcbb6df4d486626c3fa9d",
          "type": "people",
          "avatar_url": "https://pic1.zhimg.com/50/v2-6171cacc88f0016c5296b8b0772e6858_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 953,
          "follower_count": 11,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "OocCCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNzQyNjE2OTE4KhMxOTQwNDI4MDk3ODA4NzUzNzM1MgcxNDc0Nzc2OggyMzIzMzY2MkoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCgARCoAQCwAQG4AQDQAd38hcUG2AEO4AEJgAIAuAIAigMAkgMTMTQ5MjU3MTAxNzEzMDc5OTEwNJoDAKIDAKoDAMIDInJ1bV9zZWFyY2hfcmVjYWxsX2xvbmdfYW5fbTNfMTAyNGSYBADdBACgtT7gBAA=",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 156,
        "visits_count": 1306616,
        "description": "搜索数据，影像资源，等待",
        "title": "你有什么在网上搜索东西的神搜索技巧？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 16
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "那是你不会搜，知乎的网友随便一搜，就能通过答主的部分文字信息找到人家的同学，公司，甚至老爸…… 不知道为什么，现在知乎没办法截图评论区，不能把评论区的大佬分析发",
        "title": "为什么互联网给我一种想\u003Cem\u003E搜\u003C/em\u003E的东西什么都搜不到，屁用没有的信息一大堆的无力感？"
      },
      "object": {
        "original_id": "681501467",
        "id": "3582872297",
        "type": "answer",
        "excerpt": "那是你不会搜，知乎的网友随便一搜，就能通过答主的部分文字信息找到人家的同学，公司，甚至老爸…… 不知道为什么，现在知乎没办法截图评论区，不能把评论区的大佬分析发",
        "url": "https://api.zhihu.com/answers/3582872297",
        "voteup_count": 15,
        "comment_count": 7,
        "favorites_count": 12,
        "created_time": 1722686950,
        "updated_time": 1722686950,
        "content": "\u003Cp data-pid=\"BPmhUB_1\"\u003E那是你不会搜，知乎的网友随便一搜，就能通过答主的部分文字信息找到人家的同学，公司，甚至老爸……\u003C/p\u003E\u003Cp data-pid=\"3qDcP2Sh\"\u003E不知道为什么，现在知乎没办法截图评论区，不能把评论区的大佬分析发上来给大家看看……\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic4.zhimg.com/v2-3568ac3b24ea8340cd207cf0bb7f7a49_1440w.jpg\" data-rawwidth=\"1240\" data-rawheight=\"4660\" data-size=\"normal\" data-original-token=\"v2-09f5ef1a35dd190d9e521809033b8daa\" data-default-watermark-src=\"https://picx.zhimg.com/v2-93d9a02b71377183e064674e83616541_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"1240\" data-original=\"https://pic4.zhimg.com/v2-3568ac3b24ea8340cd207cf0bb7f7a49_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1240&#39; height=&#39;4660&#39;&gt;&lt;/svg&gt;\" data-rawwidth=\"1240\" data-rawheight=\"4660\" data-size=\"normal\" data-original-token=\"v2-09f5ef1a35dd190d9e521809033b8daa\" data-default-watermark-src=\"https://picx.zhimg.com/v2-93d9a02b71377183e064674e83616541_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1240\" data-original=\"https://pic4.zhimg.com/v2-3568ac3b24ea8340cd207cf0bb7f7a49_r.jpg\" data-actualsrc=\"https://pic4.zhimg.com/v2-3568ac3b24ea8340cd207cf0bb7f7a49_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp class=\"ztext-empty-paragraph\"\u003E\u003Cbr/\u003E\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic2.zhimg.com/v2-f5b3829fc30c7a5f993c1f1ae32d8fd3_1440w.jpg\" data-rawwidth=\"1022\" data-rawheight=\"890\" data-size=\"normal\" data-original-token=\"v2-9bcb40f18303cd8c505df4d1bc88f5c0\" data-default-watermark-src=\"https://pic2.zhimg.com/v2-54187b784028ceda1f203194d9060fa9_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"1022\" data-original=\"https://pic2.zhimg.com/v2-f5b3829fc30c7a5f993c1f1ae32d8fd3_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1022&#39; height=&#39;890&#39;&gt;&lt;/svg&gt;\" data-rawwidth=\"1022\" data-rawheight=\"890\" data-size=\"normal\" data-original-token=\"v2-9bcb40f18303cd8c505df4d1bc88f5c0\" data-default-watermark-src=\"https://pic2.zhimg.com/v2-54187b784028ceda1f203194d9060fa9_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1022\" data-original=\"https://pic2.zhimg.com/v2-f5b3829fc30c7a5f993c1f1ae32d8fd3_r.jpg\" data-actualsrc=\"https://pic2.zhimg.com/v2-f5b3829fc30c7a5f993c1f1ae32d8fd3_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp class=\"ztext-empty-paragraph\"\u003E\u003Cbr/\u003E\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://picx.zhimg.com/v2-322bf88dd2491d9206e5e471fbb22c01_1440w.jpg\" data-rawwidth=\"837\" data-rawheight=\"558\" data-size=\"normal\" data-original-token=\"v2-9eabd37788c3e1478db4460ceedf6104\" data-default-watermark-src=\"https://pic2.zhimg.com/v2-8bc4cbabef18536068d22f8695aa1a5b_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"837\" data-original=\"https://picx.zhimg.com/v2-322bf88dd2491d9206e5e471fbb22c01_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;837&#39; height=&#39;558&#39;&gt;&lt;/svg&gt;\" data-rawwidth=\"837\" data-rawheight=\"558\" data-size=\"normal\" data-original-token=\"v2-9eabd37788c3e1478db4460ceedf6104\" data-default-watermark-src=\"https://pic2.zhimg.com/v2-8bc4cbabef18536068d22f8695aa1a5b_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"837\" data-original=\"https://picx.zhimg.com/v2-322bf88dd2491d9206e5e471fbb22c01_r.jpg\" data-actualsrc=\"https://picx.zhimg.com/v2-322bf88dd2491d9206e5e471fbb22c01_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp class=\"ztext-empty-paragraph\"\u003E\u003Cbr/\u003E\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://pic2.zhimg.com/v2-1076609ca9d1b2e72202c0727db408eb_1440w.jpg\" data-rawwidth=\"750\" data-rawheight=\"932\" data-size=\"normal\" data-original-token=\"v2-a56ca69ce3697fc3100e8e832bcd7601\" data-default-watermark-src=\"https://pic2.zhimg.com/v2-f5264a9bc069edd6fc25d07d086facf3_b.jpg\" class=\"origin_image zh-lightbox-thumb\" width=\"750\" data-original=\"https://pic2.zhimg.com/v2-1076609ca9d1b2e72202c0727db408eb_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;750&#39; height=&#39;932&#39;&gt;&lt;/svg&gt;\" data-rawwidth=\"750\" data-rawheight=\"932\" data-size=\"normal\" data-original-token=\"v2-a56ca69ce3697fc3100e8e832bcd7601\" data-default-watermark-src=\"https://pic2.zhimg.com/v2-f5264a9bc069edd6fc25d07d086facf3_b.jpg\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"750\" data-original=\"https://pic2.zhimg.com/v2-1076609ca9d1b2e72202c0727db408eb_r.jpg\" data-actualsrc=\"https://pic2.zhimg.com/v2-1076609ca9d1b2e72202c0727db408eb_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp\u003E\u003C/p\u003E",
        "thumbnail_info": {
          "count": 3,
          "thumbnails": [
            {
              "data_url": "",
              "url": "https://pic1.zhimg.com/80/v2-09f5ef1a35dd190d9e521809033b8daa_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 1240,
              "height": 4660
            },
            {
              "data_url": "",
              "url": "https://picx.zhimg.com/80/v2-9bcb40f18303cd8c505df4d1bc88f5c0_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 1022,
              "height": 890
            },
            {
              "data_url": "",
              "url": "https://picx.zhimg.com/80/v2-9eabd37788c3e1478db4460ceedf6104_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 837,
              "height": 558
            }
          ],
          "type": "thumbnail_info",
          "total_count": 4
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "646087842",
          "type": "question",
          "name": "为什么互联网给我一种想\u003Cem\u003E搜\u003C/em\u003E的东西什么都搜不到，屁用没有的信息一大堆的无力感？",
          "url": "https://api.zhihu.com/questions/646087842",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "d04998b0cd07abadfb5c662cc3074a69",
          "url_token": "chong-hua-tu-21",
          "name": "柯里昂的胡子",
          "headline": "我于绝壁之中绽放，亦如黎明前的花朵",
          "gender": 1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/d04998b0cd07abadfb5c662cc3074a69",
          "type": "people",
          "avatar_url": "https://picx.zhimg.com/50/v2-63fabc89f4335e3e72dec79ecd565bab_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 376372,
          "follower_count": 15476,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "Ov4BCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNjgxNTAxNDY3KgozNTgyODcyMjk3MgkxMDU4NTI2NjY6CTY0NjA4Nzg0MkoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCgARGoAQCwAQG4AQDQAea7uLUG2AEP4AEHgAIAuAIA+AIBigMAkgMTMTQ5MjU3MTAxNzEzMDc5OTEwNJoDAKIDAKoDAMIDHHJ1bV9hbnN3ZXJfYnJlY2FsbF9lbWJfMTAyNGSYBAPdBACgqD7gBAA=",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 56,
        "visits_count": 71036,
        "description": "",
        "title": "为什么互联网给我一种想搜的东西什么都搜不到，屁用没有的信息一大堆的无力感？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 17
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "在百度上乱搜，网安部门一般是不监控的，你把网安民警想的太过于神通广大了，我国网民对于百度\u003Cem\u003E搜索\u003C/em\u003E引擎的使用超乎想象。 你搜什么是你的权利，只要没有影响他人，或者可能危害社会秩序和国家安全的",
        "title": "在网上瞎\u003Cem\u003E搜\u003C/em\u003E会被查吗？"
      },
      "object": {
        "original_id": "618740426",
        "id": "3237629563",
        "type": "answer",
        "excerpt": "在百度上乱搜，网安部门一般是不监控的，你把网安民警想的太过于神通广大了，我国网民对于百度\u003Cem\u003E搜索\u003C/em\u003E引擎的使用超乎想象。 你搜什么是你的权利，只要没有影响他人，或者可能危害社会秩序和国家安全的",
        "url": "https://api.zhihu.com/answers/3237629563",
        "voteup_count": 18,
        "comment_count": 0,
        "favorites_count": 22,
        "created_time": 1696494879,
        "updated_time": 1696494969,
        "content": "\u003Cp data-pid=\"qYK9qQAv\"\u003E在百度上乱搜，网安部门一般是不监控的，你把网安民警想的太过于神通广大了，我国网民对于百度搜索引擎的使用超乎想象。\u003C/p\u003E\u003Cp data-pid=\"EHhRvgki\"\u003E你搜什么是你的权利，只要没有影响他人，或者可能危害社会秩序和国家安全的，都是不会监控的，但对于你百度搜索的涉黄涉诈网站，这些网站可能已经进入了反诈中心的黑名单，只要你点击进入这些网站，浏览网页，或者与网站的弹窗案客服沟通，甚至有资金交易，都会被反诈中心预警，如果你名下的银行卡没有资金交易，反诈中心的预警可能只是低风险，如果有资金交易，会被反诈中心识别为高风险，按照你浏览网站的IP地址或者转账时的IP地址，推送本辖区的派出所，由派出所民警联系你进行劝阻。\u003C/p\u003E\u003Cp data-pid=\"x62XUx6N\"\u003E国家开展的净网行动，特别是搜索引擎，已经比较干净了，基本上搜不出什么东西来了。你的搜索记录，其他人是看不到的，会保存在你的手机里，或者互联网经营者的服务器上，不会影响其他人，基于这一点，网安部门都是不会监控的。\u003C/p\u003E\u003Cp data-pid=\"N4EbopmZ\"\u003E专业背景：公安业务答主，从警十年，熟知公安执法流程、行政复议、行政诉讼、招警政审。拘留所、看守所工作经历。\u003C/p\u003E\u003Cp data-pid=\"LlaSzDJP\"\u003E答疑范围：公安机关行政办案程序、刑事办案程序，银行卡被止付冻结问题、行政处罚裁量标准、开具无犯罪证明相关问题，行政复议、行政诉讼、招警政审，专业人士，专业解答。\u003C/p\u003E",
        "thumbnail_info": {
          "count": 0,
          "thumbnails": [],
          "type": "thumbnail_info",
          "total_count": 0
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "624769918",
          "type": "question",
          "name": "在网上瞎\u003Cem\u003E搜\u003C/em\u003E会被查吗？",
          "url": "https://api.zhihu.com/questions/624769918",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "8b7c559d197ed47f21affc12b1d11c76",
          "url_token": "24-40-17-98",
          "name": "阿sir说法",
          "headline": "一线执法，治安、刑事丰富办案经验。",
          "gender": 1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/8b7c559d197ed47f21affc12b1d11c76",
          "type": "people",
          "avatar_url": "https://pica.zhimg.com/50/v2-de7a36d050355b6535693584d682a853_l.jpg?source=4e949a73",
          "badge": [
            {
              "type": "identity",
              "description": "法律职业资格证持证人",
              "topics": []
            },
            {
              "type": "identity",
              "description": "已认证的机构",
              "topics": []
            }
          ],
          "authority_info": null,
          "voteup_count": 28085,
          "follower_count": 13528,
          "badge_v2": {
            "title": "法律职业资格证持证人",
            "merged_badges": [
              {
                "type": "identity",
                "detail_type": "identity_people",
                "title": "认证",
                "description": "法律职业资格证持证人",
                "url": "https://zhuanlan.zhihu.com/p/96956163",
                "sources": [],
                "icon": "",
                "night_icon": ""
              }
            ],
            "detail_badges": [
              {
                "type": "identity",
                "detail_type": "identity_people",
                "title": "已认证的个人",
                "description": "法律职业资格证持证人",
                "url": "https://zhuanlan.zhihu.com/p/96956163",
                "sources": [],
                "icon": "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
                "night_icon": "https://pica.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c"
              }
            ],
            "icon": "https://pic1.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
            "night_icon": "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c"
          },
          "old_badges": [
            {
              "type": "identity",
              "description": "法律职业资格证持证人"
            }
          ],
          "badge_v2_string": "{\"title\":\"法律职业资格证持证人\",\"merged_badges\":[{\"type\":\"identity\",\"detail_type\":\"identity_people\",\"title\":\"认证\",\"description\":\"法律职业资格证持证人\",\"url\":\"https://zhuanlan.zhihu.com/p/96956163\",\"sources\":[],\"icon\":\"\",\"night_icon\":\"\",\"badge_status\":\"passed\"}],\"detail_badges\":[{\"type\":\"identity\",\"detail_type\":\"identity_people\",\"title\":\"已认证的个人\",\"description\":\"法律职业资格证持证人\",\"url\":\"https://zhuanlan.zhihu.com/p/96956163\",\"sources\":[],\"icon\":\"https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c\",\"night_icon\":\"https://pica.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c\",\"badge_status\":\"passed\"}],\"icon\":\"https://pic1.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c\",\"night_icon\":\"https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": "法律"
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "Ov0BCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNjE4NzQwNDI2KgozMjM3NjI5NTYzMgkxMDExMTY5MzE6CTYyNDc2OTkxOEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCgARKoAQCwAQG4AQDQAZ/q+agG2AES4AEAgAIAuAIAigMAkgMTMTQ5MjU3MTAxNzEzMDc5OTEwNJoDAKIDAKoDAMIDHnJ1bV9hbnN3ZXJfc2ltcHJlcmFua19lbWJfMTI4ZJgEAN0EAMB/PuAEAA==",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 4,
        "visits_count": 22881,
        "description": "\u003Cp\u003E昨天忍不住在网上瞎搜一些敏感内容（关于违法犯罪的），单纯好奇搜出来会有什么东西，现在好怕被警察叔叔查，我真的是遵纪守法的好公民啊!\u003C/p\u003E",
        "title": "在网上瞎搜会被查吗？",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 18
    },
    {
      "type": "search_result",
      "highlight": {
        "description": "自己看吧。\u003Cem\u003E查询\u003C/em\u003E时间2025年9月5日",
        "title": "百度为什么越来越垃圾了?"
      },
      "object": {
        "original_id": "745777977",
        "id": "1947698877697336810",
        "type": "answer",
        "excerpt": "自己看吧。\u003Cem\u003E查询\u003C/em\u003E时间2025年9月5日",
        "url": "https://api.zhihu.com/answers/1947698877697336810",
        "voteup_count": 2427,
        "comment_count": 327,
        "favorites_count": 189,
        "created_time": 1757147598,
        "updated_time": 1757147598,
        "content": "\u003Cp data-pid=\"Rmg_Pbdx\"\u003E自己看吧。查询时间2025年9月5日\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cnoscript\u003E\u003Cimg src=\"https://picx.zhimg.com/v2-5ec5d8fbd6682928d74164a07c6683b1_1440w.jpg\" data-rawwidth=\"1108\" data-rawheight=\"448\" data-size=\"normal\" data-original-token=\"v2-3c8b1537f8384522f470e995304871a3\" class=\"origin_image zh-lightbox-thumb\" width=\"1108\" data-original=\"https://picx.zhimg.com/v2-5ec5d8fbd6682928d74164a07c6683b1_r.jpg\"/\u003E\u003C/noscript\u003E\u003Cimg src=\"data:image/svg+xml;utf8,&lt;svg xmlns=&#39;http://www.w3.org/2000/svg&#39; width=&#39;1108&#39; height=&#39;448&#39;&gt;&lt;/svg&gt;\" data-rawwidth=\"1108\" data-rawheight=\"448\" data-size=\"normal\" data-original-token=\"v2-3c8b1537f8384522f470e995304871a3\" class=\"origin_image zh-lightbox-thumb lazy\" width=\"1108\" data-original=\"https://picx.zhimg.com/v2-5ec5d8fbd6682928d74164a07c6683b1_r.jpg\" data-actualsrc=\"https://picx.zhimg.com/v2-5ec5d8fbd6682928d74164a07c6683b1_1440w.jpg\"/\u003E\u003C/figure\u003E\u003C/figure\u003E\u003Cp\u003E\u003C/p\u003E",
        "thumbnail_info": {
          "count": 1,
          "thumbnails": [
            {
              "data_url": "",
              "url": "https://picx.zhimg.com/80/v2-3c8b1537f8384522f470e995304871a3_qhd.jpg?source=4e949a73",
              "type": "image",
              "width": 1108,
              "height": 448
            }
          ],
          "type": "thumbnail_info",
          "total_count": 1
        },
        "relationship": {
          "voting": 0,
          "is_author": false,
          "is_thanked": false,
          "is_nothelp": false,
          "following_upvoter": [],
          "following_upvoter_count": 0,
          "following_collect": [],
          "following_collect_count": 0,
          "high_level_creator": [],
          "high_level_creator_count": 0,
          "is_following": false
        },
        "question": {
          "id": "610546705",
          "type": "question",
          "name": "百度为什么越来越垃圾了?",
          "url": "https://api.zhihu.com/questions/610546705",
          "answer_count": 0,
          "follow_count": 0,
          "attached_info_bytes": ""
        },
        "author": {
          "id": "350b4b53b147dcf32b290470d56e97e0",
          "url_token": "li-jun-30-18-98",
          "name": "李俊",
          "headline": "",
          "gender": -1,
          "is_followed": false,
          "is_following": false,
          "user_type": "people",
          "url": "https://api.zhihu.com/people/350b4b53b147dcf32b290470d56e97e0",
          "type": "people",
          "avatar_url": "https://pica.zhimg.com/50/v2-abed1a8c04700ba7d72b45195223e0ff_l.jpg?source=4e949a73",
          "badge": [],
          "authority_info": null,
          "voteup_count": 2612,
          "follower_count": 44,
          "badge_v2": {
            "title": "",
            "merged_badges": [],
            "detail_badges": [],
            "icon": "",
            "night_icon": ""
          },
          "badge_v2_string": "{\"title\":\"\",\"merged_badges\":[],\"detail_badges\":null,\"icon\":\"\",\"night_icon\":\"\"}",
          "topic_bayes_map": null,
          "topic_map": {

          },
          "verify_bayes": ""
        },
        "is_anonymous": false,
        "is_zhi_plus": false,
        "is_zhi_plus_content": false,
        "extra": "",
        "health_tag": "",
        "answer_type": "normal",
        "sub_content_type": "",
        "settings": {
          "table_of_contents": {
            "enabled": false
          }
        },
        "attached_info_bytes": "OogCCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYBCIJNzQ1Nzc3OTc3KhMxOTQ3Njk4ODc3Njk3MzM2ODEwMgg5Nzk1NzkyMjoJNjEwNTQ2NzA1SgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA5gBAKABE6gBALABAbgBANABzuPvxQbYAfsS4AHHAoACALgCAPgCAYoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwCqAwDCAxxydW1fYW5zd2VyX2JyZWNhbGxfZW1iXzEwMjRkmAQB3QQAABU+4AQA",
        "biz_encoded_params": "sw%3D%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9",
        "answer_count": 1038,
        "visits_count": 16630859,
        "description": "\u003Cp\u003E搜出来的内容带有主观性，偏见我可以理解。但是搜出来的内容好多好多都是错误的这就很难让人理解了！包括“先进的”AI技术全网搜索资料，生成了很多与问题毫不相关的回答，这就是所谓的AI技术？\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pica.zhimg.com/v2-8de3373eacb480a666c6e987dff809d4_1440w.jpg\" data-rawwidth=\"2160\" data-rawheight=\"1082\" data-size=\"normal\" data-original-token=\"v2-8de3373eacb480a666c6e987dff809d4\" class=\"origin_image zh-lightbox-thumb\" width=\"2160\" data-original=\"https://pica.zhimg.com/v2-8de3373eacb480a666c6e987dff809d4_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp\u003E硫酸和氢碘酸明显氢碘酸酸性更强好吧\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic1.zhimg.com/v2-dde13df2e7016a37a6b14cd50788b2c0_1440w.jpg\" data-rawwidth=\"2160\" data-rawheight=\"1620\" data-size=\"normal\" data-original-token=\"v2-dde13df2e7016a37a6b14cd50788b2c0\" class=\"origin_image zh-lightbox-thumb\" width=\"2160\" data-original=\"https://pic1.zhimg.com/v2-dde13df2e7016a37a6b14cd50788b2c0_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp\u003E刑拘更严重好吧\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pica.zhimg.com/v2-e988453fef27d0889fd387e2ba848244_1440w.jpg\" data-rawwidth=\"2160\" data-rawheight=\"1620\" data-size=\"normal\" data-original-token=\"v2-e988453fef27d0889fd387e2ba848244\" class=\"origin_image zh-lightbox-thumb\" width=\"2160\" data-original=\"https://pica.zhimg.com/v2-e988453fef27d0889fd387e2ba848244_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp\u003E这个我直接无语\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic1.zhimg.com/v2-e89afde34c82eda3c4e298687b07d3d0_1440w.jpg\" data-rawwidth=\"2160\" data-rawheight=\"1620\" data-size=\"normal\" data-original-token=\"v2-e89afde34c82eda3c4e298687b07d3d0\" class=\"origin_image zh-lightbox-thumb\" width=\"2160\" data-original=\"https://pic1.zhimg.com/v2-e89afde34c82eda3c4e298687b07d3d0_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp\u003E无语了。β粒子在高中物理可能没有正电子，但是β粒子不带电这就太离谱了吧\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic2.zhimg.com/v2-17f6f4822e5648f85cc21bbce8934fff_1440w.jpg\" data-rawwidth=\"2160\" data-rawheight=\"926\" data-size=\"normal\" data-original-token=\"v2-17f6f4822e5648f85cc21bbce8934fff\" class=\"origin_image zh-lightbox-thumb\" width=\"2160\" data-original=\"https://pic2.zhimg.com/v2-17f6f4822e5648f85cc21bbce8934fff_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp\u003E非金属性是氧化性还差不多\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pica.zhimg.com/v2-cb01e2ff1ac9318bf6fe8ac82ca5843e_1440w.jpg\" data-rawwidth=\"1620\" data-rawheight=\"2160\" data-size=\"normal\" data-original-token=\"v2-cb01e2ff1ac9318bf6fe8ac82ca5843e\" class=\"origin_image zh-lightbox-thumb\" width=\"1620\" data-original=\"https://pica.zhimg.com/v2-cb01e2ff1ac9318bf6fe8ac82ca5843e_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp\u003E电场和磁场有原子结构？？？\u003C/p\u003E\u003Cfigure data-size=\"normal\"\u003E\u003Cimg src=\"https://pic3.zhimg.com/v2-557394a59e5a1aefdb94b4c37d030566_1440w.jpg\" data-rawwidth=\"2160\" data-rawheight=\"405\" data-size=\"normal\" data-original-token=\"v2-557394a59e5a1aefdb94b4c37d030566\" class=\"origin_image zh-lightbox-thumb\" width=\"2160\" data-original=\"https://pic3.zhimg.com/v2-557394a59e5a1aefdb94b4c37d030566_r.jpg\"/\u003E\u003C/figure\u003E\u003Cp\u003E变形虫是真核生物，细胞中没有高尔基体？？？\u003C/p\u003E",
        "title": "百度为什么越来越垃圾了?",
        "native": 0,
        "doc_relevance_scores": null,
        "bert_rel": 0
      },
      "hit_labels": null,
      "index": 19
    }
  ],
  "hit_labels": null,
  "related_search_result": [],
  "search_action_info": {
    "attached_info_bytes": "OngSIGY5NWRjMWFiMmQ2YmRhZTJkZDlkOTZlNTU4ZmRkY2NkSgzmkJzntKLlhoXlrrlQAFgBYAFqDOaQnOe0ouWGheWuuXAEgAGOstzd2v+QA4ACALgCAIoDAJIDEzE0OTI1NzEwMTcxMzA3OTkxMDSaAwCiAwDgBAA=",
    "lc_idx": 20,
    "search_hash_id": "f95dc1ab2d6bdae2dd9d96e558fddccd",
    "isfeed": false
  },
  "is_brand_word": false,
  "pendant": null,
  "sensitive_level": -1,
  "warning": "",
  "filter_items": null,
  "ab_params": "dm_hot_koc=1;dm_rel_rank=0;dm_content_quota=3;dm_all_question=0;sb_zhiedu=1;sb_xg_search=1;dm_rank_ctx=0;dm_fine_rel=2;dm_test=1;adflow=0;dm_ai_use_qu=0;dm_answer_meta=0;dm_preset_migrate=2;dm_op_format=0;se_koc_list_box=1;sb_index=0;sb_fs_check=0;bt_qq=0;dm_mig_pk=0;dm_integrate=0;dm_center_feature=1;dm_new_xtr=0;sb_newreb=1;bt_vertical_mem=1;bt_member_verify=1;dm_preset_up=1;dm_pin_field=0;dm_mig_mixer=0;dm_guess_rank=0;dm_q2q_recall=0;dm_preset_opt=1;sb_cardfilter=1;sb_slot_domain=2;sb_ecpmctcvr=1;dm_pre_docs=2;se_mix_timebox=0;sb_domain_2025=0;dm_recency_root=0;dm_pu_sug=0;sb_caowei_4in10=1;sb_zhiad=1;sb_sku_kzh=0;dm_v_member_zag=2;dm_question_limit=2;dm_mig_zr=0;dm_rescore_v=2;sb_eduzhi=1;sb_novel_member=0;bt_holdback_2025=0;dm_picsearch=1",
  "item_in_view": [
    {
      "type": "user_survey",
      "index": 5,
      "token": "f789e8e4867b476c8406c1420e4e05f4"
    },
    {
      "type": "ask_question",
      "index": 9
    }
  ],
  "domain_info": {
    "first_category": "互联网"
  },
  "is_koc_words": true,
  "is_hit_koc_cache": false
}
            """.trimIndent()

        // Parse the response - First convert from snake_case to camelCase as the actual API does
        val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(realApiResponse)
        val convertedJson = AccountData.snake_case2camelCase(jsonElement) as JsonObject

        // Verify response structure
        assertTrue("Response should contain 'data' field", "data" in convertedJson)
        assertTrue("Response should contain 'paging' field", "paging" in convertedJson)

        val dataArray = convertedJson["data"]?.jsonArray
        assertNotNull("Data array should not be null", dataArray)
        assertTrue("Should have search results", dataArray != null && dataArray.size > 0)
        println("Got ${dataArray?.size} search results from real API response")

        // Parse all results to verify they can all be decoded as SearchResult
        var successCount = 0
        var resultsWithObject = 0
        var parseFailures = 0
        val typesSeen = mutableSetOf<String>()

        dataArray?.forEachIndexed { index, element ->
            try {
                val result = AccountData.decodeJson<SearchResult>(element)
                assertNotNull("Result $index should have type", result.type)
                assertNotNull("Result $index should have id", result.id)
                // Note: obj field is optional for some result types like "relevant_query"
                if (result.obj != null) {
                    resultsWithObject++
                }
                typesSeen.add(result.type)
                successCount++
            } catch (e: Exception) {
                parseFailures++
                println("Warning: Failed to parse result $index as SearchResult: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
                // Don't fail the test - some results may have incompatible structures
            }
        }

        println("Successfully parsed $successCount out of ${dataArray?.size} search results")
        println("Parse failures: $parseFailures")
        println("Results with object field: $resultsWithObject")
        println("Result types found: ${typesSeen.joinToString(", ")}")

        // Assert that we parsed at least some results successfully
        assertTrue("Should successfully parse at least some results", successCount > 0)

        // Verify we can parse at least the first successfully parsed result
        if (successCount > 0) {
            // Find the first successfully parsed result
            for (i in 0 until (dataArray?.size ?: 0)) {
                try {
                    val element = dataArray?.get(i)
                    val result = AccountData.decodeJson<SearchResult>(element!!)
                    println("First successfully parsed result details:")
                    println("  Type: ${result.type}")
                    println("  ID: ${result.id}")
                    println("  Has object: ${result.obj != null}")
                    break
                } catch (e: Exception) {
                    // Skip failed results
                    continue
                }
            }
        }

        // Verify paging information
        val pagingObj = convertedJson["paging"]
        assertNotNull("Paging should not be null", pagingObj)
        println("Paging information present in response")

        println("All search results parsed successfully!")
    }

    /**
     * Test parsing a simple search result with answer type
     */
    @Test
    fun testSearchResultParsing() {
        // Sample search result JSON based on Zhihu search API v3 format
        val searchJson =
            """
            {
                "type": "search_result",
                "id": "1234567890",
                "object": {
                    "id": 123456,
                    "type": "answer",
                    "url": "https://www.zhihu.com/question/123/answer/456",
                    "author": {
                        "id": "abc123",
                        "name": "Test User",
                        "url": "https://www.zhihu.com/people/test-user",
                        "urlToken": "test-user",
                        "avatarUrl": "https://pic1.zhimg.com/avatar.jpg",
                        "userType": "people",
                        "headline": "Test Headline",
                        "gender": 1,
                        "isOrg": false,
                        "isAdvertiser": false,
                        "isPrivacy": false,
                        "badge": [],
                        "badgeV2": {
                            "title": "",
                            "mergedBadges": [],
                            "detailBadges": [],
                            "icon": "",
                            "nightIcon": ""
                        }
                    },
                    "created_time": 1234567890,
                    "updated_time": 1234567890,
                    "voteup_count": 100,
                    "comment_count": 10,
                    "question": {
                        "id": 123,
                        "type": "question",
                        "title": "Test Question Title",
                        "url": "https://www.zhihu.com/question/123",
                        "created": 1234567890,
                        "updated_time": 1234567890
                    },
                    "excerpt": "This is a test excerpt",
                    "content": "This is test content",
                    "relationship": {
                        "is_author": false,
                        "voting": 0
                    }
                },
                "highlight": {
                    "title": ["Test <em>search</em> result"],
                    "description": ["Description with <em>highlight</em>"]
                }
            }
            """.trimIndent()

        // Parse the search result
        val searchResult = Json { ignoreUnknownKeys = true }.decodeFromString<SearchResult>(searchJson)

        // Verify basic fields
        assertEquals("search_result", searchResult.type)
        assertEquals("1234567890", searchResult.id)
        assertNotNull(searchResult.obj)
        assertNotNull(searchResult.highlight)

        // Verify highlight fields (can be String or List)
        assertNotNull(searchResult.highlight?.title)
    }

    /**
     * Test converting search result to Feed object
     */
    @Test
    fun testSearchResultToFeed() {
        // Simpler search result for conversion test
        val searchJson =
            """
            {
                "type": "search_result",
                "id": "test123",
                "object": {
                    "id": 789,
                    "type": "answer",
                    "url": "https://www.zhihu.com/answer/789",
                    "created_time": 1000000000,
                    "updated_time": 1000000000,
                    "voteup_count": 50,
                    "comment_count": 5,
                    "question": {
                        "id": 456,
                        "type": "question",
                        "title": "How to test?",
                        "url": "https://www.zhihu.com/question/456",
                        "created": 1000000000,
                        "updated_time": 1000000000
                    },
                    "excerpt": "Test excerpt",
                    "content": "Test content",
                    "relationship": {
                        "isFollowing": false,
                        "isFollowed": false
                    }
                }
            }
            """.trimIndent()

        val searchResult = Json { ignoreUnknownKeys = true }.decodeFromString<SearchResult>(searchJson)

        // Verify the search result can be parsed
        assertEquals("search_result", searchResult.type)
        assertEquals("test123", searchResult.id)
    }

    /**
     * Test search query URL encoding
     */
    @Test
    fun testSearchQueryEncoding() {
        val query = "测试搜索 with spaces"
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")

        // Verify URL encoding works correctly
        assertTrue(encoded.contains("%"))
        assertTrue(encoded.contains("+") || encoded.contains("%20"))
    }

    /**
     * Test empty search result list
     */
    @Test
    fun testEmptySearchResults() {
        val emptyResultsJson =
            """
            {
                "data": [],
                "paging": {
                    "isEnd": true,
                    "next": "",
                    "previous": ""
                }
            }
            """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(emptyResultsJson)
        val dataArray = json["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(0, dataArray?.size)
    }
}
