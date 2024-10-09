package com.github.zly2006.zhihu.ui.home

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.webkit.*
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.navigate
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.postHistory
import com.github.zly2006.zhihu.databinding.FragmentReadArticleBinding
import com.github.zly2006.zhihu.loadImage
import com.github.zly2006.zhihu.ui.home.ReadArticleFragment.ReadArticleViewModel.VoteUpState.*
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
private const val ARG_ARTICLE_TYPE = "type"
private const val ARG_ARTICLE_ID = "id"
private const val ARG_TITLE = "title"
private const val ARG_AUTHOR_NAME = "authorName"
private const val ARG_BIO = "authorBio"
private const val ARG_CONTENT = "content"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            this.type = it.getString(ARG_ARTICLE_TYPE) ?: "answer"
            viewModel.title.value = it.getString(ARG_TITLE) ?: "title"
            viewModel.authorName.value = it.getString(ARG_AUTHOR_NAME) ?: "authorName"
            viewModel.bio.value = it.getString(ARG_BIO) ?: "bio"
            viewModel.content.value = it.getString(ARG_CONTENT) ?: ""
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
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(binding.web.settings, true);
        }
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
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
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
                val reaction = httpClient.post("https://www.zhihu.com/api/v4/answers/3077409061/voters") {
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
            binding.voteUp.text = "${viewModel.voteUpCount.value} " + if (viewModel.votedUp.value == Up) "已赞同" else "赞同"
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

        launch {
            if (type == "answer") {
                val answer = activity?.let { DataHolder.getAnswer(it, httpClient, articleId) }?.value
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
                    if (activity is MainActivity) {
                        requireActivity().postHistory(
                            Article(
                                answer.question.title,
                                "answer",
                                articleId,
                                answer.author.name,
                                answer.author.headline,
                                answer.content,
                                answer.author.avatarUrl,
                                answer.excerpt
                            )
                        )
                    }
                } else {
                    if (viewModel.content.value.isNullOrEmpty()) {
                        viewModel.content.postValue("<h1>Answer not found</p>")
                    }
                    Log.e("ReadArticleFragment", "Answer not found")
                    return@launch
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
            Toast.makeText(context, "Image URL is null", Toast.LENGTH_SHORT).show()
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

                requireActivity().runOnUiThread {
                    Toast.makeText(context, "Image saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun viewImage(imageUrl: String?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(imageUrl)
        context?.startActivity(intent)
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher(), block = block)
    }
}

class WebViewListener {
    fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
    }

    fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
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
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                    return true
                }
            }
            return super.shouldOverrideUrlLoading(view, request)
        }
    }
    if (VERSION.SDK_INT > VERSION_CODES.Q) {
        web.isForceDarkAllowed = true
        runCatching {
            // Android 13+
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(web.settings, true)
        }
    }
}
