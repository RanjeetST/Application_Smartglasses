package com.st.predictivemaintenance.dashboard.models

import com.google.gson.annotations.SerializedName
import com.st.BlueSTSDK.Node

data class PredictiveMaintenanceDevice(
    @SerializedName("DeviceId") val deviceId : String,
    @SerializedName("Name") val name : String,
    @SerializedName("DeviceType") val deviceType : Node.Type,
    @SerializedName("Certificate") val certificate : String,
    @SerializedName("PrivateKey") val privateKey : String
)