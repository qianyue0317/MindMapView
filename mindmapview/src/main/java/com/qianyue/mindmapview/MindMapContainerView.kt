package com.qianyue.mindmapview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.forEach
import kotlin.math.abs

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

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var lastPoint: Point = Point(0, 0)
    private var downPoint: Point = Point(0, 0)

    private var isDragging: Boolean = false
    private var isScaling: Boolean = false

    private val matrix: Matrix = Matrix()
    private val matrixValues = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

    private val scaleCenterPointer = PointF()

    private val lastPointer0: PointF = PointF(0f, 0f)
    private val lastPointer1: PointF = PointF(0f, 0f)

    private var lastPointerId0 = -1
    private var lastPointerId1 = -1

    private var distanceBetweenPointer = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.actionMasked ?: return false) {
            MotionEvent.ACTION_DOWN -> {
                downPoint.x = ev.x.toInt()
                downPoint.y = ev.y.toInt()
                lastPoint.set(downPoint.x, downPoint.y)
            }

            MotionEvent.ACTION_MOVE -> {
                lastPoint.apply {
                    x = ev.x.toInt()
                    y = ev.y.toInt()
                }
                if (!isDragging && downPoint.distance(ev.x.toInt(), ev.y.toInt()) > touchSlop) {
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
        var offsetLR = 0
        var offsetTB = 0
        var changeScale = false

        when (ev!!.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downPoint.x = ev.x.toInt()
                downPoint.y = ev.y.toInt()
                lastPoint.set(downPoint.x, downPoint.y)
            }

            MotionEvent.ACTION_MOVE -> {

                val dx = ev.x - lastPoint.x
                val dy = ev.y - lastPoint.y

                lastPoint.apply {
                    x = ev.x.toInt()
                    y = ev.y.toInt()
                }
                if (!isScaling && !isDragging && downPoint.distance(
                        ev.x.toInt(),
                        ev.y.toInt()
                    ) > touchSlop
                ) {
                    isDragging = true
                }

                if (isDragging) {
                    matrix.postTranslate(dx, dy)
                    offsetLR = dx.toInt()
                    offsetTB = dy.toInt()
                }

                if (isScaling) {
                    changeScale = true
                    val oldScale =
                        matrix.getValues(matrixValues).let { matrixValues }[Matrix.MSCALE_X]
                    val distance = distanceBetweenPointer
                    resetCenterPoint(ev)
                    matrix.postScale(
                        oldScale * (distanceBetweenPointer / distance) / oldScale,
                        oldScale * (distanceBetweenPointer / distance) / oldScale,
                        scaleCenterPointer.x,
                        scaleCenterPointer.y
                    )
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
        invalidate()
        return true
    }

    val paint = Paint().apply {
        setColor(Color.BLUE)
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.save()
        canvas?.concat(matrix)
        canvas?.drawCircle(400f, 700f, 60f, paint)
        canvas?.restore()
    }

    private fun resetCenterPoint(ev: MotionEvent) {
        val x0 = ev.getX(0)
        val y0 = ev.getY(0)
        val x1 = ev.getX(1)
        val y1 = ev.getY(1)

        scaleCenterPointer.x = x0 + (x1 - x0) / 2
        scaleCenterPointer.y = y0 + (y1 - y0) / 2
        Log.i("qianyueqianyue", "${scaleCenterPointer.x},${scaleCenterPointer.y}")

        lastPointer0.x = x0
        lastPointer0.y = y0

        lastPointer1.x = x1
        lastPointer1.y = y1

//        lastPointerId0 = ev.getPointerId(0)
//        lastPointerId1 = ev.getPointerId(1)

        distanceBetweenPointer = lastPointer0.distance(lastPointer1).toFloat()
    }

    private fun resetFlags() {
        isScaling = false
        isDragging = false
    }

}