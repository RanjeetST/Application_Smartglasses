package com.st.smartglasses

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.st.BlueNRG.fwUpgrade.BlueNRGAdvertiseFilter
import com.st.BlueNRG.fwUpgrade.feature.BlueNRGOTASupport
import com.st.BlueSTSDK.Features.standardCharacteristics.StdCharToFeatureMap
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.BlueSTSDK.Utils.BLENodeDefines
import com.st.BlueSTSDK.Utils.ConnectionOption
import com.st.BlueSTSDK.Utils.NodeScanActivity
import com.st.BlueSTSDK.gui.NodeConnectionService
import com.st.BlueSTSDK.gui.fwUpgrade.fwVersionConsole.RetrieveNodeVersion
import com.st.STM32WB.fwUpgrade.feature.STM32OTASupport
import com.st.STM32WB.p2pDemo.Peer2PeerDemoConfiguration
import com.st.clab.bleSensor.fwUpgradeChecker.CheckFirmwareUpgradePresences

class STScanActivity : NodeScanActivity(), AdapterView.OnItemClickListener {

    companion object {
        private const val TAG = "STScanActivity"
        private const val NODE_TAG_KEY = "tag"
        private const val SCAN_TIME_MS = 20_000
    }

    private lateinit var nodeListView: AbsListView
    private lateinit var swipeLayout: SwipeRefreshLayout
    private var progressDialog: StatusDialog? = null
    private val handler = Handler()
    private val timeoutRunnable = Runnable {
        progressDialog?.dismiss()
        progressDialog = null
        Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show()
    }

    private var adapter: NodeArrayAdapter? = null
    private var currentNode: Node? = null
    private var clearDeviceCache = true
    private var keepConnectionOpen = false

    private val nodeStateListener = object : Node.NodeStateListener {
        override fun onStateChange(node: Node, newState: Node.State, prevState: Node.State) {
            Log.d(TAG, "State changed: $newState")
            if (newState == Node.State.Connected) {
                startActivity(Intent(this@STScanActivity, SelectModeActivity::class.java).apply {
                    putExtra(NODE_TAG_KEY, node.tag)
                })
                node.removeNodeStateListener(this)
                progressDialog?.dismiss()
                progressDialog = null
                handler.removeCallbacks(timeoutRunnable)
            }
        }
    }

    private val discoveryListener = object : Manager.ManagerListener {
        override fun onDiscoveryChange(manager: Manager, enabled: Boolean) {
            if (!enabled) {
                runOnUiThread {
                    stopNodeDiscovery()
                }
            }
        }

        override fun onNodeDiscovered(manager: Manager, node: Node) {
            Log.d(TAG, "Node discovered: ${node.tag}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stscan)
        window.statusBarColor = getColor(R.color.blue)

        nodeListView = findViewById(R.id.nodeListView)
        swipeLayout = findViewById(R.id.mswiperRefreshDeviceList)

        adapter = NodeArrayAdapter(this).apply {
            addAll(mManager.nodes)
        }
        nodeListView.adapter = adapter
        nodeListView.onItemClickListener = this

        swipeLayout.setOnRefreshListener {
            resetNodeList()
            startNodeDiscovery()
        }

        swipeLayout.setProgressBackgroundColorSchemeColor(
                resources.getColor(com.st.BlueSTSDK.gui.R.color.swipeColor_background)
        )

        swipeLayout.setColorSchemeResources(
                com.st.BlueSTSDK.gui.R.color.swipeColor_1,
                com.st.BlueSTSDK.gui.R.color.swipeColor_2,
                com.st.BlueSTSDK.gui.R.color.swipeColor_3,
                com.st.BlueSTSDK.gui.R.color.swipeColor_4
        )

        swipeLayout.setSize(SwipeRefreshLayout.DEFAULT)
    }

    override fun onResume() {
        super.onResume()
        resetNodeList()
        startNodeDiscovery()
    }

    override fun onStart() {
        super.onStart()
        NodeConnectionService.disconnectAllNodes(this)
    }

    override fun onPause() {
        stopNodeDiscovery()
        progressDialog?.dismiss()
        progressDialog = null
        super.onPause()
    }

    private fun resetNodeList() {
        mManager.resetDiscovery()
        mManager.removeNodes()
        adapter?.clear()
    }

    override fun stopNodeDiscovery() {
        super.stopNodeDiscovery()
        mManager.removeListener(discoveryListener)
        adapter?.let { mManager.removeListener(it) }
        swipeLayout.isRefreshing = false
    }

    private fun startNodeDiscovery() {
        swipeLayout.isRefreshing = true
        mManager.addListener(discoveryListener)
        NodeConnectionService.disconnectAllNodes(this)
        adapter?.let { mManager.addListener(it) }
        super.startNodeDiscovery(SCAN_TIME_MS)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        stopNodeDiscovery()
        val node = adapter?.getItem(position) ?: return

        node.addNodeStateListener(nodeStateListener)
        NodeConnectionService.connect(this, node, buildConnectionOption(node))

        progressDialog?.dismiss()
        handler.removeCallbacks(timeoutRunnable)

        progressDialog = StatusDialog.with(this)
                .setCancelable(false)
                .setPrompt("Connecting ${node.name}")
                .setType(StatusDialog.Type.PROGRESS)
                .show()

        handler.postDelayed(timeoutRunnable, 15000)
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

        if (node.type != Node.Type.SENSOR_TILE_BOX) {
            try {
                node.enableNodeServer(BLENodeDefines.FeatureCharacteristics.getDefaultExportedFeature())
            } catch (e: IllegalStateException) {
                Toast.makeText(this, "BLE server not started", Toast.LENGTH_SHORT).show()
            }
        }

        when (node.advertiseInfo) {
            is BlueNRGAdvertiseFilter.BlueNRGAdvertiseInfo -> Log.d(TAG, "BlueNRG node")
        }

        return builder.build()
    }

    private fun setupPrivateServices(node: Node) {
        val versionConsole = RetrieveNodeVersion().apply {
            addListener(CheckFirmwareUpgradePresences(this@STScanActivity))
            node.addNodeStateListener(this)
        }
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        // Not used
    }
}
