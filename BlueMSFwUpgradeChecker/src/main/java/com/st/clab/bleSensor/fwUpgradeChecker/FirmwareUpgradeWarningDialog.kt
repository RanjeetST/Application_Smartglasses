package com.st.clab.bleSensor.fwUpgradeChecker


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.st.BlueSTSDK.gui.fwUpgrade.download.DownloadFwFileService

class FirmwareUpgradeWarningDialog(private val activity: FragmentActivity) : LifecycleObserver {


    private var onBroadcastReceived = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action != CheckFirmwareVersionService.NEW_FW_AVAILABLE_ACTION)
                return
            val fwLocation = intent.getParcelableExtra<Uri>(CheckFirmwareVersionService.NEW_FW_LOCATION_URI)
            val dialog = DownloadFwFileService.buildAvailableFwNotificationDialog(activity,fwLocation,false)
            dialog.show(activity.supportFragmentManager,"fwDialog")

        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun registerBroadcastListener() {
        LocalBroadcastManager.getInstance(activity)
                .registerReceiver(onBroadcastReceived,
                        CheckFirmwareVersionService.NEW_FW_AVAILABLE_INTENT_FILTER)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun unRegisterBroadcastListener() {
        LocalBroadcastManager.getInstance(activity)
                .unregisterReceiver(onBroadcastReceived)
    }

}