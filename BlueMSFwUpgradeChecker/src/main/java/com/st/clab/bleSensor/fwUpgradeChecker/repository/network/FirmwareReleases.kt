package com.st.clab.bleSensor.fwUpgradeChecker.repository.network

import com.google.gson.annotations.SerializedName
import com.st.clab.bleSensor.fwUpgradeChecker.repository.DeviceFirmware
import java.util.*

internal data class FirmwareReleases(
        @SerializedName("lastUpgrade")
        val lastUpgrade:Date,
        @SerializedName("firmwares")
        val firmwares:List<DeviceFirmware>
)