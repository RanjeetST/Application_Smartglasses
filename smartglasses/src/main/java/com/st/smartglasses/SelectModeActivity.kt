package com.st.smartglasses

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node

class SelectModeActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "SelectModeActivity"
        private const val NODE_TAG_KEY = "tag"
    }

    private var node: Node? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selectmode)
        window.statusBarColor = getColor(R.color.blue)

        val nodeTag = intent.getStringExtra(NODE_TAG_KEY) ?: ""
        node = Manager.getSharedInstance().getNodeWithTag(nodeTag)

        if (node == null) {
            Log.e(TAG, "Node is null")
        }

        findViewById<View>(R.id.data).setOnClickListener(this)
        findViewById<View>(R.id.qrcode).setOnClickListener(this)
        findViewById<View>(R.id.gesture).setOnClickListener(this)
        findViewById<View>(R.id.qvar).setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (node == null) return

        val nextIntent = when (view.id) {
            R.id.data -> Intent(this, ShowDataActivity::class.java)
            R.id.qrcode -> Intent(this, QRcodeActivity::class.java)
            R.id.gesture -> Intent(this, GestureActivity::class.java)
            R.id.qvar -> Intent(this, QVARActivity::class.java)
            else -> null
        }

        nextIntent?.let {
            it.putExtra(NODE_TAG_KEY, intent.getStringExtra(NODE_TAG_KEY))
            startActivity(it)
        } ?: Toast.makeText(this, "Unknown selection", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optionally disconnect node or release resources
    }
}
