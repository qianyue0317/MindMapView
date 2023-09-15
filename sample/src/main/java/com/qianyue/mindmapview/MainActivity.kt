package com.qianyue.mindmapview

import android.graphics.Color
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.qianyue.mindmapview.model.MindMapNode
import com.qianyue.mindmapview.util.NodeAdapter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = fakeData()
        val adapter = MindAdapter()
        adapter.root = root
        val mindView = findViewById<MindMapView>(R.id.mind_map_view)

        mindView.adapter = adapter

        mindView.postDelayed({
            (mindView.parent as MindMapContainerView).resetPosition()
        }, 5000)
    }

    private fun fakeData(): MindMapNode<String> {
        val root = MindMapNode<String>("1111", null)
        val child1 = MindMapNode<String>("1-222", root)
        val child11 = MindMapNode<String>("1-222--111", child1)
        val child12 = MindMapNode<String>("1-222--222", child1)
        val child13 = MindMapNode<String>("1-222--333", child1)
        child1.children = listOf(child11, child12, child13)

        val child2 = MindMapNode<String>("1-333", root)
        val child21 = MindMapNode<String>("2-222--111", child2)
        val child22 = MindMapNode<String>("2-222--222", child2)
        child2.children = listOf(child21, child22)

        // 换行测试
        val child3 = MindMapNode<String>("3-222--333", root)
        val child31 = MindMapNode<String>("3-333--111", child3)
        val child32 = MindMapNode<String>("3-333--222", child3)

        val child311 = MindMapNode<String>("3-33311--111", child31)
        val child312 = MindMapNode<String>("3-33312--111", child31)
        val child313 = MindMapNode<String>("3-33313--111", child31)
        val child314 = MindMapNode<String>("3-33314--111", child31)
        child31.children = listOf(child311, child312, child313, child314)


        child3.children = listOf(child31, child32)

        root.children = listOf(child1, child2, child3)
        return root
    }

    private fun fakeData2() :MindMapNode<String> {
        val root = MindMapNode<String>("1111", null)
        val child1 = MindMapNode<String>("1-222", root)
        val child11 = MindMapNode<String>("1-222--111", child1)
        val child12 = MindMapNode<String>("1-222--222", child1)
        val child13 = MindMapNode<String>("1-222--333", child1)
        child1.children = listOf(child11, child12, child13)

        val child2 = MindMapNode<String>("1-333", root)
        val child21 = MindMapNode<String>("2-222--111", child2)
        val child22 = MindMapNode<String>("2-222--222", child2)
        child2.children = listOf(child21, child22)

        // 换行测试
        val child3 = MindMapNode<String>("3-222--333", root)
        val child31 = MindMapNode<String>("3-333--111", child3)
        val child32 = MindMapNode<String>("3-333--222", child3)

        val child311 = MindMapNode<String>("3-33311--111", child31)
        val child312 = MindMapNode<String>("3-33312--111", child31)
        val child313 = MindMapNode<String>("3-33313--111", child31)
        val child314 = MindMapNode<String>("3-33314--111", child31)
//        child31.children = listOf(child311, child312, child313, child314)


        child3.children = listOf(child31, child32)

        root.children = listOf(child1, child2)
        return root
    }

    inner class MindAdapter : NodeAdapter<String>() {
        override fun getView(view: View?, level: Int, posInLevel: Int, t: String): View {
            if (level == 2 && posInLevel == 1) {
                return ImageView(this@MainActivity).apply {
                    setImageResource(R.mipmap.ic_launcher)
                }
            }
            if (view != null) {
                (view as TextView).text = t
                return view
            }
           return TextView(this@MainActivity).apply {
               textSize = 18f
               setTextColor(Color.BLUE)
               text = t
               setPaddingRelative(16, 16, 16, 16)
               setBackgroundColor(Color.GREEN)

               setOnClickListener {
                   Toast.makeText(this@MainActivity, "这是:$t", Toast.LENGTH_SHORT).show()
               }
           }
        }
    }
}