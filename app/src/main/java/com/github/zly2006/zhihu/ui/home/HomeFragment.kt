package com.github.zly2006.zhihu.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.databinding.FragmentHomeBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderContent
import com.github.zly2006.zhihu.ui.home.browse.MyHomeArticleItemRecyclerViewAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.list.adapter = MyHomeArticleItemRecyclerViewAdapter(
            listOf(
                PlaceholderContent.PlaceholderItem(
                    "#1",
                    "Item 1",
                    "This is item 1"
                ),
                PlaceholderContent.PlaceholderItem(
                    "#2",
                    "Item 2 long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long ",
                    "This is item 2, very very" +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. " +
                    "very very long. "
                ),
            ).toMutableList().apply {
                repeat(98) {
                    add(
                        PlaceholderContent.PlaceholderItem(
                            "#${it + 3}",
                            "Item ${it + 3}",
                            "This is item ${it + 3}"
                        )
                    )
                }
            }
        )

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
