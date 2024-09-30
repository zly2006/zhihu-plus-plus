package com.github.zly2006.zhihu.ui.home.browse

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

import com.github.zly2006.zhihu.placeholder.PlaceholderContent.PlaceholderItem
import com.github.zly2006.zhihu.databinding.FragmentHomeArticleItemBinding

/**
 * [RecyclerView.Adapter] that can display a [PlaceholderItem].
 * TODO: Replace the implementation with code for your data type.
 */
class MyHomeArticleItemRecyclerViewAdapter(
    private val values: List<PlaceholderItem>,
    private val context: Context?
) : RecyclerView.Adapter<MyHomeArticleItemRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentHomeArticleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.title.text = item.title
        holder.summary.text = item.summary
        holder.details.text = item.details
        holder.itemView.setOnClickListener {
            if (context != null) {
                AlertDialog.Builder(context).apply {
                    setTitle("Click " + item.title)
                    setMessage(item.summary)
                    setPositiveButton("OK") { _, _ ->
                    }
                }.create().show()
            }
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentHomeArticleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val title: TextView = binding.title
        val summary: TextView = binding.summary
        val details: TextView = binding.details
    }

}
