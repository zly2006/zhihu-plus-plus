package com.github.zly2006.zhihu.legacy.ui.home.question

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.navigate
import com.github.zly2006.zhihu.databinding.FragmentQuestionDetailsAnswerBinding
import com.github.zly2006.zhihu.loadImage
import io.ktor.client.*

class AnswerListAdapter(
    private val values: List<Feed.AnswerTarget>,
    private val activity: FragmentActivity,
    private val httpClient: HttpClient,
    private val viewModel: QuestionDetailsFragment.QuestionViewModel
) : RecyclerView.Adapter<AnswerListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentQuestionDetailsAnswerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.summary.text = item.excerpt
        holder.details.text = "${item.voteup_count} 赞同 · ${item.comment_count} 评论"
        holder.author.text = item.author.name
        holder.card.setOnClickListener {
            DataHolder.putFeed(
                Feed(target = item, type = "mock")
            )
            activity.navigate(
                Article(
                    viewModel.title.value ?: "${item.author.name}的回答",
                    "answer",
                    item.id,
                    item.author.name,
                    item.author.headline,
                    item.author.avatar_url,
                    item.excerpt
                )
            )
        }
        if (item.author.avatar_url.isNotEmpty()) {
            loadImage(holder, activity, httpClient, item.author.avatar_url) {
                holder.avatar.setImageBitmap(it)
            }
        }
    }

    override fun getItemCount(): Int = values.size

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onAppear()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.onDisappear()
    }

    inner class ViewHolder(binding: FragmentQuestionDetailsAnswerBinding) : RecyclerView.ViewHolder(binding.root),
        LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        val summary = binding.summary
        val details = binding.details
        val author = binding.author
        val avatar = binding.avatar
        val card = binding.root

        init {
            lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        }

        fun onAppear() {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        fun onDisappear() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
    }
}
