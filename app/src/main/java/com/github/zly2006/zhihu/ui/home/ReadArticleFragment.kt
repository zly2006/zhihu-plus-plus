package com.github.zly2006.zhihu.ui.home

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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.databinding.FragmentReadArticleBinding
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ReadArticleFragment : Fragment() {
    /**
     * answer | article
     */
    private val ARG_TYPE = "type"
    private val ARG_ID = "id"
    private lateinit var dto: Feed
    private lateinit var document: Document
    private var _binding: FragmentReadArticleBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadArticleBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val httpClient = AccountData.httpClient(requireContext())

        binding.title.text = dto.target.question?.title
        binding.author.text = dto.target.author.name
        binding.bio.text = dto.target.author.headline
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("zhihu-plus.internal")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireActivity()))
            .build()
        binding.web.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }
        registerForContextMenu(binding.web)
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

        GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher()) {
            if (dto.target.type == "answer") {
                binding.title.text = dto.target.question!!.title
                val answer = DataHolder.getAnswer(httpClient, dto.target.id)?.value
                if (answer == null) {
                    Log.e("ReadArticleFragment", "Answer not found")
                    return@launch
                }
                val avatarSrc = answer.author.avatarUrl
                if (avatarSrc != null) {
                    launch {
                        httpClient.get(avatarSrc).bodyAsChannel().toInputStream().buffered().use {
                            val bitmap = BitmapFactory.decodeStream(it)
                            requireActivity().runOnUiThread {
                                binding.avatar.setImageBitmap(bitmap)
                            }
                        }
                    }
                }

                binding.author.text = answer.author.name
                binding.bio.text = answer.author.headline
                setupUpDarkMode(binding.web)
                document = Jsoup.parse(answer.content)
                document.select("img.lazy").forEach { it.remove() }
                binding.web.loadDataWithBaseURL(
                    "https://www.zhihu.com/question/${answer.question.id}/answer/${answer.id}", """
                    <head>
                    <link rel="stylesheet" href="//zhihu-plus.internal/assets/stylesheet.css">
                    <viewport content="width=device-width, initial-scale=1.0">
                    </head>
                """.trimIndent() + document.toString(), "text/html", "utf-8", null
                )
            }
            if (dto.target.question != null) {

                binding.title.setOnClickListener {
                    requireActivity().supportFragmentManager.commit {
                        replace(
                            R.id.nav_host_fragment_activity_main,
                            QuestionDetailsFragment.newInstance(dto.target.question!!.id)
                        )
                        addToBackStack("Question-Details")
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
                dto = feed
            }
    }
}

fun setupUpDarkMode(web: WebView) {
    if (VERSION.SDK_INT > VERSION_CODES.Q) {
        web.isForceDarkAllowed = true
        runCatching {
            // Android 13+
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(web.settings, true)
        }
    }
}
