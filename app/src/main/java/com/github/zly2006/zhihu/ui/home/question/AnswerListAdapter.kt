package com.github.zly2006.zhihu.ui.home.question

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentQuestionDetailsAnswerBinding
import com.github.zly2006.zhihu.ui.home.ReadArticleFragment

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
