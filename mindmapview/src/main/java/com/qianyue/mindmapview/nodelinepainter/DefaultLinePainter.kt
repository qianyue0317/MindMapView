package com.qianyue.mindmapview.nodelinepainter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.qianyue.mindmapview.NodeLinePainter
import com.qianyue.mindmapview.R

/**
 * @author QianYue
 * @since 2023/9/14
 */
class DefaultLinePainter(val context: Context) : NodeLinePainter {

    private val _path: Path = Path()

    private val cornerR: Float = context.resources.getDimensionPixelOffset(R.dimen.mind_map_line_corner_radius).toFloat()

    private val _linePaint = Paint().apply {
        strokeWidth = context.resources.getDimensionPixelOffset(R.dimen.mind_map_line_stroke_width).toFloat()
        color = context.getColor(R.color.mind_map_line_color)
        style = Paint.Style.STROKE
    }

    override fun drawLine(canvas: Canvas, nodeView: View, parentNodeView: View) {

        _path.reset()

        _path.apply {

            var currentX = 0f
            var currentY = 0f

            val dx = nodeView.left - parentNodeView.right
            val dy =
                nodeView.top + nodeView.measuredHeight / 2 - (parentNodeView.top + parentNodeView.measuredHeight / 2)

            moveTo(
                parentNodeView.right.toFloat().apply { currentX = this },
                (parentNodeView.top + parentNodeView.measuredHeight / 2).toFloat()
                    .apply { currentY = this }
            )


            lineTo(currentX + dx / 2 - cornerR, currentY)

            if (dy < 0) {
                arcTo(
                    currentX + dx / 2 - cornerR.times(2),
                    currentY - cornerR.times(2),
                    currentX + dx / 2,
                    currentY,
                    90f,
                    -90f,
                    true
                )
                currentX += (dx / 2).toFloat()
                lineTo(
                    currentX,
                    (nodeView.top + nodeView.measuredHeight / 2 + cornerR)
                        .apply { currentY = this })
            } else if (dy > 0) {
                arcTo(
                    currentX + dx / 2 - cornerR.times(2),
                    currentY,
                    currentX + dx / 2,
                    currentY + cornerR.times(2),
                    -90f,
                    90f,
                    true
                )
                currentX += (dx / 2).toFloat()
                lineTo(
                    currentX,
                    (nodeView.top + nodeView.measuredHeight / 2 - cornerR)
                        .apply { currentY = this })
            } else {
                lineTo(currentX + dx, (nodeView.top + nodeView.measuredHeight / 2).toFloat())
                return@apply
            }


            if (dy < 0) {
                arcTo(
                    currentX,
                    currentY - cornerR,
                    currentX + cornerR.times(2),
                    currentY + cornerR,
                    180f,
                    90f,
                    true
                )
            } else {
                arcTo(
                    currentX,
                    currentY - cornerR,
                    currentX + cornerR.times(2),
                    currentY + cornerR,
                    -180f,
                    -90f,
                    true
                )
            }

            lineTo(
                nodeView.left.toFloat(),
                (nodeView.top + nodeView.measuredHeight / 2).toFloat()
            )
        }
        canvas.drawPath(_path, _linePaint)
    }
}