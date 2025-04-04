package com.st.smartglasses

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.FeaturePosture
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node

class GestureActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "GestureActivity"
        private const val NODE_TAG_KEY = "tag"
        private const val INITIAL_NUM = 1
    }

    private var node: Node? = null
    private lateinit var backButton: LinearLayout
    private lateinit var img: ImageView
    private var gestureFeatures: List<FeaturePosture> = emptyList()

    private var dialog: Dialog? = null
    private val handler = Handler()

    private var hasResult = false
    private var gestureCounts = mutableMapOf<FeaturePosture.AudioClass, Int>().withDefault { 0 }
    private var isDebug = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gesture)
        window.statusBarColor = getColor(R.color.blue)

        val nodeTag = intent.getStringExtra(NODE_TAG_KEY) ?: ""
        node = Manager.getSharedInstance().getNodeWithTag(nodeTag)
        if (node == null) {
            Log.e(TAG, "Node is null")
        }

        img = findViewById(R.id.gestureimg)
        backButton = findViewById(R.id.backingesture)
        backButton.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        node?.let { enableNotification(it) }
    }

    override fun onPause() {
        super.onPause()
        disableNotification(node)
        dialog?.dismiss()
        dialog = null
        handler.removeCallbacks(clearResultRunnable)
        hasResult = false
        resetGestureCounts()
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.backingesture) finish()
    }

    private fun enableNotification(node: Node) {
        gestureFeatures = node.getFeatures(FeaturePosture::class.java)
        if (gestureFeatures.isNotEmpty()) {
            Toast.makeText(this, "Gesture enabled", Toast.LENGTH_SHORT).show()
            gestureFeatures.forEach {
                it.addFeatureListener(gestureListener)
                node.enableNotification(it)
            }
        }
    }

    private fun disableNotification(node: Node?) {
        gestureFeatures.forEach {
            it.removeFeatureListener(gestureListener)
            node?.disableNotification(it)
        }
    }

    private val gestureListener = Feature.FeatureListener { _, sample ->
        val gesture = FeaturePosture.getAudioClass(sample)

        if (gesture == FeaturePosture.AudioClass.UNKNOWN) return@FeatureListener
        if (hasResult) return@FeatureListener

        val count = gestureCounts.getValue(gesture) + 1
        gestureCounts[gesture] = count

        if (count >= INITIAL_NUM) {
            runOnUiThread {
                hasResult = true
                if (isDebug) showQrcodeImage(gesture) else showQrcodeDialog(gesture)
                resetGestureCounts()
            }
        }
    }

    private val clearResultRunnable = Runnable {
        if (isDebug) {
            img.setImageDrawable(null)
            img.visibility = View.GONE
        } else {
            dialog?.dismiss()
        }
        hasResult = false
    }

    private fun showQrcodeImage(status: FeaturePosture.AudioClass) {
        val drawableRes = when (status) {
            FeaturePosture.AudioClass.LIKE -> R.drawable.nice2
            FeaturePosture.AudioClass.DISLIKE -> R.drawable.bad2
            FeaturePosture.AudioClass.FIST -> R.drawable.fist
            FeaturePosture.AudioClass.FLATHAND -> R.drawable.flathand
            FeaturePosture.AudioClass.OK -> R.drawable.hand_ok
            FeaturePosture.AudioClass.LOVE -> R.drawable.love
            FeaturePosture.AudioClass.CROSSHANDS -> R.drawable.crosshand
            else -> null
        }

        if (drawableRes != null) {
            img.setImageResource(drawableRes)
            img.visibility = View.VISIBLE
        } else {
            img.setImageDrawable(null)
            img.visibility = View.GONE
        }

        handler.postDelayed(clearResultRunnable, 800)
    }

    private fun showQrcodeDialog(status: FeaturePosture.AudioClass) {
        if (dialog == null) {
            dialog = Dialog(this, R.style.QrCodeDialog).apply { setCancelable(false) }
        }

        val view = View.inflate(applicationContext, R.layout.gesture_dialog, null)
        dialog?.setContentView(view)
        val dialogImg = view.findViewById<ImageView>(R.id.iv_dialog_qrcode)

        val drawableRes = when (status) {
            FeaturePosture.AudioClass.LIKE -> R.drawable.nice
            FeaturePosture.AudioClass.DISLIKE -> R.drawable.bad
            else -> null
        }
        drawableRes?.let { dialogImg.setImageResource(it) }

        dialog?.show()
        handler.postDelayed(clearResultRunnable, 500)
    }

    private fun resetGestureCounts() {
        gestureCounts = mutableMapOf<FeaturePosture.AudioClass, Int>().withDefault { 0 }
    }
}
