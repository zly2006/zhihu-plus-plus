package com.github.zly2006.zhihu.ui.home.question

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentQuestionDetailsAnswerBinding
import com.github.zly2006.zhihu.ui.home.ReadArticleFragment
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

class AnswerListAdapter(
    private val values: List<Feed>,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<AnswerListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentQuestionDetailsAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.summary.text = item.target.excerpt
        holder.details.text = "${item.target.voteup_count} 赞同 · ${item.target.comment_count} 评论"
        holder.author.text = item.target.author.name
        holder.card.setOnClickListener {
            val readArticleFragment = ReadArticleFragment.newInstance(item)

            activity.supportFragmentManager.commit {
                setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_in,
                    0,
                    0
                )
                replace(
                    R.id.nav_host_fragment_activity_main,
                    readArticleFragment
                )
                addToBackStack("Read-Article")
            }
        }
        if (item.target.author.avatar_url.isNotEmpty()) {
            GlobalScope.launch(activity.mainExecutor.asCoroutineDispatcher()) {
                AccountData.httpClient(activity).get(item.target.author.avatar_url).bodyAsChannel().toInputStream().buffered().use {
                    if (values[position] === item) { // avoid sync loading bug
                        val bitmap = BitmapFactory.decodeStream(it)
                        holder.avatar.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentQuestionDetailsAnswerBinding) : RecyclerView.ViewHolder(binding.root) {
        val summary = binding.summary
        val details = binding.details
        val author = binding.author
        val avatar = binding.avatar
        val card = binding.root
    }
}
