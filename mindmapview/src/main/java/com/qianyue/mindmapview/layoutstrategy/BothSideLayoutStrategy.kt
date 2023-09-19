package com.qianyue.mindmapview.layoutstrategy

import android.util.Size
import android.view.View
import android.view.View.MeasureSpec
import com.qianyue.mindmapview.MindMapContentView
import com.qianyue.mindmapview.NodeLayoutStrategy
import com.qianyue.mindmapview.R
import com.qianyue.mindmapview.model.MindMapNode
import java.util.LinkedList
import kotlin.math.max

/**
 * @author QianYue
 * @since 2023/9/18
 */
class BothSideLayoutStrategy : NodeLayoutStrategy {
    private lateinit var _layoutHelper: MindMapContentView.LayoutHelper

    private val _tempQueue: LinkedList<MindMapNode<*>> = LinkedList()

    private var _size: Size? = null

    private var rightHeight: Int = 0

    private var rightWidth: Int = 0

    private var leftHeight: Int = 0

    private var leftWidth: Int = 0

    private var rootHeight: Int = 0

    private var rootWidth = 0

    override fun init(layoutHelper: MindMapContentView.LayoutHelper) {
        _layoutHelper = layoutHelper
    }

    override fun onMeasure(): Long {
        _layoutHelper.adapter ?: return 0L
        _layoutHelper.adapter!!.root ?: return 0L
        val nodeVerSpace = _layoutHelper.verSpace
        val nodeHorSpace = _layoutHelper.horSpace
        val container = _layoutHelper.container

        rightHeight = 0
        rightWidth = 0
        leftHeight = 0
        leftWidth = 0
        rootHeight = 0
        rootWidth = 0

        val rootNode = _layoutHelper.adapter!!.root as MindMapNode<*>

        _tempQueue.clear()

        rootNode.leftOrRight = 0
        var currentLevel = -1
        var posIndexInLevel = -1
        var currentParent: MindMapNode<*>?


        val half =
            if (rootNode.children?.isNotEmpty() == true) (rootNode.children!!.size + 1) / 2 else -1


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
                _layoutHelper.adapter!!.getView(
                    _layoutHelper.getCachedView(tempNode)?.also { noViewCache = false },
                    currentLevel,
                    posIndexInLevel,
                    tempNode
                ).also { if (noViewCache) _layoutHelper.cacheView(tempNode, it) }
            view.setTag(R.id.mind_node_tag, tempNode)
            view.measure(
                MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST),
            )
            if (tempNode == rootNode) {
                rootWidth = view.measuredWidth
                rootHeight = view.measuredHeight
            }
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
                    width += (_layoutHelper.getCachedView(node)!!.measuredWidth + nodeHorSpace)

                    if (node.level == 1) {
                        if (node.posInLevel < half) {
                            rightWidth = max(width + rootWidth, rightWidth)
                        } else {
                            leftWidth = max(width + rootWidth, leftWidth)
                        }
                    }

                    node = node.parent
                }
                width -= nodeHorSpace
            }
            // </editor-fold>

            tempNode.children?.takeIf { it.isNotEmpty() }?.let { _tempQueue.addAll(it) }
        }


        rootNode.children?.takeIf { it.isNotEmpty() }?.let {

            for (i in it.indices) {
                if (i < half) {
                    rightHeight += it[i].placeHeight + nodeVerSpace
                } else {
                    leftHeight += it[i].placeHeight + nodeVerSpace
                }
            }
            rightHeight -= nodeVerSpace
            leftHeight -= nodeVerSpace
        }

        _size = Size(rightWidth + leftWidth - rootWidth , max(rightHeight, leftHeight))

        return (MeasureSpec.makeMeasureSpec(_size!!.width, MeasureSpec.EXACTLY)
            .toLong() shl 32) or (MeasureSpec.makeMeasureSpec(_size!!.height, MeasureSpec.EXACTLY)
            .toLong())
    }

    override fun onLayout() {
        _size ?: return
        val vCenter = _size!!.height / 2
        val hCenter = _size!!.width / 2
        val nodeVerSpace = _layoutHelper.verSpace
        val nodeHorSpace = _layoutHelper.horSpace
        val container = _layoutHelper.container

        val rootNode = _layoutHelper.adapter?.root ?: return
        val rootView = _layoutHelper.getCachedView(rootNode) ?: return
        // 根节点
        val rootLeft = leftWidth - rootView.measuredWidth
        val rootTop = (_layoutHelper.container.measuredHeight / 2) - rootView.measuredHeight / 2
        rootView.layout(
            rootLeft,
            rootTop,
            rootLeft + rootView.measuredWidth,
            rootTop + rootView.measuredHeight
        )

        _tempQueue.clear()
        if (rootNode.children?.isNotEmpty() == true) {
            _tempQueue.addAll(
                rootNode.children!!.subList(
                    0,
                    (rootNode.children!!.size + 1) / 2
                )
            )
        }

        // <editor-fold desc="右侧">
        while (_tempQueue.isNotEmpty()) {
            val tempNode = _tempQueue.pop()
            val tempView = _layoutHelper.getCachedView(tempNode) as View
            val parentNode = tempNode.parent
            val parentView = _layoutHelper.getCachedView(parentNode!!) as View
            val nodeIndex = parentNode.children!!.indexOf(tempNode)
            if (nodeIndex == 0) parentNode.layoutConsumed = 0

            val left = parentView.right + nodeHorSpace
            val vCenter = parentView.top + parentView.measuredHeight / 2

            val top =
                if (parentNode == rootNode) vCenter - (rightHeight / 2) + parentNode.layoutConsumed else vCenter - (parentNode.placeHeight / 2) + parentNode.layoutConsumed


            val childrenHeight =
                if (parentNode == rootNode) {
                    rightHeight
                } else parentNode.childrenHeight

            if (parentNode.selfHeight > childrenHeight) {
                var tempTop = vCenter - (childrenHeight / 2) + parentNode.layoutConsumed
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
        // </editor-fold>

        if (rootNode.children?.isNotEmpty() == true) {
            _tempQueue.addAll(
                rootNode.children!!.subList(
                    (rootNode.children!!.size + 1) / 2,
                    rootNode.children!!.size
                )
            )
        }

        rootNode.layoutConsumed = 0
        // <editor-fold desc="左侧">
        while (_tempQueue.isNotEmpty()) {
            val tempNode = _tempQueue.pop()
            val tempView = _layoutHelper.getCachedView(tempNode) as View
            val parentNode = tempNode.parent
            val parentView = _layoutHelper.getCachedView(parentNode!!) as View
            val nodeIndex = parentNode.children!!.indexOf(tempNode)
            if (nodeIndex == 0) parentNode.layoutConsumed = 0

            val right = parentView.left - nodeHorSpace
            val vCenter = parentView.top + parentView.measuredHeight / 2

            val top =
                if (parentNode == rootNode) vCenter - (leftHeight / 2) + parentNode.layoutConsumed else vCenter - (parentNode.placeHeight / 2) + parentNode.layoutConsumed


            val childrenHeight =
                if (parentNode == rootNode) {
                    leftHeight
                } else parentNode.childrenHeight

            if (parentNode.selfHeight > childrenHeight) {
                var tempTop = vCenter - (childrenHeight / 2) + parentNode.layoutConsumed
                if (tempNode.placeHeight > tempNode.selfHeight) {
                    tempTop += (tempNode.placeHeight - tempNode.selfHeight) / 2
                }
                tempView.layout(
                    right - tempView.measuredWidth,
                    tempTop,
                    right,
                    tempTop + tempView.measuredHeight
                )
            } else if (tempNode.placeHeight > tempNode.selfHeight) {
                tempView.layout(
                    right - tempView.measuredWidth,
                    top + (tempNode.placeHeight - tempNode.selfHeight) / 2,
                    right,
                    top + (tempNode.placeHeight - tempNode.selfHeight) / 2 + tempView.measuredHeight
                )
            } else tempView.layout(
                right - tempView.measuredWidth,
                top,
                right,
                top + tempView.measuredHeight
            )
            parentNode.layoutConsumed += (if (nodeIndex != (parentNode.children!!.size - 1)) tempNode.placeHeight + nodeVerSpace else tempNode.placeHeight)

            _tempQueue.addAll(tempNode.children ?: emptyList())
        }
        // </editor-fold>
    }
}
