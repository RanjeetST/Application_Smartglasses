package com.st.smartglasses

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.FeatureQVARNew
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.youth.banner.Banner
import com.youth.banner.indicator.CircleIndicator
import com.youth.banner.listener.OnBannerListener

class QVARActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "QVARActivity"
        private const val NODE_TAG_KEY = "tag"
    }

    private var node: Node? = null
    private lateinit var banner: Banner<DataBean, ImageAdapter>
    private lateinit var qvarLeft: ImageView
    private lateinit var qvarRight: ImageView
    private var gestureFeatures: List<FeatureQVARNew> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qvar)
        window.statusBarColor = getColor(R.color.blue)

        val nodeTag = intent.getStringExtra(NODE_TAG_KEY) ?: ""
        node = Manager.getSharedInstance().getNodeWithTag(nodeTag)

        if (node == null) {
            Log.e(TAG, "Node is null")
        }

        banner = findViewById(R.id.mbanner)
        banner.setAdapter(ImageAdapter(DataBean.getTestData()))
                .addBannerLifecycleObserver(this)
                .setIndicator(CircleIndicator(this))
                .isAutoLoop(false)
                .setOnBannerListener(object : OnBannerListener<DataBean> {
                    override fun OnBannerClick(data: DataBean?, position: Int) {
                        // Optional click action
                    }
                })

        qvarLeft = findViewById(R.id.qvarleft)
        qvarRight = findViewById(R.id.qvarright)
        findViewById<LinearLayout>(R.id.qrbackingesture).setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        node?.let { enableNotification(it) }
    }

    override fun onPause() {
        super.onPause()
        node?.let { disableNotification(it) }
    }

    private fun enableNotification(node: Node) {
        gestureFeatures = node.getFeatures(FeatureQVARNew::class.java)
        Log.d(TAG, "Gesture features found: ${gestureFeatures.isNotEmpty()}")
        if (gestureFeatures.isNotEmpty()) {
            Toast.makeText(this, "QVAR enabled", Toast.LENGTH_SHORT).show()
            gestureFeatures.forEach {
                it.addFeatureListener(gestureListener)
                node.enableNotification(it)
            }
        }
    }

    private fun disableNotification(node: Node) {
        gestureFeatures.forEach {
            it.removeFeatureListener(gestureListener)
            node.disableNotification(it)
        }
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.qrbackingesture) finish()
    }

    private val gestureListener = Feature.FeatureListener { _, sample ->
        runOnUiThread {
            val currentItem = banner.currentItem
            when (FeatureQVARNew.getAudioClass(sample)) {
                FeatureQVARNew.AudioClass.LEFT -> {
                    qvarLeft.setImageResource(R.drawable.letfyellow)
                    qvarRight.setImageResource(R.drawable.rightwhite)
                    banner.currentItem = if (currentItem == 1) 3 else currentItem - 1
                }
                FeatureQVARNew.AudioClass.RIGHT -> {
                    qvarLeft.setImageResource(R.drawable.leftwhite)
                    qvarRight.setImageResource(R.drawable.rightyellow)
                    banner.currentItem = if (currentItem == 3) 1 else currentItem + 1
                }
                else -> {}
            }
        }
    }
}
