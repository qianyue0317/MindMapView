package com.qianyue.mindmapview

/**
 * @author QianYue
 * @since 2023/9/18
 */
interface NodeLayoutStrategy {
    fun init(layoutHelper: MindMapContentView.LayoutHelper)

    fun onMeasure(): Long

    fun onLayout()

}