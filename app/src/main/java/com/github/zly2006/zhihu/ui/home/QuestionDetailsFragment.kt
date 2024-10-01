package com.github.zly2006.zhihu.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.zly2006.zhihu.R

private const val ARG_QUESTION_ID = "q-id"

class QuestionDetailsFragment : Fragment() {
    private lateinit var questionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionId = requireArguments().getString(ARG_QUESTION_ID)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_question_details, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment QuestionDetailsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(questionId: String) =
            QuestionDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUESTION_ID, questionId)
                }
            }
    }
}
