package com.github.zly2006.zhihu.ui.home

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.MainActivity.MainActivityViewModel
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.navigate
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.postHistory
import com.github.zly2006.zhihu.databinding.FragmentReadArticleBinding
import com.github.zly2006.zhihu.loadImage
import com.github.zly2006.zhihu.ui.home.ReadArticleFragment.ReadArticleViewModel.VoteUpState.*
import com.github.zly2006.zhihu.ui.home.comment.CommentsDialog
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.properties.Delegates

/**
 * answer | article
 */
private const val ARG_ARTICLE_TYPE = "article_type_1"
private const val ARG_ARTICLE_ID = "id"
private const val ARG_TITLE = "title"
private const val ARG_AUTHOR_NAME = "authorName"
private const val ARG_BIO = "authorBio"
private const val ARG_QUESTION_ID = "questionId"
private const val ARG_AVATAR_SRC = "avatarSrc"
private const val ARG_VOTE_UP_COUNT = "voteUpCount"

@Serializable
data class Reaction(
    val reaction_count: Int,
    val reaction_state: Boolean,
    val reaction_value: String,
    val success: Boolean,
    val is_thanked: Boolean,
    val thanks_count: Int,
    val red_heart_count: Int,
    val red_heart_has_set: Boolean,
    val is_liked: Boolean,
    val liked_count: Int,
    val is_up: Boolean,
    val voteup_count: Int,
    val is_upped: Boolean,
    val up_count: Int,
    val is_down: Boolean,
    val voting: Int,
    val heavy_up_result: String,
    val is_auto_send_moments: Boolean
)

class ReadArticleFragment : Fragment() {
    private lateinit var document: Document
    private lateinit var type: String
    private var articleId by Delegates.notNull<Long>()
    private var _binding: FragmentReadArticleBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: ReadArticleViewModel by viewModels()

    class ReadArticleViewModel : ViewModel() {
        enum class VoteUpState(val key: String) {
            Up("up"),
            Down("down"),
            Neutral("neutral"),
        }
        val title = MutableLiveData<String>()
        val authorName = MutableLiveData<String>()
        val bio = MutableLiveData<String>()
        val content = MutableLiveData<String>()
        val questionId = MutableLiveData<Long>()
        val avatarSrc = MutableLiveData<String>()
        val votedUp = MutableLiveData(Neutral)
        val voteUpCount = MutableLiveData(0)
        val commentCount = MutableLiveData(0)
    }

    val gViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            this.type = it.getString(ARG_ARTICLE_TYPE) ?: "answer"
            viewModel.title.value = it.getString(ARG_TITLE) ?: "title"
            viewModel.authorName.value = it.getString(ARG_AUTHOR_NAME) ?: "authorName"
            viewModel.bio.value = it.getString(ARG_BIO) ?: "bio"
            viewModel.questionId.value = it.getLong(ARG_QUESTION_ID)
            viewModel.avatarSrc.value = it.getString(ARG_AVATAR_SRC) ?: ""
            viewModel.voteUpCount.value = it.getInt(ARG_VOTE_UP_COUNT)
            articleId = it.getLong(ARG_ARTICLE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadArticleBinding.inflate(inflater, container, false)

        val root: View = binding.root

        val httpClient = AccountData.httpClient(requireContext())
        registerForContextMenu(binding.web)
        setupUpWebview(binding.web, requireContext())
        binding.web.setOnLongClickListener { view ->
            val result = (view as WebView).hitTestResult
            if (result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                view.showContextMenu()
                true
            } else {
                false
            }
        }
        binding.copyLink.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Link",
                "https://www.zhihu.com/question/${viewModel.questionId.value}/answer/${articleId}"
                        + "\n【${viewModel.title.value} - ${viewModel.authorName.value}的回答】"
            )
            clipboard.setPrimaryClip(clip)
            gViewModel.toast.postValue("Link copied")
        }
        binding.voteUp.setOnClickListener {
            val value = when (viewModel.votedUp.value) {
                null -> Up
                Up -> Neutral
                Neutral -> Up
                Down -> Up
            }
            viewModel.votedUp.value = value
            launch {
                if (type == "answer") {
                    val reaction = httpClient.post("https://www.zhihu.com/api/v4/answers/${articleId}/voters") {
                        setBody(
                            mapOf(
                                "type" to value.key
                            )
                        )
                        contentType(ContentType.Application.Json)
                    }.body<Reaction>()
                    viewModel.voteUpCount.postValue(reaction.voteup_count)
                }
            }
        }
        binding.openComments.setOnClickListener {
            if (viewModel.commentCount.value == 0) {
                Toast.makeText(requireContext(), "还没有评论", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val frag = CommentsDialog(
                httpClient, Article(
                    viewModel.title.value ?: "title",
                    type,
                    articleId,
                    viewModel.authorName.value ?: "authorName",
                    viewModel.bio.value ?: "bio",
                    viewModel.avatarSrc.value,
                    null
                )
            )
            frag.show(parentFragmentManager, "comments")
        }

        viewModel.title.distinctUntilChanged().observe(viewLifecycleOwner) { binding.title.text = it }
        viewModel.authorName.distinctUntilChanged().observe(viewLifecycleOwner) { binding.author.text = it }
        viewModel.bio.distinctUntilChanged().observe(viewLifecycleOwner) { binding.bio.text = it }
        viewModel.questionId.distinctUntilChanged().observe(viewLifecycleOwner) { questionId ->
            if (questionId != 0L) {
                binding.title.setOnClickListener {
                    requireActivity().navigate(
                        Question(
                            questionId, viewModel.title.value ?: "Question"
                        )
                    )
                }
            } else {
                binding.title.setOnClickListener(null)
            }
        }
        viewModel.content.distinctUntilChanged().observe(viewLifecycleOwner) { html ->
            if (DataHolder.definitelyAd.any { keyword -> keyword in html }) {
                Log.i("ReadArticleFragment", "Answer is an ad")
                binding.web.loadData("<h1>广告</h1><p>这个回答被识别为广告，已被隐藏。</p>", "text/html", "utf-8")
            }
            document = Jsoup.parse(html)
            document.select("img.lazy").forEach { it.remove() }
            binding.web.loadDataWithBaseURL(
                "https://www.zhihu.com/question/${viewModel.questionId.value}/answer/${articleId}", """
                    <head>
                    <link rel="stylesheet" href="//zhihu-plus.internal/assets/stylesheet.css">
                    <viewport content="width=device-width, initial-scale=1.0">
                    </head>
                """.trimIndent() + document.toString(), "text/html", "utf-8", null
            )
        }
        viewModel.avatarSrc.distinctUntilChanged().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                loadImage(viewLifecycleOwner, requireActivity(), httpClient, it, binding.avatar::setImageBitmap)
            }
        }
        val voteUp = Observer<Any> {
            @SuppressLint("SetTextI18n")
            binding.voteUp.text = "${viewModel.voteUpCount.value} " + if (viewModel.votedUp.value == Up) "已赞" else "赞同"
        }
        viewModel.voteUpCount.distinctUntilChanged().observe(viewLifecycleOwner, voteUp)
        viewModel.votedUp.observe(viewLifecycleOwner) {
            if (it == Neutral) {
                binding.voteUp.setBackgroundColor(0xFF29B6F6.toInt())
            } else if (it == Up) {
                binding.voteUp.setBackgroundColor(0xFF0D47A1.toInt())
            }
            voteUp.onChanged(it)
        }
        viewModel.commentCount.observe(viewLifecycleOwner) {
            binding.openComments.text = it.toString()
        }

        launch {
            if (activity == null) return@launch
            if (type == "answer") {
                DataHolder.getAnswerCallback(requireActivity(), httpClient, articleId) { answer ->
                    if (answer != null) {
                        if (viewModel.content.value.isNullOrEmpty()) {
                            viewModel.content.postValue(answer.content)
                        }
                        viewModel.questionId.postValue(answer.question.id)
                        viewModel.title.postValue(answer.question.title)
                        viewModel.authorName.postValue(answer.author.name)
                        viewModel.bio.postValue(answer.author.headline)
                        viewModel.avatarSrc.postValue(answer.author.avatarUrl)
                        viewModel.voteUpCount.postValue(answer.voteupCount)
                        viewModel.commentCount.postValue(answer.commentCount)
                        activity?.postHistory(
                            Article(
                                answer.question.title,
                                "answer",
                                articleId,
                                answer.author.name,
                                answer.author.headline,
                                answer.author.avatarUrl,
                                answer.excerpt
                            )
                        )
                    } else {
                        if (viewModel.content.value.isNullOrEmpty()) {
                            viewModel.content.postValue("<h1>Answer not found</p>")
                        }
                        Log.e("ReadArticleFragment", "Answer not found")
                    }
                }
            } else if (type == "article") {
                DataHolder.getArticleCallback(requireActivity(), httpClient, articleId) { article ->
                    if (article != null) {
                        if (viewModel.content.value.isNullOrEmpty()) {
                            viewModel.content.postValue(article.content)
                        }
                        viewModel.title.postValue(article.title)
                        viewModel.authorName.postValue(article.author.name)
                        viewModel.bio.postValue(article.author.headline)
                        viewModel.avatarSrc.postValue(article.author.avatarUrl)
                        viewModel.voteUpCount.postValue(article.voteupCount)
                        viewModel.commentCount.postValue(article.commentCount)
                        activity?.postHistory(
                            Article(
                                article.title,
                                "article",
                                articleId,
                                article.author.name,
                                article.author.headline,
                                article.author.avatarUrl,
                                article.excerpt
                            )
                        )
                    } else {
                        if (viewModel.content.value.isNullOrEmpty()) {
                            viewModel.content.postValue("<h1>Article not found</p>")
                        }
                        Log.e("ReadArticleFragment", "Article not found")
                    }
                }
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v == binding.web) {
            val result = binding.web.hitTestResult
            if (result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                val imgElement = document.select("img[src='${result.extra}']").first()
                val dataOriginalUrl = imgElement?.attr("data-original")
                menu.setHeaderTitle("Image Options")
                menu.add(0, v.id, 0, "Save Image").setOnMenuItemClickListener {
                    saveImage(dataOriginalUrl ?: result.extra)
                    true
                }
                menu.add(0, v.id, 1, "View Image").setOnMenuItemClickListener {
                    viewImage(dataOriginalUrl ?: result.extra)
                    true
                }
            }
        }
    }

    private fun saveImage(imageUrl: String?) {
        if (imageUrl == null) {
            gViewModel.toast.postValue("Image URL is null")
            return
        }

        launch {
            try {
                val httpClient = AccountData.httpClient(requireContext())
                val response: HttpResponse = httpClient.get(imageUrl)
                val bytes = response.readBytes()

                val fileName = Uri.parse(imageUrl).lastPathSegment ?: "downloaded_image"
                val file =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).resolve(fileName)

                file.outputStream().use { it.write(bytes) }

                gViewModel.toast.postValue("Image saved: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("ReadArticleFragment", "Failed to save image", e)
                gViewModel.toast.postValue("Failed to save image: ${e.message}")
            }
        }
    }

    private fun viewImage(imageUrl: String?) {
        if (imageUrl != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(imageUrl)
            context?.startActivity(intent)
        }
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher(), block = block)
    }
}

fun setupUpWebview(web: WebView, context: Context) {
    val assetLoader = WebViewAssetLoader.Builder()
        .setDomain("zhihu-plus.internal")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    web.webViewClient = object : WebViewClientCompat() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(request.url)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (request.url.host == "link.zhihu.com") {
                val url =
                    request.url.query?.split("&")?.firstOrNull { it.startsWith("target=") }?.substringAfter("target=")
                if (url != null) {
                    val intent: CustomTabsIntent = CustomTabsIntent.Builder()
                        .build()
                    intent.launchUrl(context, Uri.parse(url))
                    return true
                }
            }
            else if (request.url.host == "www.zhihu.com" && context is MainActivity) {
                val destination = context.resolveContent(request.url)
                if (destination != null) {
                    context.navigate(destination)
                    return true
                }
            }
            return super.shouldOverrideUrlLoading(view, request)
        }
    }
}
