package com.github.zly2006.zhihu.ui.home.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.zly2006.zhihu.MainActivity.MainActivityViewModel
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.databinding.FragmentCommentsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.jsoup.nodes.Document

class CommentFragment : Fragment() {
    private val httpClient by lazy { AccountData.httpClient(requireContext()) }
    private var fetchingNewItems = false
    private var canFetchMore = true
    private var questionId: Long = 0
    private lateinit var document: Document
    private var session = ""
    private var cursor = ""

    private var _binding: FragmentCommentsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val comments = mutableListOf<DataHolder.Comment>()

    //    private val viewModel: QuestionViewModel by viewModels()
    val gViewModel: MainActivityViewModel by activityViewModels()

    private suspend fun fetch() {
        try {
            fetchingNewItems = true
            activity?.runOnUiThread {
                val start = comments.size
                _binding?.list?.adapter?.notifyItemRangeInserted(start, 0)
            }
            fetchingNewItems = false
            if (canFetchMore) {
                if (_binding != null && !binding.list.canScrollVertically(binding.list.height)) {
                    fetch()
                }
            }
        } catch (_: Exception) {
            if (activity != null) {
                gViewModel.toast.postValue("Failed to load answers")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        viewModel.title.postValue(requireArguments().getParcelable("comments"))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val httpClient = AccountData.httpClient(requireContext())
        launch {
            fetch()
        }

        binding.list.adapter = CommentAdapter(comments, requireActivity(), httpClient)
        binding.list.setOnScrollChangeListener { _, _, _, _, _ ->
            if (!canFetchMore || fetchingNewItems) return@setOnScrollChangeListener
            if (!binding.list.canScrollVertically(binding.list.height)) {
                launch {
                    fetch()
                }
            }
        }

        launch {
            val question = DataHolder.getQuestion(requireActivity(), httpClient, questionId)?.value
            if (question == null) {
                if (context != null) {
                    AlertDialog.Builder(requireContext()).apply {
                        setTitle("Error")
                        setMessage("Failed to load question details $questionId")
                        setPositiveButton("OK") { _, _ ->
                        }
                    }.create().show()
                }
                return@launch
            }
        }
        return root
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        GlobalScope.launch(requireActivity().mainExecutor.asCoroutineDispatcher(), block = block)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
