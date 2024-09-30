package com.github.zly2006.zhihu.ui.home.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.databinding.FragmentHomeArticleItemBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderItem
import com.github.zly2006.zhihu.ui.home.HomeFragment
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup

/**
 * [RecyclerView.Adapter] that can display a [PlaceholderItem].
 * TODO: Replace the implementation with code for your data type.
 */
class HomeArticleItemAdapter(
    private val values: List<PlaceholderItem>,
    private val fragment: HomeFragment
) : RecyclerView.Adapter<HomeArticleItemAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentHomeArticleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.title.text = item.title
        holder.summary.text = item.summary
        holder.details.text = item.details
        holder.itemView.setOnClickListener {
            if (item.dto == null) {
                AlertDialog.Builder(fragment.requireContext()).apply {
                    setTitle("Click " + item.title)
                    setMessage(item.summary)
                    setPositiveButton("OK") { _, _ ->
                    }
                }.create().show()
            } else {
                runBlocking {
                    val html = fragment.httpClient.get("https://www.zhihu.com/question/${item.dto.target.question.id}/answer/${item.dto.target.id}").bodyAsText()
                    val document = Jsoup.parse(html)
                    val answer = document.select(".ztext.RichText").first()
                    println(answer)
                }
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
