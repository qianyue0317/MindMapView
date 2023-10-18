package com.qianyue.mindmapview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams
import com.qianyue.mindmapview.util.NodeAdapter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 支持触摸事件的容器
 *
 * @author QianYue
 * @since 2023/9/14
 */
class MindMapView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attributeSet, defStyle) {

    private val mindMapContentView: MindMapContentView

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        var nodeHorSpace =
            context.resources.getDimensionPixelOffset(R.dimen.mind_map_node_hor_space)
        var nodeVerSpace =
            context.resources.getDimensionPixelOffset(R.dimen.mind_map_node_ver_space)
        attributeSet?.let {
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.MindMapView)
            nodeHorSpace = typedArray.getDimensionPixelOffset(
                R.styleable.MindMapView_horSpace, nodeHorSpace
            )
            nodeVerSpace = typedArray.getDimensionPixelOffset(
                R.styleable.MindMapView_verSpace,
                nodeVerSpace
            )
            typedArray.recycle()
        }

        mindMapContentView = MindMapContentView(context).apply {
            this.nodeHorSpace = nodeHorSpace
            this.nodeVerSpace = nodeVerSpace
        }
        addView(
            mindMapContentView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
    }

    var enableTouch: Boolean = true

    fun setHorSpace(horSpace: Int) {
        mindMapContentView.nodeHorSpace = horSpace
    }

    fun setVerSpace(verSpace: Int) {
        mindMapContentView.nodeVerSpace = verSpace
    }

    fun setLayoutStrategy(layoutStrategy: NodeLayoutStrategy) {
        mindMapContentView.layoutStrategy = layoutStrategy
    }

    fun setNodeLinePainter(nodeLinePainter: NodeLinePainter) {
        mindMapContentView.nodeLinePainter = nodeLinePainter
    }

    var maxScaleFactor = 3f

    var minScaleFactor = 0.2f

    // 使导图控件正好填充此容器，需要缩放的因子
    private var fitScaleFactor = -1f

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

    fun <T> setAdapter(nodeAdapter: NodeAdapter<T>) {
        mindMapContentView.adapter = nodeAdapter
    }

    fun setContentGravity(gravity: Int) {
        mindMapContentView.updateLayoutParams<LayoutParams> {
            this.gravity = gravity
        }
    }

    fun fitCenter() {
        val fitScaleRun = {
            // 要自适应缩放必须设置Gravity为Center
            setContentGravity(Gravity.CENTER)

            matrix.getValues(matrixValues)
            matrix.setScale(
                fitScaleFactor,
                fitScaleFactor,
                mindMapContentView.measuredWidth / 2f,
                mindMapContentView.measuredHeight / 2f
            )
            mindMapContentView.pivotX = 0f
            mindMapContentView.pivotY = 0f
            mindMapContentView.scaleX =
                matrix.getValues(matrixValues).let { matrixValues }[Matrix.MSCALE_X]
            mindMapContentView.scaleY = matrixValues[Matrix.MSCALE_Y]
            mindMapContentView.translationX = matrixValues[Matrix.MTRANS_X]
            mindMapContentView.translationY = matrixValues[Matrix.MTRANS_Y]
        }
        if (fitScaleFactor < 0) {
            // 还没有测量
            post {
                fitScaleRun()
            }
        } else fitScaleRun()

    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (!enableTouch) return false
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        fitScaleFactor = min(
            (measuredHeight - paddingTop - paddingBottom) / mindMapContentView.measuredHeight.toFloat(),
            (measuredWidth- paddingStart - paddingEnd) / mindMapContentView.measuredWidth .toFloat()
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (!enableTouch) return false
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

        if (abs(offsetLR) > 0 || abs(offsetTB) > 0) mindMapContentView.let {
            it.translationX += (offsetLR)
            it.translationY += (offsetTB)
        }

        if (changeScale) mindMapContentView.let {
            it.pivotX = -it.left.toFloat()
            it.pivotY = -it.top.toFloat()
            it.scaleX = matrix.getValues(matrixValues).let { matrixValues }[Matrix.MSCALE_X]
            it.scaleY = matrixValues[Matrix.MSCALE_Y]
            it.translationX = matrixValues[Matrix.MTRANS_X]
            it.translationY = matrixValues[Matrix.MTRANS_Y]
        }
        return true
    }

    fun resetPosition() {
        matrix.reset()
        forEach {
            it.pivotX = 0f
            it.pivotY = 0f
            it.scaleX = matrix.getValues(matrixValues).let { matrixValues }[Matrix.MSCALE_X]
            it.scaleY = matrixValues[Matrix.MSCALE_Y]
            it.translationX = matrixValues[Matrix.MTRANS_X]
            it.translationY = matrixValues[Matrix.MTRANS_Y]
        }
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