package com.github.zly2006.zhihu.ui.home.question

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.navigate
import com.github.zly2006.zhihu.databinding.FragmentQuestionDetailsAnswerBinding
import com.github.zly2006.zhihu.loadImage
import io.ktor.client.*

class AnswerListAdapter(
    private val values: List<Feed>,
    private val activity: FragmentActivity,
    private val httpClient: HttpClient
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
        holder.summary.text = item.target.excerpt
        holder.details.text = "${item.target.voteup_count} 赞同 · ${item.target.comment_count} 评论"
        holder.author.text = item.target.author.name
        holder.card.setOnClickListener {
            activity.navigate(
                Article(
                    "${item.target.author.name}的回答",
                    item.target.type,
                    item.target.id,
                    item.target.author.name,
                    item.target.author.headline,
                    item.target.content,
                    item.target.author.avatar_url,
                    item.target.excerpt
                )
            )
        }
        if (item.target.author.avatar_url.isNotEmpty()) {
            loadImage(holder, activity, httpClient, item.target.author.avatar_url) {
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
