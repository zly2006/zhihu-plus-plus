package com.github.zly2006.zhihu.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import com.github.zly2006.zhihu.ui.view.ZoomableImageView

class ZoomableImageDialog(context: Context, private val image: Bitmap) : Dialog(context) {
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        setCanceledOnTouchOutside(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageView = ZoomableImageView(context).apply {
            setImageBitmap(image)
            setOnClickListener { dismiss() }
        }
        setContentView(imageView)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        image.recycle()
    }
}
