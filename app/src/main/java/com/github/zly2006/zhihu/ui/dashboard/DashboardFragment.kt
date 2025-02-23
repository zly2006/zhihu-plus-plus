package com.github.zly2006.zhihu.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.login.setOnClickListener {
            val myIntent = Intent(context, LoginActivity::class.java)
            startActivity(myIntent)
        }
        binding.logout.setOnClickListener {
            AccountData.delete(requireContext())
        }
        binding.openUrl.setOnClickListener {
            val url = binding.url.text.toString()
            Uri.parse(url)?.let { uri ->
                if (uri.host == "www.zhihu.com" || uri.host == "zhuanlan.zhihu.com") {
                    val intent = Intent(Intent.ACTION_VIEW, uri, requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    return@let
                }
                Toast.makeText(context, "Invalid URL (not zhihu)", Toast.LENGTH_LONG).show()
            }
        }

        if (requireActivity() is MainActivity) {
            val httpClient = AccountData.httpClient(requireContext())
            val history = (requireActivity() as MainActivity).history.history
            if (binding.viewHistory.adapter != null) {
                @SuppressLint("NotifyDataSetChanged")
                binding.viewHistory.adapter!!.notifyDataSetChanged()
            }
            binding.viewHistory.adapter = ViewHistoryAdapter(history, requireActivity(), httpClient)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
