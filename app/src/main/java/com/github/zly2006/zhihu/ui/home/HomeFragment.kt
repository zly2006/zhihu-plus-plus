package com.github.zly2006.zhihu.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentHomeBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderItem
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class HomeFragment : Fragment() {
    val httpClient by lazy { AccountData.httpClient(requireContext()) }

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val list = mutableListOf<PlaceholderItem>()

    private var fetchingNewItems = false
    suspend fun fetch() {
        try {
            GlobalScope.launch {
                val response = httpClient.post("https://www.zhihu.com/lastread/touch") {
                    header("x-requested-with", "fetch")
                    setBody(MultiPartFormDataContent(
                        formData {
                            append("items", buildJsonArray {
                                list.filter { !it.touched && it.dto?.target?.type == "answer" }.forEach { item ->
                                    add(buildJsonArray {
                                        add("answer")
                                        add(item.dto!!.target.id)
                                        add("touch")
                                    })
                                }
                            }.toString())
                        }
                    ))
                }
                if (!response.status.isSuccess()) {
                    Log.e("Browse-Fetch", response.bodyAsText())
                }
            }
            val response = httpClient.get("https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&end_offset=${list.size}")
            if (response.status == HttpStatusCode.OK) {
                @Serializable
                class Response(val data: List<Feed>, val fresh_text: String, val paging: JsonObject)
                val text = response.bodyAsText()
                try {
                    val data = json.decodeFromString<Response>(text)
                    val index = list.size
                    list.addAll(data.data.filter {
                        (it.type != "feed_advert" && it.created_time != -1L &&
                                it.target.created_time != -1L && it.target.relationship != null)
                    }.map {
                        PlaceholderItem(
                            it.target.question!!.title,
                            it.target.excerpt,
                            "${it.target.voteup_count}赞 ${it.target.author.name} ${it.target.type}",
                            it
                        )
                    })
                    activity?.runOnUiThread {
                        _binding?.list?.adapter?.notifyItemRangeInserted(index, data.data.size)
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

    fun refresh() {
        val size = list.size
        list.clear()
        binding.list.adapter?.notifyItemRangeRemoved(0, size)
        GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher()) {
            repeat(3) {
                fetch()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val data = AccountData.getData(requireContext())
        if (!data.login) {
            val myIntent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(myIntent)
        }
        else {
            refresh()
        }

        binding.refreshList.setOnClickListener { refresh() }
        binding.list.adapter = HomeArticleItemAdapter(list, requireActivity())
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!binding.list.canScrollVertically(1000)) {
                    if (!fetchingNewItems) {
                        GlobalScope.launch {
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
