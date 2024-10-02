package com.github.zly2006.zhihu.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentReadArticleBinding
import com.github.zly2006.zhihu.ui.home.question.QuestionDetailsFragment
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
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
private const val ARG_BIO = "bio"
private const val ARG_CONTENT = "content"
private const val ARG_QUESTION_ID = "questionId"
private const val ARG_AVATAR_SRC = "avatarSrc"

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
        val title = MutableLiveData<String>()
        val authorName = MutableLiveData<String>()
        val bio = MutableLiveData<String>()
        val content = MutableLiveData<String>()
        val questionId = MutableLiveData<Long>()
        val avatarSrc = MutableLiveData<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val type = it.getString(ARG_ARTICLE_TYPE) ?: "answer"
            val id = it.getLong(ARG_ARTICLE_ID)
            val title = it.getString(ARG_TITLE) ?: "title"
            val authorName = it.getString(ARG_AUTHOR_NAME) ?: "authorName"
            val bio = it.getString(ARG_BIO) ?: "bio"
            val content = it.getString(ARG_CONTENT) ?: "content"
            val questionId = it.getLong(ARG_QUESTION_ID)
            val avatarSrc = it.getString(ARG_AVATAR_SRC) ?: ""
            viewModel.title.value = title
            viewModel.authorName.value = authorName
            viewModel.bio.value = bio
            viewModel.content.value = content
            viewModel.questionId.value = questionId
            viewModel.avatarSrc.value = avatarSrc
            articleId = id
            this.type = type
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

        viewModel.title.distinctUntilChanged().observe(viewLifecycleOwner) { binding.title.text = it }
        viewModel.authorName.distinctUntilChanged().observe(viewLifecycleOwner) { binding.author.text = it }
        viewModel.bio.distinctUntilChanged().observe(viewLifecycleOwner) { binding.bio.text = it }
        viewModel.questionId.distinctUntilChanged().observe(viewLifecycleOwner) { questionId ->
            if (questionId != 0L) {
                binding.title.setOnClickListener {
                    requireActivity().supportFragmentManager.commit {
                        replace(
                            R.id.nav_host_fragment_activity_main,
                            QuestionDetailsFragment.newInstance(questionId, viewModel.title.value ?: "Loading...")
                        )
                        addToBackStack("Question-Details")
                    }
                }
            } else {
                binding.title.setOnClickListener(null)
            }
        }
        viewModel.content.distinctUntilChanged().observe(viewLifecycleOwner) {
            document = Jsoup.parse(it)
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
                GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher()) {
                    httpClient.get(it).bodyAsChannel().toInputStream().buffered().use {
                        val bitmap = BitmapFactory.decodeStream(it)
                        binding.avatar.setImageBitmap(bitmap)
                    }
                }
            }
        }

        GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher()) {
            if (type == "answer") {
                val answer = DataHolder.getAnswer(httpClient, articleId)?.value
                viewModel.questionId.postValue(answer?.question?.id ?: 0)
                viewModel.title.postValue(answer?.question?.title ?: "title")
                viewModel.authorName.postValue(answer?.author?.name ?: "authorName")
                viewModel.bio.postValue(answer?.author?.headline ?: "author bio")
                if (answer == null) {
                    Log.e("ReadArticleFragment", "Answer not found")
                    return@launch
                }
                viewModel.avatarSrc.postValue(answer.author.avatarUrl)
                if (DataHolder.definitelyAd.any { it in answer.content }) {
                    Log.i("ReadArticleFragment", "Answer is an ad")
                    viewModel.content.postValue("<h1>广告</h1><p>这个回答被识别为广告，已被隐藏。</p>")
                    return@launch
                }
                viewModel.content.postValue(answer.content)
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

        GlobalScope.launch {
            try {
                val httpClient = AccountData.httpClient(requireContext())
                val response: HttpResponse = httpClient.get(imageUrl)
                val bytes = response.readBytes()

                val fileName = Uri.parse(imageUrl).lastPathSegment ?: "downloaded_image"
                val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).resolve(fileName)

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

    companion object {
        @JvmStatic
        fun newInstance(feed: Feed) =
            ReadArticleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ARTICLE_TYPE, feed.target.type)
                    putLong(ARG_ARTICLE_ID, feed.target.id)
                    if (feed.target.type == "answer") {
                        putString(ARG_TITLE, feed.target.question!!.title)
                    }
                    putString(ARG_AUTHOR_NAME, feed.target.author.name)
                    putString(ARG_BIO, feed.target.author.headline)
                    putString(ARG_CONTENT, feed.target.content)
                    putLong(ARG_QUESTION_ID, feed.target.question?.id ?: 0)
                    putString(ARG_AVATAR_SRC, feed.target.author.avatar_url)
                }
            }
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
    }
    if (VERSION.SDK_INT > VERSION_CODES.Q) {
        web.isForceDarkAllowed = true
        runCatching {
            // Android 13+
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(web.settings, true)
        }
    }
}
