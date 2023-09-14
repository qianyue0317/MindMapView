package com.qianyue.mindmapview.util

import android.view.View
import com.qianyue.mindmapview.model.MindMapNode

/**
 * @author QianYue
 * @since 2023/9/6
 */
abstract class NodeAdapter<T>() {
    var root: MindMapNode<T>? = null
        set(value) {
            field = value
            observer?.notifyChange()
        }

    var observer: Observer? = null
        set(value) {
            field = value
            value?.notifyChange()
        }

    @Suppress("UNCHECKED_CAST")
    fun getView(view: View?, level: Int, posInLevel: Int, node: MindMapNode<*>): View {
        return getView(view, level, posInLevel, node.value as T)
    }

    abstract fun getView(view: View?, level: Int, posInLevel: Int, t: T): View

    interface Observer {
        fun notifyChange()
    }
}