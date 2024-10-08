package com.github.zly2006.zhihu.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.navigate
import com.github.zly2006.zhihu.databinding.FragmentHomeArticleItemBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderItem

class HomeArticleItemAdapter(
    private val values: List<PlaceholderItem>,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<HomeArticleItemAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentHomeArticleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.title.text = item.title
        holder.summary.text = item.summary
        holder.details.text = item.details
        holder.card.setOnClickListener {
            if (item.dto == null) {
                AlertDialog.Builder(activity).apply {
                    setTitle("Click " + item.title)
                    setMessage(item.summary)
                    setPositiveButton("OK") { _, _ ->
                    }
                }.create().show()
            } else {
                activity.navigate(Article(
                    item.title,
                    item.dto.target.type,
                    item.dto.target.id,
                    item.dto.target.author.name,
                    item.dto.target.author.headline,
                    item.dto.target.content,
                    item.dto.target.author.avatar_url,
                ))
            }
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentHomeArticleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val title: TextView = binding.title
        val summary: TextView = binding.summary
        val details: TextView = binding.details
        val card = binding.card
    }
}
