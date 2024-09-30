package com.github.zly2006.zhihu.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.zly2006.zhihu.databinding.FragmentReadArticleBinding

/**
 * A simple [Fragment] subclass.
 * Use the [ReadArticleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ReadArticleFragment : Fragment() {

    private var _binding: FragmentReadArticleBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadArticleBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.web

        return root
    }
}
