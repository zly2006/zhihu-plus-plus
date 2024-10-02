package com.github.zly2006.zhihu.ui.home.question

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.databinding.FragmentQuestionDetailsAnswerBinding

class AnswerListAdapter(
    private val values: List<DataHolder.Answer>,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<AnswerListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentQuestionDetailsAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
//        holder.title.text = item
//        holder.summary.text = item.summary
//        holder.details.text = item.details
//        holder.card.setOnClickListener {
//
//            val readArticleFragment = ReadArticleFragment.newInstance(item.id)
//
//            activity.supportFragmentManager.commit {
//                setCustomAnimations(
//                    R.anim.slide_in,
//                    R.anim.slide_in,
//                    0,
//                    0
//                )
//                replace(
//                    R.id.nav_host_fragment_activity_main,
//                    readArticleFragment
//                )
//                addToBackStack("Read-Article")
//            }
//        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentQuestionDetailsAnswerBinding) : RecyclerView.ViewHolder(binding.root) {

    }
}
