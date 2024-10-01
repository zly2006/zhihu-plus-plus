package com.github.zly2006.zhihu.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentHomeBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderItem
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

class HomeFragment : Fragment() {
    val httpClient by lazy { AccountData.httpClient(requireContext()) }

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val list = mutableListOf(
        PlaceholderItem(
            "#1",
            "Item 1",
            "This is item 1"
        ),
        PlaceholderItem(
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
                    "very very long. ",
            "This is item 2"
        ),
    ).apply {
        repeat(8) {
            add(
                PlaceholderItem(
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
            val response = httpClient.get("https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&end_offset=${list.size}")
            if (response.status == HttpStatusCode.OK) {
                @Serializable
                class Response(val data: List<Feed>, val fresh_text: String, val paging: JsonObject)
                val text = response.bodyAsText()
                try {
                    val data = json.decodeFromString<Response>(text)
                    val index = list.size
                    list.addAll(data.data.filter {
                        it.target.created_time != -1L && it.target.relationship != null
                    }.map {
                        PlaceholderItem(
                            it.target.question!!.title,
                            it.target.excerpt,
                            "${it.target.voteup_count}赞 ${it.target.author.name}",
                            it
                        )
                    })
                    activity?.runOnUiThread {
                        binding.list.adapter?.notifyItemRangeInserted(index, data.data.size)
                    }
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

        binding.list.adapter = HomeArticleItemAdapter(list, this)
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!binding.list.canScrollVertically(1000)) {
                    if (!fetchingNewItems) {
                        GlobalScope.launch {
                            println("fetch")
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
        val articleFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        if (articleFragment is ReadArticleFragment) {
            requireActivity().supportFragmentManager.beginTransaction()
                .remove(articleFragment)
                .commit()
        }
        _binding = null
    }
}
