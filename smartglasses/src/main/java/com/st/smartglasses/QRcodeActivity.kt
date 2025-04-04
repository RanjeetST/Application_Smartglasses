package com.st.smartglasses

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Features.FeatureBarcode
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node

class QRcodeActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "QRcodeActivity"
        private const val NODE_TAG_KEY = "tag"
    }

    private var node: Node? = null
    private var barcodeFeatures: List<FeatureBarcode> = emptyList()
    private lateinit var cleanButton: Button
    private lateinit var resultText: TextView
    private var hasResult = false
    private val handler = Handler()

    private val barcodeListener = Feature.FeatureListener { _, sample ->
        runOnUiThread {
            val barcode = FeatureBarcode.getBarcodeStringASCII(sample)
            if (!hasResult && barcode.isNotEmpty()) {
                resultText.text = barcode
                resultText.setTextColor(getColor(R.color.yellow))
                resultText.textSize = 34f
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)
        window.statusBarColor = getColor(R.color.blue)

        val nodeTag = intent.getStringExtra(NODE_TAG_KEY) ?: ""
        node = Manager.getSharedInstance().getNodeWithTag(nodeTag)

        if (node == null) {
            Log.e(TAG, "Node is null")
        }

        findViewById<View>(R.id.qrbackingesture).setOnClickListener(this)
        cleanButton = findViewById(R.id.cleanresult)
        cleanButton.setOnClickListener(this)
        resultText = findViewById(R.id.tips)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.qrbackingesture -> finish()
            R.id.cleanresult -> {
                hasResult = false
                resultText.text = ""
            }
        }
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
        barcodeFeatures = node.getFeatures(FeatureBarcode::class.java)
        Log.d(TAG, "Barcode features found: ${barcodeFeatures.isNotEmpty()}")
        if (barcodeFeatures.isNotEmpty()) {
            Toast.makeText(this, "Barcode enabled", Toast.LENGTH_SHORT).show()
            barcodeFeatures.forEach {
                it.addFeatureListener(barcodeListener)
                node.enableNotification(it)
            }
        }
    }

    private fun disableNotification(node: Node) {
        barcodeFeatures.forEach {
            it.removeFeatureListener(barcodeListener)
            node.disableNotification(it)
        }
    }
}
