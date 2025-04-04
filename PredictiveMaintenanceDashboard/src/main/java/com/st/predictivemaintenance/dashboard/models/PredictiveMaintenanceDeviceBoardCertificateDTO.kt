package com.st.predictivemaintenance.dashboard.models

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

data class PredictiveMaintenanceDeviceBoardCertificateDTO(
    @SerializedName("DeviceId") val deviceId : String,
    @SerializedName("Certificate") val certificate : String,
    @SerializedName("PrivateKey") val privateKey : String,
)

fun PredictiveMaintenanceDevice.getBoardCertificateDTO(): String {
    val boardCertificate = PredictiveMaintenanceDeviceBoardCertificateDTO(
        deviceId,
        certificate,
        privateKey
    )

    val gson = GsonBuilder()
        .disableHtmlEscaping() // avoid escaping HTML characters
        .create()

    return gson.toJson(boardCertificate)
}