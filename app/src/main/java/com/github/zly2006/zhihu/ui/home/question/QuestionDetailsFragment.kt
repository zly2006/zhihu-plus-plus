package com.github.zly2006.zhihu.ui.home.question

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentQuestionDetailsBinding
import com.github.zly2006.zhihu.ui.home.setupUpWebview
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private const val ARG_QUESTION_ID = "q-id"
private const val ARG_QUESTION_TITLE = "q-title"

class QuestionDetailsFragment : Fragment() {
    private val httpClient by lazy { AccountData.httpClient(requireContext()) }
    private var fetchingNewItems = false
    private var canFetchMore = true
    private var questionId: Long = 0
    private lateinit var document: Document
    private var session = ""
    private var cursor = ""

    private var _binding: FragmentQuestionDetailsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    val answers = mutableListOf<Feed>()
    val viewModel: QuestionViewModel by viewModels()

    suspend fun fetch() {
        fetchingNewItems = true
        val response =
            httpClient.get("https://www.zhihu.com/api/v4/questions/${questionId}/feeds?session_id=${session}&cursor=${cursor}")
        val jojo = response.body<JsonObject>()
        val feeds = json.decodeFromJsonElement<List<Feed>>(jojo["data"]!!)
        cursor = feeds.last().cursor
        session = jojo["session"]!!.jsonObject["id"]!!.jsonPrimitive.content
        canFetchMore = !jojo["paging"]!!.jsonObject["is_end"]!!.jsonPrimitive.boolean
        activity?.runOnUiThread {
            val start = answers.size
            answers.addAll(feeds)
            _binding?.answers?.adapter?.notifyItemRangeInserted(start, feeds.size)
        }
        fetchingNewItems = false
        if (canFetchMore && !binding.scroll.canScrollVertically(binding.scroll.height)) {
            fetch()
        }
    }

    class QuestionViewModel : ViewModel() {
        val title = MutableLiveData<String>()
        val detail = MutableLiveData<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionId = requireArguments().getLong(ARG_QUESTION_ID)
        viewModel.title.postValue(requireArguments().getString(ARG_QUESTION_TITLE))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionDetailsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        GlobalScope.launch {
            fetch()
        }
        setupUpWebview(binding.webview, requireContext())

        viewModel.title.distinctUntilChanged().observe(viewLifecycleOwner) { binding.title.text = it }
        viewModel.detail.distinctUntilChanged().observe(viewLifecycleOwner) {
            document = Jsoup.parse(it)
            document.select("img.lazy").forEach { it.remove() }
            document.select("img").forEach {
                it.removeAttr("width")
            }

            binding.webview.loadDataWithBaseURL(
                "https://www.zhihu.com/question/${questionId}",
                """
                    <head>
                    <link rel="stylesheet" href="//zhihu-plus.internal/assets/stylesheet.css">
                    <viewport content="width=device-width, initial-scale=1.0">
                    </head>
                """.trimIndent() + document.toString(), "text/html", "utf-8", null
            )
        }
        binding.answers.adapter = AnswerListAdapter(answers, requireActivity())
        binding.scroll.setOnScrollChangeListener { view, i, i2, i3, i4 ->
            if (!canFetchMore || fetchingNewItems) return@setOnScrollChangeListener
            if (!binding.scroll.canScrollVertically(binding.scroll.height)) {
                GlobalScope.launch { fetch() }
            }
        }

        val httpClient = AccountData.httpClient(requireContext())
        GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher()) {
            val question = DataHolder.getQuestion(httpClient, questionId)?.value
            if (question == null) {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Error")
                    setMessage("Failed to load question details $questionId")
                    setPositiveButton("OK") { _, _ ->
                    }
                }.create().show()
                return@launch
            }
            viewModel.title.postValue(question.title)
            viewModel.detail.postValue(question.detail)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(questionId: Long, title: String) =
            QuestionDetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_QUESTION_ID, questionId)
                    putString(ARG_QUESTION_TITLE, title)
                }
            }
    }
}
