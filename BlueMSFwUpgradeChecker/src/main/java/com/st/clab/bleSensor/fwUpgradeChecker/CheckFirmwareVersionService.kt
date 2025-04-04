package com.st.clab.bleSensor.fwUpgradeChecker

import android.app.Service
import android.content.Intent
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.BlueSTSDK.gui.fwUpgrade.download.DownloadFwFileService
import com.st.BlueSTSDK.gui.fwUpgrade.fwVersionConsole.FwVersionBoard
import com.st.clab.bleSensor.fwUpgradeChecker.repository.DeviceFirmware
import com.st.clab.bleSensor.fwUpgradeChecker.repository.DeviceFirmwareRepository
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

private const val CHECK_LAST_FW_VERSION = "com.st.clab.bleSensor.fwUpgradeChecker.action.CHECK_LAST_FW_VERSION"

private const val CURRENT_FW_VERSION = "com.st.clab.bleSensor.fwUpgradeChecker.extra.CURRENT_FW_VERSION"
private const val NODE_TAG = "com.st.clab.bleSensor.fwUpgradeChecker.extra.NODE_TAG"

class CheckFirmwareVersionService : Service(), CoroutineScope {
    override fun onBind(p0: Intent?): IBinder? = null

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val boardVersion = intent?.getParcelableExtra<FwVersionBoard>(CURRENT_FW_VERSION)
        val nodeTag = intent?.getStringExtra(NODE_TAG)
        if(intent?.action != CHECK_LAST_FW_VERSION || boardVersion == null || nodeTag == null){
            stopSelf()
            return START_NOT_STICKY
        }

        Manager.getSharedInstance().getNodeWithTag(nodeTag)?.let {
            checkLastFwVersion(it,boardVersion)
        }
        return START_REDELIVER_INTENT
    }

    private fun checkLastFwVersion(node: Node, boardVersion: FwVersionBoard) {
        val repo = DeviceFirmwareRepository.getInstance(this, BuildConfig.DB_BASE_URL)
        val context = this
        runBlocking{
            val lastFw = repo.getLastFwFor(node.typeId,boardVersion.name,boardVersion.mcuType!!)
            if(lastFw!=null && boardVersion<lastFw.version){
                val uri = buildFwUri(lastFw)
                DownloadFwFileService.displayAvailableFwNotification(context, uri)
                val intent = Intent(NEW_FW_AVAILABLE_ACTION).apply {
                    putExtra(NEW_FW_LOCATION_URI,uri)
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }
            stopSelf()
        }
    }

    private fun buildFwUri(fwInfo:DeviceFirmware):Uri{
        val fullUrl = BuildConfig.DB_BASE_URL+fwInfo.relativeFwPath
        return Uri.parse(fullUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob.cancel()
    }

    companion object {
        @JvmStatic
        fun checkLastFwVersion(context: Context, node: Node, fwVersionBoard: FwVersionBoard) {
            val intent = Intent(context, CheckFirmwareVersionService::class.java).apply {
                action = CHECK_LAST_FW_VERSION
                putExtra(CURRENT_FW_VERSION, fwVersionBoard)
                putExtra(NODE_TAG,node.tag)
            }
            context.startService(intent)
        }

        val NEW_FW_AVAILABLE_ACTION = CheckFirmwareVersionService::class.java.name+".NEW_FW_AVAILABLE_ACTION"
        val NEW_FW_LOCATION_URI = CheckFirmwareVersionService::class.java.name+".NEW_FW_LOCATION_URI"

        val NEW_FW_AVAILABLE_INTENT_FILTER = IntentFilter().apply {
            addAction(NEW_FW_AVAILABLE_ACTION)
        }
    }
}
