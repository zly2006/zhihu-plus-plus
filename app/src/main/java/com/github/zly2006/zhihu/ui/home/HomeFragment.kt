package com.github.zly2006.zhihu.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentHomeBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderContent
import com.github.zly2006.zhihu.ui.home.browse.MyHomeArticleItemRecyclerViewAdapter
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.math.max

class HomeFragment : Fragment() {
    private val httpClient by lazy { AccountData.httpClient(requireContext()) }

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val list = mutableListOf(
        PlaceholderContent.PlaceholderItem(
            "#1",
            "Item 1",
            "This is item 1"
        ),
        PlaceholderContent.PlaceholderItem(
            "#2",
            "Item 2 long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long ",
            "This is item 2, very very" +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. "
        ),
    ).apply {
        repeat(8) {
            add(
                PlaceholderContent.PlaceholderItem(
                    "#${it + 3}",
                    "Item ${it + 3}",
                    "This is item ${it + 3}"
                )
            )
        }
    }

    private var fetchingNewItems = false
    suspend fun fetch() {
        try {
            fetchingNewItems = true
            val response = httpClient.get("https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true")
            if (response.status == HttpStatusCode.OK) {
                @Serializable
                class Response(val data: List<Feed>, val fresh_text: String, val paging: JsonObject)
                val text = response.bodyAsText()
                try {
                    val data = Json.decodeFromString<Response>(text)
                    val index = list.size
                    list.addAll(data.data.map {
                        PlaceholderContent.PlaceholderItem(
                            it.target.question.title,
                            it.target.excerpt,
                            "${it.target.voteup_count}赞 ${it.target.author.name}"
                        )
                    })
                    binding.list.adapter?.notifyItemRangeInserted(index, data.data.size)
                } catch (e: SerializationException) {
                    Log.e("HomeFragment", "Failed to parse JSON: $text", e)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fetchingNewItems = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.list.adapter = MyHomeArticleItemRecyclerViewAdapter(list, context)
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastChild = binding.list.getChildAt(max(0, binding.list.childCount - 5))
                if (lastChild.bottom <= binding.list.height) {
                    println("fetch")
                    GlobalScope.launch {
                        if (!fetchingNewItems) {
                            fetch()
                        }
                    }
                }
            }
        })

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
