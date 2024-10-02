package com.github.zly2006.zhihu.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.databinding.FragmentQuestionDetailsBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private const val ARG_QUESTION_ID = "q-id"

class QuestionDetailsFragment : Fragment() {
    private var questionId: Long = 0
    private lateinit var document: Document

    private var _binding: FragmentQuestionDetailsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionId = requireArguments().getLong(ARG_QUESTION_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionDetailsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupUpDarkMode(binding.webview)
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
            binding.title.text = question.title

            document = Jsoup.parse(question.detail)
            document.select("img.lazy").forEach { it.remove() }
            binding.webview.loadData(
                """
                    <head>
                    <link rel="stylesheet" href="//zhihu-plus.internal/assets/stylesheet.css">
                    <viewport content="width=device-width, initial-scale=1.0">
                    </head>
                """.trimIndent() + document.toString(), "text/html", "utf-8"
            )
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(questionId: Long) =
            QuestionDetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_QUESTION_ID, questionId)
                }
            }
    }
}
