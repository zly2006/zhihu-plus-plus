package com.github.zly2006.zhihu.ui.view

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var matrix = Matrix()
    private var mode = NONE
    private var last = PointF()
    private var start = PointF()
    private var minScale = 1f
    private var maxScale = 4f
    private var m = FloatArray(9)
    private var viewWidth = 0
    private var viewHeight = 0
    private var saveScale = 1f
    private var origWidth = 0f
    private var origHeight = 0f
    private var oldMeasuredWidth = 0
    private var oldMeasuredHeight = 0
    private var hasMoved = false
    private var startTime = 0L

    init {
        scaleType = ScaleType.MATRIX
        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val mScaleFactor = detector.scaleFactor
                val origScale = saveScale
                saveScale *= mScaleFactor
                if (saveScale > maxScale) {
                    saveScale = maxScale
                    return true
                } else if (saveScale < minScale) {
                    saveScale = minScale
                    return true
                }
                
                if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight)
                    matrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2f, viewHeight / 2f)
                else
                    matrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
                
                fixTranslation()
                return true
            }
        }
        val scaleDetector = ScaleGestureDetector(context, scaleListener)

        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            matrix.getValues(m)
            val x = m[Matrix.MTRANS_X]
            val y = m[Matrix.MTRANS_Y]
            val curr = PointF(event.x, event.y)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    last.set(event.x, event.y)
                    start.set(last)
                    mode = DRAG
                    hasMoved = false
                    startTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    last.set(event.x, event.y)
                    start.set(last)
                    mode = ZOOM
                    hasMoved = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG && saveScale > minScale) {
                        val deltaX = curr.x - last.x
                        val deltaY = curr.y - last.y
                        // 判断移动距离是否超过阈值
                        if (Math.abs(deltaX) > CLICK_THRESHOLD || Math.abs(deltaY) > CLICK_THRESHOLD) {
                            hasMoved = true
                        }
                        matrix.postTranslate(deltaX, deltaY)
                        fixTranslation()
                        last.set(curr.x, curr.y)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mode = NONE
                    // 检查是否是单击事件
                    val clickDuration = System.currentTimeMillis() - startTime
                    if (!hasMoved && clickDuration < CLICK_DURATION_THRESHOLD) {
                        performClick()
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> mode = NONE
            }
            imageMatrix = matrix
            invalidate()
            true
        }
    }

    private fun fixTranslation() {
        matrix.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]
        val fixTransX = getFixTranslation(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTranslation(transY, viewHeight.toFloat(), origHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f)
            matrix.postTranslate(fixTransX, fixTransY)
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float

        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }

        if (trans < minTrans) return -trans + minTrans
        if (trans > maxTrans) return -trans + maxTrans
        return 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (oldMeasuredWidth == viewWidth && oldMeasuredHeight == viewHeight || viewWidth == 0 || viewHeight == 0)
            return

        oldMeasuredHeight = viewHeight
        oldMeasuredWidth = viewWidth

        if (drawable == null) return
        
        origWidth = drawable.intrinsicWidth.toFloat()
        origHeight = drawable.intrinsicHeight.toFloat()
        
        val scaleX = viewWidth / origWidth
        val scaleY = viewHeight / origHeight
        val scale = minOf(scaleX, scaleY)
        
        matrix.setScale(scale, scale)
        matrix.postTranslate(
            (viewWidth - origWidth * scale) / 2f,
            (viewHeight - origHeight * scale) / 2f
        )
        
        imageMatrix = matrix
        saveScale = 1f
    }

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
        private const val CLICK_THRESHOLD = 10f  // 移动阈值，小于这个值认为是点击
        private const val CLICK_DURATION_THRESHOLD = 200L  // 点击持续时间阈值
    }
}
