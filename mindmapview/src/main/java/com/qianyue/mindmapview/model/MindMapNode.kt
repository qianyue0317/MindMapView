package com.qianyue.mindmapview.model

import kotlin.math.max

/**
 * @author QianYue
 * @since 2023/9/6
 *
 * @param parent 除了rootNode，其他node的parent非空
 */
class MindMapNode<T>(val value: T, var parent: MindMapNode<T>?, var children: List<MindMapNode<T>>? = null, var expanded: Boolean = true) {

    // 是算上间隙的距离

    /**          ------- node   ↓
     *          |               ↓
     *          |               ↓
     *          |
     *  node ---             childrenHeight
     *          |
     *          |               ↑
     *          |               ↑
     *          -------- node   ↑
     */
    var childrenHeight: Int = 0

    var selfHeight: Int = 0

    val placeHeight: Int get() = max(childrenHeight, selfHeight)

    var layoutConsumed = 0

    private var _oldPlaceHeight = 0

    val oldPlaceHeight: Int get() = _oldPlaceHeight

    fun saveOldPlaceHeight() {
        _oldPlaceHeight = placeHeight
    }

    fun isLeaf() = children?.isEmpty() ?: true

    fun isExpanded(): Boolean {
        if (!expanded) return false
        var tempNode = parent
        while (tempNode != null ) {
            if (!tempNode.expanded) return false
            tempNode = tempNode.parent
        }
        return true
    }

    fun reset() {
        childrenHeight = 0
        selfHeight = 0
        _oldPlaceHeight = 0
    }

    // 层级，在遍历过程中赋值
    var level: Int = -1

    // 在层中的位置，在遍历过程中赋值
    var posInLevel: Int = -1

    // 在根的左侧还是右侧，遍历过程中赋值 小于0为左，大于0为右
    var leftOrRight = -1
}