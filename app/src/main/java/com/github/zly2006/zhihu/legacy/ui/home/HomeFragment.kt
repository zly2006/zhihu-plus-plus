package com.github.zly2006.zhihu.legacy.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.LegacyMainActivity
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentHomeBinding
import com.github.zly2006.zhihu.legacy.placeholder.PlaceholderItem
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

class HomeFragment : Fragment() {
    private val httpClient by lazy { AccountData.httpClient(requireContext()) }

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: LegacyMainActivity.MainActivityViewModel by activityViewModels()

    private var fetchingNewItems = false
    suspend fun fetch() {
        if (context == null) return
        try {
            httpClient.post("https://www.zhihu.com/lastread/touch") {
                header("x-requested-with", "fetch")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("items", buildJsonArray {
                                viewModel.list.filter { !it.touched && it.dto?.target is Feed.AnswerTarget }
                                    .forEach { item ->
                                        add(buildJsonArray {
                                            add("answer")
                                            add((item.dto!!.target as Feed.AnswerTarget).id)
                                            add("touch")
                                        })
                                    }
                            }.toString())
                        }
                    ))
            }.let { response ->
                if (!response.status.isSuccess()) {
                    Log.e("Browse-Touch", response.bodyAsText())
                }
            }
            val response = httpClient.get("https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&action=down&end_offset=${viewModel.list.size}") {
                signFetchRequest(context!!)
            }
            if (response.status == HttpStatusCode.OK) {
                @Suppress("PropertyName")
                @Serializable
                class Response(val data: List<Feed>, val fresh_text: String, val paging: JsonObject)
                val text = response.body<JsonObject>()
                try {
                    "${text["data"]!!.jsonArray.size} " + text["data"]!!.jsonArray.count { it.jsonObject["target"]!!.jsonObject["type"]!!.jsonPrimitive.content == "zvideo" }
                    val data = AccountData.decodeJson<Response>(text)
                    val index = viewModel.list.size
                    viewModel.list.addAll(data.data
                        .filter {
                            it.target !is Feed.AdvertTarget
                    }.map {
                        if (it.target.filterReason() != null) {
                            return@map PlaceholderItem(
                                "已屏蔽",
                                it.target.filterReason()!!,
                                it.target.detailsText(),
                                it
                            )
                        }
                        if (it.target is Feed.AnswerTarget) {
                            PlaceholderItem(
                                it.target.question.title,
                                it.target.excerpt,
                                it.target.detailsText(),
                                it
                            )
                        }
                        else if (it.target is Feed.ArticleTarget) {
                            PlaceholderItem(
                                it.target.title,
                                it.target.excerpt,
                                it.target.detailsText(),
                                it
                            )
                        }
                        else {
                            PlaceholderItem(
                                it.target.javaClass.simpleName,
                                "Not Implemented",
                                it.target.detailsText(),
                            )
                        }
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
            viewModel.toast.postValue("Failed to fetch recommends, ${e.message}")
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
