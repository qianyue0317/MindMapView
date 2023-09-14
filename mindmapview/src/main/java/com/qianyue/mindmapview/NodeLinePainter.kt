package com.qianyue.mindmapview

import android.graphics.Canvas
import android.view.View

/**
 * 绘制连接父子节点的线
 *
 * @author QianYue
 * @since 2023/9/14
 */
interface NodeLinePainter {
    fun drawLine(canvas: Canvas, nodeView: View, parentNodeView: View)
}