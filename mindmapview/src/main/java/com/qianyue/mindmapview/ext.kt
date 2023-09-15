package com.qianyue.mindmapview

import android.graphics.Point
import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import kotlin.math.sqrt

/**
 * @author QianYue
 * @since 2023/9/14
 */
fun Point.distance(other: Point): Double {
    return sqrt(((other.x - x) * (other.x - x) + (other.y - y) * (other.y - y)).toDouble())
}

fun PointF.distance(other: PointF): Double {
    return sqrt(((other.x - x) * (other.x - x) + (other.y - y) * (other.y - y)).toDouble())
}


fun Point.distance(otherX: Int, otherY: Int): Double {
    return sqrt(((otherX - x) * (otherX - x) + (otherY - y) * (otherY - y)).toDouble())
}


fun PointF.distance(otherX: Float, otherY: Float): Double {
    return sqrt(((otherX - x) * (otherX - x) + (otherY - y) * (otherY - y)).toDouble())
}

fun ViewGroup.findChild(block: (View) -> Boolean): View? {
    forEach {
        if (block(it)) {
            return it
        }
    }
    return null
}