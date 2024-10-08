package com.github.zly2006.zhihu.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentHomeBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderItem
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class HomeFragment : Fragment() {
    private val httpClient by lazy { AccountData.httpClient(requireContext()) }

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: MainActivity.MainActivityViewModel by activityViewModels()

    private var fetchingNewItems = false
    suspend fun fetch() {
        if (context == null) return
        try {
            coroutineScope {
                launch {
                    val response = httpClient.post("https://www.zhihu.com/lastread/touch") {
                        header("x-requested-with", "fetch")
                        setBody(MultiPartFormDataContent(
                            formData {
                                append("items", buildJsonArray {
                                    viewModel.list.filter { !it.touched && it.dto?.target?.type == "answer" }.forEach { item ->
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
            }
            val response = httpClient.get("https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&action=down&end_offset=${viewModel.list.size}")
            if (response.status == HttpStatusCode.OK) {
                @Suppress("PropertyName")
                @Serializable
                class Response(val data: List<Feed>, val fresh_text: String, val paging: JsonObject)
                val text = response.bodyAsText()
                try {
                    val data = json.decodeFromString<Response>(text)
                    val index = viewModel.list.size
                    viewModel.list.addAll(data.data.filter {
                        (it.type != "feed_advert" && it.created_time != -1L &&
                                it.target.created_time != -1L && it.target.relationship != null)
                    }.map {
                        PlaceholderItem(
                            it.target.question!!.title,
                            it.target.excerpt,
                            "${it.target.voteup_count}赞 ${it.target.author.name}",
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
            Log.e("HomeFragment", "Failed to fetch", e)
            Toast.makeText(requireContext(), "Failed to fetch recommends", Toast.LENGTH_LONG).show()
        } finally {
            fetchingNewItems = false
        }
    }

    private fun refresh() {
        val size = viewModel.list.size
        viewModel.list.clear()
        binding.list.adapter?.notifyItemRangeRemoved(0, size)
        launch {
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

        val preferences = requireActivity().getSharedPreferences(
            "com.github.zly2006.zhihu_preferences",
            MODE_PRIVATE
        )
        val developer = preferences.getBoolean("developer", false)
        if (!developer) {
            AlertDialog.Builder(requireActivity()).apply {
                setTitle("登录失败")
                setMessage("您当前的IP不在校园内，禁止使用！本应用仅供学习使用，使用责任由您自行承担。")
                setPositiveButton("OK") { _, _ ->
                }
            }.create().show()
            return root
        }
        val data = AccountData.getData(requireContext())
        if (!data.login) {
            val myIntent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(myIntent)
        }
        else {
            launch {
                repeat(3) {
                    fetch()
                }
            }
        }

        binding.refreshList.setOnClickListener { refresh() }
        binding.list.adapter = HomeArticleItemAdapter(viewModel.list, requireActivity())
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!binding.list.canScrollVertically(binding.list.height)) {
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
        _binding = null
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher(), block = block)
    }
}
