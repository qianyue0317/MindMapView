package com.qianyue.mindmapview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.forEach
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @author QianYue
 * @since 2023/9/14
 */
class MindMapContainerView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attributeSet, defStyle) {

    init {
        setWillNotDraw(false)
    }

    var maxScaleFactor = 3f

    var minScaleFactor = 0.2f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var lastPoint: PointF = PointF(0f, 0f)
    private var downPoint: PointF = PointF(0f, 0f)

    private var isDragging: Boolean = false
    private var isScaling: Boolean = false

    private val matrix: Matrix = Matrix()
    private val matrixValues = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

    private val scaleCenterPointer = PointF()

    private val lastPointer0: PointF = PointF(0f, 0f)
    private val lastPointer1: PointF = PointF(0f, 0f)

    private var distanceBetweenPointer = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.actionMasked ?: return false) {
            MotionEvent.ACTION_DOWN -> {
                downPoint.x = ev.x
                downPoint.y = ev.y
                lastPoint.set(downPoint.x, downPoint.y)
            }

            MotionEvent.ACTION_MOVE -> {
                lastPoint.apply {
                    x = ev.x
                    y = ev.y
                }
                if (!isDragging && downPoint.distance(ev.x, ev.y) > touchSlop) {
                    isDragging = true
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (ev.pointerCount == 2) {
                    // scaling
                    isScaling = true
                    resetCenterPoint(ev)
                    isDragging = false
                } else {
                    isScaling = false
                    isDragging = false
                }
                return true
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        var offsetLR = 0f
        var offsetTB = 0f
        var changeScale = false

        when (ev!!.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downPoint.x = ev.x
                downPoint.y = ev.y
                lastPoint.set(downPoint.x, downPoint.y)
            }

            MotionEvent.ACTION_MOVE -> {

                val dx = ev.x - lastPoint.x
                val dy = ev.y - lastPoint.y

                if (!isScaling && !isDragging && downPoint.distance(
                        ev.x,
                        ev.y
                    ) > touchSlop
                ) {
                    isDragging = true
                }

                if (isDragging && lastPoint.x > 0) {
                    matrix.postTranslate(dx, dy)
                    offsetLR = dx
                    offsetTB = dy
                }

                if (isScaling) {
                    changeScale = true
                    val oldScale =
                        matrix.getValues(matrixValues).let { matrixValues }[Matrix.MSCALE_X]
                    val distance = distanceBetweenPointer
                    resetCenterPoint(ev)
                    val newScale = min(
                        max((distanceBetweenPointer / distance) * oldScale, minScaleFactor),
                        maxScaleFactor
                    )
                    val postScale = newScale / oldScale
                    matrix.postScale(
                        postScale,
                        postScale,
                        scaleCenterPointer.x,
                        scaleCenterPointer.y
                    )
                }

                lastPoint.apply {
                    x = ev.x
                    y = ev.y
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (ev.pointerCount == 2) {
                    // scaling
                    isScaling = true
                    resetCenterPoint(ev)
                    isDragging = false
                } else {
                    isScaling = false
                    isDragging = false
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                when (ev.pointerCount) {
                    3 -> {
                        isDragging = false
                        isScaling = true
                        resetCenterPoint(ev)
                    }

                    2 -> {
                        // 两指变一指，防止跳动
                        lastPoint.x = -1f
                        lastPoint.y = -1f
                        isDragging = true
                        isScaling = false
                    }

                    else -> {
                        isDragging = false
                        isScaling = false
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resetFlags()
            }
        }

        if (abs(offsetLR) > 0 || abs(offsetTB) > 0) forEach {
            it.translationX += (offsetLR)
            it.translationY += (offsetTB)
        }

        if (changeScale) forEach {
            it.pivotX = 0f
            it.pivotY = 0f
            it.scaleX = matrix.getValues(matrixValues).let { matrixValues }[Matrix.MSCALE_X]
            it.scaleY = matrixValues[Matrix.MSCALE_Y]
            it.translationX = matrixValues[Matrix.MTRANS_X]
            it.translationY = matrixValues[Matrix.MTRANS_Y]
        }
        return true
    }

    private fun resetCenterPoint(ev: MotionEvent) {
        val x0 = ev.getX(0)
        val y0 = ev.getY(0)
        val x1 = ev.getX(1)
        val y1 = ev.getY(1)

        scaleCenterPointer.x = x0 + (x1 - x0) / 2
        scaleCenterPointer.y = y0 + (y1 - y0) / 2

        lastPointer0.x = x0
        lastPointer0.y = y0

        lastPointer1.x = x1
        lastPointer1.y = y1

        distanceBetweenPointer = lastPointer0.distance(lastPointer1).toFloat()
    }

    private fun resetFlags() {
        isScaling = false
        isDragging = false
    }

}