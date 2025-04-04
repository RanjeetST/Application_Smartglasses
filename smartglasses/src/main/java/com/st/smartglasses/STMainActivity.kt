package com.st.smartglasses

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.st.BlueNRG.fwUpgrade.BlueNRGAdvertiseFilter
import com.st.BlueNRG.fwUpgrade.feature.BlueNRGOTASupport
import com.st.BlueSTSDK.Features.standardCharacteristics.StdCharToFeatureMap
import com.st.BlueSTSDK.Node
import com.st.BlueSTSDK.Node.State
import com.st.BlueSTSDK.Utils.BLENodeDefines
import com.st.BlueSTSDK.Utils.ConnectionOption
import com.st.BlueSTSDK.Utils.NodeScanActivity
import com.st.BlueSTSDK.Utils.SearchSpecificNode
import com.st.BlueSTSDK.gui.NodeConnectionService
import com.st.BlueSTSDK.gui.fwUpgrade.fwVersionConsole.RetrieveNodeVersion
import com.st.STM32WB.fwUpgrade.feature.STM32OTASupport
import com.st.STM32WB.p2pDemo.Peer2PeerDemoConfiguration
import com.st.clab.bleSensor.fwUpgradeChecker.CheckFirmwareUpgradePresences
import kotlin.math.pow

class STMainActivity : NodeScanActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "STMainActivity"
        private const val SEARCH_NODE_TIMEOUT_MS = 10000
        private var pairingPin: ByteArray? = null
        private var isDemoRunning = false
    }

    private var nodeTag: String? = null
    private var pendingIntent: PendingIntent? = null
    private var nfcAdapter: NfcAdapter? = null
    private var clearDeviceCache = true
    private var keepConnectionOpen = false

    private var progressDialog: StatusDialog? = null
    private var connectedNode: Node? = null

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        progressDialog?.dismiss()
        NodeConnectionService.disconnectAllNodes(this)
    }

    private val connectionHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            connectedNode?.apply {
                addNodeStateListener(object : Node.NodeStateListener {
                    override fun onStateChange(node: Node, newState: State, prevState: State) {
                        if (newState == State.Connected) {
                            val intent = Intent(this@STMainActivity, SelectModeActivity::class.java).apply {
                                putExtra("tag", node.tag)
                            }
                            startActivity(intent)
                            node.removeNodeStateListener(this)
                            progressDialog?.dismiss()
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            progressDialog = null
                            finish()
                        }
                    }
                })
                stopNodeDiscovery()
                NodeConnectionService.connect(this@STMainActivity, this, buildConnectionOption(this))
                timeoutHandler.removeCallbacks(timeoutRunnable)
                timeoutHandler.postDelayed(timeoutRunnable, 15000)
            }
            nodeTag = null
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")
        if (isDemoRunning) return

        intent?.data?.getQueryParameter("Add")?.let {
            nodeTag = it
        }
    }

    private fun convertPinToBytes(pin: Int): ByteArray {
        require(pin in 0..999999) { "Pin must have at maximum 6 digits" }
        return ByteArray(6) { i ->
            ('0'.code + (pin / (10.0.pow(5 - i).toInt()) % 10)).toByte()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isDemoRunning) {
            finish()
            return
        }

        setContentView(R.layout.activity_stmain)
        window.statusBarColor = getColor(R.color.blue)

        findViewById<View>(R.id.connect).setOnClickListener(this)

        nodeTag = intent?.data?.getQueryParameter("Add")

    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")

        if (nodeTag != null) {
            isDemoRunning = true
            NodeConnectionService.disconnectAllNodes(this)
            SearchNodeTask(this, SEARCH_NODE_TIMEOUT_MS).execute(nodeTag)
            progressDialog = StatusDialog.with(this)
                    .setCancelable(false)
                    .setPrompt("Connecting $nodeTag")
                    .setType(StatusDialog.Type.PROGRESS)
                    .show()
        } else {
            Log.d(TAG, "No nodeTag found in URI")
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.connect -> startActivity(Intent(this, STScanActivity::class.java))
        }
    }

    private inner class SearchNodeTask(activity: NodeScanActivity, timeoutMs: Int) :
            SearchSpecificNode(activity, timeoutMs) {

        override fun onPostExecute(result: Node?) {
            isDemoRunning = false
            if (result == null) {
                NodeConnectionService.disconnectAllNodes(this@STMainActivity)
                progressDialog?.dismiss()
                Toast.makeText(this@STMainActivity, "Please try again", Toast.LENGTH_SHORT).show()
                nodeTag = null
            } else {
                connectedNode = result
                connectionHandler.sendEmptyMessage(0x001)
            }
        }
    }

    private fun setupPrivateServices(node: Node) {
        val versionConsole = RetrieveNodeVersion().apply {
            addListener(CheckFirmwareUpgradePresences(this@STMainActivity))
            node.addNodeStateListener(this)
        }
    }

    private fun buildConnectionOption(node: Node): ConnectionOption {
        setupPrivateServices(node)

        val builder = ConnectionOption.builder()
                .resetCache(clearDeviceCache)
                .enableAutoConnect(keepConnectionOpen)
                .setFeatureMap(STM32OTASupport.getOTAFeatures())
                .setFeatureMap(BlueNRGOTASupport.getOTAFeatures())
                .setFeatureMap(StdCharToFeatureMap())

        if (Peer2PeerDemoConfiguration.isValidDeviceNode(node)) {
            builder.setFeatureMap(Peer2PeerDemoConfiguration.getCharacteristicMapping())
        }

        try {
            if (node.type != Node.Type.SENSOR_TILE_BOX) {
                node.enableNodeServer(BLENodeDefines.FeatureCharacteristics.getDefaultExportedFeature())
            }
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "BLE server not started", Toast.LENGTH_SHORT).show()
        }

        when (node.advertiseInfo) {
            is BlueNRGAdvertiseFilter.BlueNRGAdvertiseInfo -> Log.d(TAG, "Device is BlueNRG")
        }

        return builder.build()
    }

    override fun onPause() {
        super.onPause()
        progressDialog?.dismiss()
        progressDialog = null
        nodeTag = null
        Log.d(TAG, "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
