package com.github.zly2006.zhihu.ui.home.comment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.databinding.FragmentCommentsBinding
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

data class CommentItem(
    val item: DataHolder.Comment,
    val clickTarget: CommentHolder?
)

class CommentsDialog(
    private val httpClient: HttpClient,
    private val content: NavDestination
) : DialogFragment() {
    init {
        require(content is Article || content is Question || content is CommentHolder) {
            "Only Article and Question can have comments"
        }
    }

    private var _binding: FragmentCommentsBinding? = null
    val binding get() = _binding!!

    override fun onStart() {
        super.onStart()
        // Full screen
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)

        val root: View = binding.root

        val list = mutableListOf<CommentItem>()
        Log.i("CommentsDialog", "Loading comments for $content")
        GlobalScope.launch {
            if (content is Article && content.type == "answer") {
                val response =
                    httpClient.get("https://www.zhihu.com/api/v4/answers/${content.id}/root_comments?order_by=score&limit=20") {
                        header("x-requested-with", "fetch")
                    }
                if (response.status.isSuccess()) {
                    val comments = response.body<JsonObject>()
                    val ja = comments["data"]!!.jsonArray
                    val commentsList = try {
                        AccountData.decodeJson<List<DataHolder.Comment>>(ja)
                    } catch (e: Exception) {
                        Log.e("CommentsDialog", "Failed to decode comments", e)
                        Log.e("", ja.toString())
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Failed to decode comments", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }.map {
                        CommentItem(
                            it,
                            if (it.childCommentCount == 0) null
                            else CommentHolder(
                                it.id,
                                content
                            )
                        )
                    }
                    val posStart = list.size
                    list.addAll(commentsList)
                    Log.i("CommentsDialog", "Loaded ${commentsList.size} comments")
                    activity?.runOnUiThread {
                        binding.list.adapter?.notifyItemRangeInserted(posStart, commentsList.size)
                    }
                } else {
                    Log.e("CommentsDialog", "Failed to load comments: ${response.status}, ${response.bodyAsText()}")
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load comments: ${response.status}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (content is CommentHolder) {
                // 楼中楼
                val response =
                    httpClient.get("https://www.zhihu.com/api/v4/comment_v5/comment/${content.commentId}/child_comment?order_by=ts&limit=20&offset=")

                if (response.status.isSuccess()) {
                    val comments = response.body<JsonObject>()
                    val ja = comments["data"]!!.jsonArray
                    val commentsList = try {
                        AccountData.decodeJson<List<DataHolder.Comment>>(ja)
                    } catch (e: Exception) {
                        Log.e("CommentsDialog", "Failed to decode comments", e)
                        Log.e("", ja.toString())
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Failed to decode comments", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }.map { CommentItem(it, null) }
                    val posStart = list.size
                    list.addAll(commentsList)
                    Log.i("CommentsDialog", "Loaded ${commentsList.size} comments")
                    activity?.runOnUiThread {
                        binding.list.adapter?.notifyItemRangeInserted(posStart, commentsList.size)
                    }
                } else {
                    Log.e("CommentsDialog", "Failed to load comments: ${response.status}, ${response.bodyAsText()}")
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load comments: ${response.status}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Comments are not supported for this content", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        binding.list.adapter = CommentAdapter(list, requireActivity(), httpClient, content)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
