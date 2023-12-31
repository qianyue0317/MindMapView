package com.qianyue.mindmapview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.qianyue.mindmapview.layoutstrategy.BothSideLayoutStrategy
import com.qianyue.mindmapview.layoutstrategy.RightLayoutStrategy
import com.qianyue.mindmapview.model.MindMapNode
import com.qianyue.mindmapview.nodelinepainter.BothSideLinePainter
import com.qianyue.mindmapview.nodelinepainter.DefaultLinePainter
import com.qianyue.mindmapview.util.NodeAdapter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = fakeData()
        val adapter = MindAdapter()
        adapter.root = root
        val mindView = findViewById<MindMapView>(R.id.mind_map_view)
        mindView.apply {
            addView(
                Button(this@MainActivity).apply {
                    text = "fitCenter"
                    setOnClickListener {
                        mindView.fitCenter()
                    }
                    setBackgroundColor(Color.YELLOW)
                },
                FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.END or Gravity.BOTTOM
                ).apply {
                    this.bottomMargin = 30
                    this.marginEnd = 30
                })

            addView(
                Button(this@MainActivity).apply {
                    text = "两侧摆放"
                    setOnClickListener {
                        mindView.setLayoutStrategy(BothSideLayoutStrategy())
                        mindView.setNodeLinePainter(BothSideLinePainter(context))
                    }
                    setBackgroundColor(Color.YELLOW)
                },
                FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.END or Gravity.BOTTOM
                ).apply {
                    this.bottomMargin = 30
                    this.marginEnd = 400
                })

            addView(Button(this@MainActivity).apply {
                text = "右侧摆放"
                setOnClickListener {
                    mindView.setLayoutStrategy(RightLayoutStrategy())
                    mindView.setNodeLinePainter(DefaultLinePainter(context))
                }
                setBackgroundColor(Color.YELLOW)
            },
                FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.END or Gravity.BOTTOM
                ).apply {
                    this.bottomMargin = 30
                    this.marginEnd = 800
                })
        }
        mindView.setAdapter(adapter)
        // 设置Gravity
//        mindView.setContentGravity(Gravity.START)
        // 设置缩放到父容器大小，类似ImageView的 FIT_CENTER
//        mindView.fitCenter()
        // 设置是否可缩放、移动
//        mindView.enableTouch = false

        // <editor-fold desc="这里是使用原始的导图控件，没有触摸事件处理，用户也可以自行定义容器，参考MindMapView">
        val root2 = fakeData2()
        val adapter2 = MindAdapter()
        adapter2.root = root2
        val mindContentView = findViewById<MindMapContentView>(R.id.mind_content_view)
        mindContentView.adapter = adapter2
        // </editor-fold>

//        mindView.postDelayed({
//                             mindView.fitScale()
//        }, 5000)
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
        child21.children = listOf(MindMapNode("2-222-111-111", child21))

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


        // 换行测试
        val child4 = MindMapNode<String>("4-111--222", root)
        val child41 = MindMapNode<String>("4-444--111", child4)
        val child42 = MindMapNode<String>("4-444--222", child4)

        child4.children = listOf(child41, child42)


        root.children = listOf(child1, child2, child3, child4)
        return root
    }

    private fun fakeData2(): MindMapNode<String> {
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
            if (view != null) return view
            if (level == 2 && posInLevel == 2) {
                return ImageView(this@MainActivity).apply {
                    setImageResource(R.mipmap.ic_launcher)
                }
            }
            return TextView(this@MainActivity).apply {
                textSize = 18f
                setTextColor(Color.BLUE)
                text = t
                setPaddingRelative(16, 16, 16, 16)
                setBackgroundColor(if (level == 0 && posInLevel == 0) Color.RED else Color.GREEN)

                setOnClickListener {
                    Toast.makeText(this@MainActivity, "这是:$t", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}