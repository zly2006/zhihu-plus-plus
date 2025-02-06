package com.github.zly2006.zhihu.ui.home.comment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.databinding.ItemCommentBinding
import com.github.zly2006.zhihu.loadImage
import io.ktor.client.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jsoup.Jsoup
import java.text.SimpleDateFormat

private val HMS = SimpleDateFormat("HH:mm:ss")
private val MDHMS = SimpleDateFormat("MM-dd HH:mm:ss")
private val YMDHMS = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

class CommentAdapter(
    private val values: List<CommentItem>,
    private val activity: FragmentActivity,
    private val httpClient: HttpClient,
    private val navDestination: NavDestination,
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
        val html = Jsoup.parse(item.item.content)
        holder.like.text = "${item.item.likeCount} 赞"
        holder.reply.text = "${item.item.childCommentCount} 回复"
        holder.reply.isInvisible = navDestination is CommentHolder
        holder.content.text = html.text()
        val instant = Instant.fromEpochSeconds(item.item.createdTime)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val nowLocalDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        if (localDateTime.date == nowLocalDateTime.date) {
            holder.time.text = HMS.format(item.item.createdTime * 1000)
        } else if (localDateTime.year == nowLocalDateTime.year) {
            holder.time.text = MDHMS.format(item.item.createdTime * 1000)
        } else {
            holder.time.text = YMDHMS.format(item.item.createdTime * 1000)
        }
        holder.author.text = item.item.author.user.name
        if (item.item.author.user.avatarUrl.isNotEmpty()) {
            loadImage(holder, activity, httpClient, item.item.author.user.avatarUrl) {
                holder.avatar.setImageBitmap(it)
            }
        }

        if (item.clickTarget != null) {
            holder.card.setOnClickListener {
                val frag = CommentsDialog(
                    httpClient, item.clickTarget
                )
                frag.show(activity.supportFragmentManager, "comments_holder")
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
        val content = binding.content
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
