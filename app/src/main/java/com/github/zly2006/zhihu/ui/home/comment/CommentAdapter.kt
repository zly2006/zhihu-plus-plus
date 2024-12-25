package com.github.zly2006.zhihu.ui.home.comment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.databinding.ItemCommentBinding
import com.github.zly2006.zhihu.loadImage
import io.ktor.client.*
import java.text.SimpleDateFormat

class CommentAdapter(
    private val values: List<DataHolder.Comment>,
    private val activity: FragmentActivity,
    private val httpClient: HttpClient,
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCommentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.like.text = "${item.likeCount}赞"
        holder.reply.text = "${item.childCommentCount}回复"
        holder.time.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(item.createdTime)
        holder.author.text = item.author.name
        if (item.author.avatarUrl.isNotEmpty()) {
            loadImage(holder, activity, httpClient, item.author.avatarUrl) {
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

    inner class ViewHolder(binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root),
        LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        val author = binding.author
        val avatar = binding.avatar
        val time = binding.time
        val like = binding.like
        val reply = binding.reply
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
