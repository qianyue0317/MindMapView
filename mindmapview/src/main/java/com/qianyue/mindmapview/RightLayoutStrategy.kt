package com.qianyue.mindmapview

import android.util.Size
import android.view.View
import android.view.View.MeasureSpec
import com.qianyue.mindmapview.model.MindMapNode
import java.util.LinkedList
import kotlin.math.max

/**
 * @author QianYue
 * @since 2023/9/18
 */
class RightLayoutStrategy : NodeLayoutStrategy {
    lateinit var layoutHelper: MindMapContentView.LayoutHelper

    private val _tempQueue: LinkedList<MindMapNode<*>> = LinkedList()

    private var _size: Size? = null

    override fun init(layoutHelper: MindMapContentView.LayoutHelper) {
        this.layoutHelper = layoutHelper
    }

    override fun onMeasure(): Long {
        layoutHelper.adapter ?: return 0L
        layoutHelper.adapter!!.root ?: return 0L
        val nodeVerSpace = layoutHelper.verSpace
        val nodeHorSpace = layoutHelper.horSpace
        val container = layoutHelper.container

        _tempQueue.clear()

        // 测量子节点
        val rootNode = layoutHelper.adapter!!.root!!
        rootNode.leftOrRight = 0
        var currentLevel = -1
        var posIndexInLevel = -1
        var currentParent: MindMapNode<*>?

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

            var noViewCache = true
            val view =
                layoutHelper.adapter!!.getView(
                    layoutHelper.getCachedView(tempNode)?.also { noViewCache = false },
                    currentLevel,
                    posIndexInLevel,
                    tempNode
                ).also { if (noViewCache) layoutHelper.cacheView(tempNode, it) }
            view.setTag(R.id.mind_node_tag, tempNode)
            view.measure(
                MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST),
            )
            tempNode.selfHeight = view.measuredHeight
            view.visibility = if (tempNode.isExpanded()) View.VISIBLE else View.GONE
            container.addView(view)

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
                    width += (layoutHelper.getCachedView(node)!!.measuredWidth + nodeHorSpace)
                    node = node.parent
                }
                width -= nodeHorSpace
                expectedWidth = max(width, expectedWidth)
            }
            // </editor-fold>

            tempNode.children?.takeIf { it.isNotEmpty() }?.let { _tempQueue.addAll(it) }
        }

        _size = Size(expectedWidth, rootNode.placeHeight)

        return (MeasureSpec.makeMeasureSpec(expectedWidth, MeasureSpec.EXACTLY).toLong() shl 32) or (MeasureSpec.makeMeasureSpec(rootNode.placeHeight, MeasureSpec.EXACTLY).toLong())
    }

    override fun onLayout() {
        _size ?: return
        val vCenter = _size!!.height / 2
        val hCenter = _size!!.width / 2
        val nodeVerSpace = layoutHelper.verSpace
        val nodeHorSpace = layoutHelper.horSpace
        val container = layoutHelper.container


        // TODO: 还没有处理padding

        _tempQueue.clear()

        val rootNode = layoutHelper.adapter?.root ?: return
        val rootView = layoutHelper.getCachedView(rootNode) ?: return
        // 根节点
        val rootLeft = (layoutHelper.container.measuredWidth - _size!!.width) / 2
        rootView.layout(
            rootLeft,
            vCenter - rootView.measuredHeight / 2,
            rootLeft + rootView.measuredWidth,
            vCenter + rootView.measuredHeight / 2
        )

        _tempQueue.addAll(rootNode.children ?: emptyList())
        while (_tempQueue.isNotEmpty()) {
            val tempNode = _tempQueue.pop()
            val tempView = layoutHelper.getCachedView(tempNode) as View
            val parentNode = tempNode.parent
            val parentView = layoutHelper.getCachedView(parentNode!!) as View
            val nodeIndex = parentNode.children!!.indexOf(tempNode)
            if (nodeIndex == 0) parentNode.layoutConsumed = 0

            val left = parentView.right + nodeHorSpace
            val vCenter = parentView.top + parentView.measuredHeight / 2
            val top = vCenter - (parentNode.placeHeight / 2) + parentNode.layoutConsumed

            if (parentNode.selfHeight > parentNode.childrenHeight) {
                var tempTop = vCenter - (parentNode.childrenHeight / 2) + parentNode.layoutConsumed
                if (tempNode.placeHeight > tempNode.selfHeight) {
                    tempTop += (tempNode.placeHeight - tempNode.selfHeight) / 2
                }
                tempView.layout(
                    left,
                    tempTop,
                    left + tempView.measuredWidth,
                    tempTop + tempView.measuredHeight
                )
            } else if (tempNode.placeHeight > tempNode.selfHeight) {
                tempView.layout(
                    left,
                    top + (tempNode.placeHeight - tempNode.selfHeight) / 2,
                    left + tempView.measuredWidth,
                    top + (tempNode.placeHeight - tempNode.selfHeight) / 2 + tempView.measuredHeight
                )
            } else tempView.layout(
                left,
                top,
                left + tempView.measuredWidth,
                top + tempView.measuredHeight
            )
            parentNode.layoutConsumed += (if (nodeIndex != (parentNode.children!!.size - 1)) tempNode.placeHeight + nodeVerSpace else tempNode.placeHeight)

            _tempQueue.addAll(tempNode.children ?: emptyList())
        }
    }

}