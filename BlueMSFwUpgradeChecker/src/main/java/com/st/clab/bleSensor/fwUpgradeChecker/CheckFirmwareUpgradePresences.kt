package com.st.clab.bleSensor.fwUpgradeChecker

import android.content.Context
import com.st.BlueSTSDK.Node
import com.st.BlueSTSDK.Utils.FwVersion
import com.st.BlueSTSDK.gui.fwUpgrade.fwVersionConsole.FwVersionBoard
import com.st.BlueSTSDK.gui.fwUpgrade.fwVersionConsole.RetrieveNodeVersion

class CheckFirmwareUpgradePresences(private val ctx:Context) : RetrieveNodeVersion.OnVersionRead {

    override fun onVersionRead(node: Node, version: FwVersion?) {
        if(version == null)
            return
        if (version !is FwVersionBoard){
            return
        }
        CheckFirmwareVersionService.checkLastFwVersion(ctx,node,version)
    }

}