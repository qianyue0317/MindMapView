package com.qianyue.mindmapview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.qianyue.mindmapview.model.MindMapNode
import com.qianyue.mindmapview.util.NodeAdapter
import java.util.LinkedList
import kotlin.math.max

/**
 * @author QianYue
 * @since 2023/9/6
 */
class MindMapView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attributeSet, defStyle) {

    init {
        setWillNotDraw(false)
    }

    companion object {
        // 摆放在左侧
        const val MODE_LEFT = 1

        // 摆放在右侧
        const val MODE_RIGHT = 2

        // 摆放在两侧
        const val MODE_BOTH = 3
    }

    private val _tempQueue: LinkedList<MindMapNode<*>> = LinkedList()

    private val _cacheView: MutableMap<MindMapNode<*>, View> = mutableMapOf()

    private val _mindContentRegion = Rect()

    // 所有节点占的总大小，宽高
    private val _mindContentSize = arrayOf(0, 0)

    var adapter: NodeAdapter<*>? = null
        set(value) {
            field = value
            value?.observer = Observer()
        }

    var nodeVerSpace = context.resources.getDimensionPixelOffset(R.dimen.mind_map_node_ver_space)
        set(value) {
            val change = field != value
            field = value
            if (change) {
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

    var nodeLinePainter: NodeLinePainter? = DefaultLinePainter(context)
        set(value) {
            val change = field != value
            field = value
            if (change) {
                invalidate()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        removeAllViews()

        adapter ?: return
        adapter!!.root ?: return

        _tempQueue.clear()

        // 测量子节点
        val rootNode = adapter!!.root!!
        rootNode.leftOrRight = 0
        var currentLevel = -1
        var posIndexInLevel = -1
        var currentParent: MindMapNode<*>? = null

        var expectedWidth = 0

        _tempQueue.offer(rootNode)

        while (_tempQueue.isNotEmpty()) {
            val tempNode = _tempQueue.pop()
            tempNode.reset()
            currentParent = tempNode.parent
            tempNode.level = tempNode.parent?.let { it.level + 1 } ?: 0
            if (tempNode.level != currentLevel) posIndexInLevel = 0 else posIndexInLevel++
            currentLevel = tempNode.level
            tempNode.posInLevel = posIndexInLevel
            if (tempNode.level == 0 && tempNode != rootNode) {
                // 只有根节点的root可空
                throw RuntimeException("only root node's parent field can be null")
            }


            // TODO: 展开收起处理
//            if (!tempNode.expanded) continue0

            val view =
                adapter!!.getView(_cacheView[tempNode], currentLevel, posIndexInLevel, tempNode)
                    .also { _cacheView[tempNode] = it }
            view.setTag(R.id.mind_node_tag, tempNode)
            view.measure(
                MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST),
            )
            tempNode.selfHeight = view.measuredHeight
            view.visibility = if (tempNode.isExpanded()) View.VISIBLE else View.GONE
            addView(view)

            currentParent?.let {
                it.childrenHeight += (tempNode.placeHeight + nodeVerSpace)
                tempNode.saveOldPlaceHeight()
                if (it.children!!.last() == tempNode) {
                    it.childrenHeight -= nodeVerSpace

                    var tempParent = it.parent
                    var tempChild = it
                    while (tempParent != null) {
                        tempParent.childrenHeight -= tempChild.oldPlaceHeight
                        tempParent.childrenHeight += tempChild.placeHeight
                        tempChild.saveOldPlaceHeight()
                        tempChild = tempParent
                        tempParent = tempParent.parent
                    }
                }
            }

            // <editor-fold desc="测出导图内容宽度">
            if (tempNode.isLeaf()) {
                var node = tempNode
                var width = 0
                while (node != null) {
                    width += (_cacheView[node]!!.measuredWidth + nodeHorSpace)
                    node = node.parent
                }
                width -= nodeHorSpace
                expectedWidth = max(width, expectedWidth)
            }
            // </editor-fold>

            tempNode.children?.takeIf { it.isNotEmpty() }?.let { _tempQueue.addAll(it) }
        }

        _mindContentSize[0] = expectedWidth
        _mindContentSize[1] = rootNode.placeHeight
        // 尺寸完全自适应，节点占多少，自身就占多少
        super.onMeasure(MeasureSpec.makeMeasureSpec(_mindContentSize[0], MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(_mindContentSize[1], MeasureSpec.EXACTLY))
    }



    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val vCenter = measuredHeight / 2
        val hCenter = measuredWidth / 2

        // TODO: 还没有处理padding

        _tempQueue.clear()

        val rootNode = adapter?.root ?: return
        val rootView = _cacheView[rootNode] ?: return
        // 根节点
        val rootLeft = (measuredWidth - _mindContentSize[0]) / 2
        rootView.layout(
            rootLeft,
            vCenter - rootView.measuredHeight / 2,
            rootLeft + rootView.measuredWidth,
            vCenter + rootView.measuredHeight / 2
        )

        _tempQueue.addAll(rootNode.children ?: emptyList())
        while (_tempQueue.isNotEmpty()) {
            val tempNode = _tempQueue.pop()
            val tempView = _cacheView[tempNode] as View
            val parentNode = tempNode.parent
            val parentView = _cacheView[parentNode] as View
            val nodeIndex = parentNode!!.children!!.indexOf(tempNode)
            if (nodeIndex == 0) parentNode.layoutConsumed = 0


            val left = parentView.right + nodeHorSpace
            val vCenter = parentView.top + parentView.measuredHeight / 2
            val top = vCenter - (parentNode.placeHeight / 2) + parentNode.layoutConsumed

            if (parentNode.selfHeight > parentNode.childrenHeight) {
                var tempTop = vCenter - (parentNode.childrenHeight / 2) + parentNode.layoutConsumed
                if (tempNode.placeHeight > tempNode.selfHeight) {
                    tempTop += (tempNode.placeHeight - tempNode.selfHeight) / 2
                }
                tempView.layout(left, tempTop, left + tempView.measuredWidth, tempTop + tempView.measuredHeight)
            }
             else if (tempNode.placeHeight > tempNode.selfHeight) {
                tempView.layout(
                    left,
                    top + (tempNode.placeHeight - tempNode.selfHeight) / 2,
                    left + tempView.measuredWidth,
                    top + (tempNode.placeHeight - tempNode.selfHeight) / 2 + tempView.measuredHeight
                )
            }
            else tempView.layout(left, top, left + tempView.measuredWidth, top + tempView.measuredHeight)
            parentNode.layoutConsumed += (if (nodeIndex != (parentNode.children!!.size - 1)) tempNode.placeHeight + nodeVerSpace else tempNode.placeHeight)

            _tempQueue.addAll(tempNode.children ?: emptyList())
        }
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

}