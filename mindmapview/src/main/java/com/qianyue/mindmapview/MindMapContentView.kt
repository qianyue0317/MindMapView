package com.qianyue.mindmapview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.qianyue.mindmapview.layoutstrategy.RightLayoutStrategy
import com.qianyue.mindmapview.model.MindMapNode
import com.qianyue.mindmapview.nodelinepainter.DefaultLinePainter
import com.qianyue.mindmapview.util.NodeAdapter
import java.util.LinkedList

/**
 * 真正的摆放节点的视图
 *
 * @author QianYue
 * @since 2023/9/6
 */
@SuppressLint("CustomViewStyleable")
class MindMapContentView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attributeSet, defStyle) {

    var nodeVerSpace = context.resources.getDimensionPixelOffset(R.dimen.mind_map_node_ver_space)
        set(value) {
            val change = field != value
            field = value
            if (change) {
                forceLayout()
                requestLayout()
            }
        }

    var nodeHorSpace = context.resources.getDimensionPixelOffset(R.dimen.mind_map_node_hor_space)
        set(value) {
            val change = field != value
            field = value
            if (change) {
                requestLayout()
            }
        }

    var layoutStrategy: NodeLayoutStrategy? = null
        set(value) {
            value?.init(layoutHelper)
            field = value
            requestLayout()
        }


    var nodeLinePainter: NodeLinePainter? = null
        set(value) {
            val change = field != value
            field = value
            if (change) {
                invalidate()
            }
        }

    private val layoutHelper: LayoutHelper = object :LayoutHelper {
        override val horSpace: Int
            get() = (this@MindMapContentView).nodeHorSpace
        override val verSpace: Int
            get() = (this@MindMapContentView).nodeVerSpace
        override val adapter: NodeAdapter<*>?
            get() = (this@MindMapContentView).adapter
        override val container: MindMapContentView
            get() = this@MindMapContentView

        override fun getCachedView(node: MindMapNode<*>): View? {
            return _cacheView[node]
        }

        override fun cacheView(node: MindMapNode<*>, view: View) {
            _cacheView[node] = view
        }
    }

    init {
        setWillNotDraw(false)

        attributeSet?.apply {
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.MindMapView)
            nodeHorSpace =
                typedArray.getDimensionPixelOffset(R.styleable.MindMapView_horSpace, nodeHorSpace)
            nodeVerSpace =
                typedArray.getDimensionPixelOffset(R.styleable.MindMapView_verSpace, nodeVerSpace)
            typedArray.recycle()
        }

        layoutStrategy = RightLayoutStrategy()
        nodeLinePainter = DefaultLinePainter(context)
//        setBackgroundColor(Color.YELLOW)
    }

    private val _tempQueue: LinkedList<MindMapNode<*>> = LinkedList()

    private val _cacheView: MutableMap<MindMapNode<*>, View> = mutableMapOf()

    // 所有节点占的总大小，宽高
    private val _mindContentSize = arrayOf(0, 0)

    var adapter: NodeAdapter<*>? = null
        set(value) {
            field = value
            value?.observer = Observer()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        layoutStrategy ?: return

        removeAllViews()

        val result: Long = layoutStrategy!!.onMeasure()
        if (result == 0L) {
            return
        }

        // 尺寸完全自适应，节点占多少，自身就占多少
        super.onMeasure(
            result.shr(32).toInt(),
            result.toInt()
        )
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutStrategy?.onLayout()
    }

    override fun onDraw(canvas: Canvas?) {
        nodeLinePainter ?: return
        val rootNode = adapter?.root ?: return
        _cacheView[rootNode] ?: return
        canvas ?: return

        _tempQueue.addAll(rootNode.children ?: emptyList())
        while (_tempQueue.isNotEmpty()) {
            val tempNode = _tempQueue.pop()
            val tempView = _cacheView[tempNode] as View
            val parentNode = tempNode.parent
            val parentView = _cacheView[parentNode] as View

            nodeLinePainter!!.drawLine(canvas, tempView, parentView)

            _tempQueue.addAll(tempNode.children ?: emptyList())
        }
    }

    inner class Observer : NodeAdapter.Observer {
        override fun notifyChange() {
            requestLayout()
        }
    }


    interface LayoutHelper {
        val horSpace: Int
        val verSpace: Int
        val adapter: NodeAdapter<*>?
        val container: MindMapContentView
        fun getCachedView(node: MindMapNode<*>): View?
        fun cacheView(node: MindMapNode<*>, view: View)
    }
}