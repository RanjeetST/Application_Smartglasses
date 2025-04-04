package com.st.predictivemaintenance.dashboard.datasource.remote

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

internal interface PredictiveMaintenanceAPI {

    @POST("devices")
    @Headers("Content-Type: application/json")
    suspend fun addDevice(
        @Header("Authorization") idToken: String,
        @Body payload: PredictiveMaintenanceAPIDeviceAddPayload
    ): Response<ResponseBody>

    @GET("devices")
    suspend fun getDevices(
        @Header("Authorization") idToken: String,
        @Query("accessToken") accessToken: String
    ): Response<ResponseBody>

    @HTTP(method = "DELETE", path = "devices", hasBody = true)
    suspend fun deleteDevice(
        @Header("Authorization") idToken: String,
        @Body payload: PredictiveMaintenanceAPIDeviceDeletePayload
    ): Response<ResponseBody>

}

internal data class PredictiveMaintenanceAPIDeviceAddPayload(
    @SerializedName("thingName") val id: String,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("attributes") val attributes : PredictiveMaintenanceAPIDeviceAttributes,
    @SerializedName("config") val config : PredictiveMaintenanceAPIDeviceConfiguration,
    @SerializedName("endpoint") val endpoint : String = "a31pjrd6x4v4ba-ats.iot.eu-west-1.amazonaws.com",
)

internal data class PredictiveMaintenanceAPIDeviceDeletePayload(
    @SerializedName("thingName") val id: String,
    @SerializedName("accessToken") val accessToken: String,
)

internal data class PredictiveMaintenanceAPIDeviceAttributes(
    @SerializedName("name") val name : String = "",
    @SerializedName("assetname") val assetName : String = "",
    @SerializedName("fab") val fab : String = "",
    @SerializedName("group") val group : String = "",
    @SerializedName("owner") val owner : String = "",
    @SerializedName("coordinates") val coordinates : Array<String> = emptyArray()
)

internal data class PredictiveMaintenanceAPIDeviceConfiguration(
    @SerializedName("Env_Time") val envTime : Int = 5,
    @SerializedName("Ine_Time_TDM") val ineTimeTDM : Int = 5,
    @SerializedName("Ine_Time_FDM") val ineTimeFDM : Int = 30,
    @SerializedName("Aco_Time") val acoTime : Int = 2,
)

internal data class PredictiveMaintenanceAPIDeviceCertificate(
    @SerializedName("certificateArn") val certificateArn : String,
    @SerializedName("certificateId") val certificateId : String,
    @SerializedName("certificatePem") val certificatePem : String,
    @SerializedName("keyPair") val keyPair : PredictiveMaintenanceAPIDeviceCertificateKeyPair
)

internal data class PredictiveMaintenanceAPIDeviceCertificateKeyPair(
    @SerializedName("PublicKey") val publicKey : String,
    @SerializedName("PrivateKey") val privateKey : String
)

internal data class PredictiveMaintenanceAPIGetDevicesResponse(
    @SerializedName("things") val things: Array<PredictiveMaintenanceAPIDevice>,
)

internal data class PredictiveMaintenanceAPIDevice(
    @SerializedName("thingName") val thingName: String,
    @SerializedName("thingTypeName") val thingTypeName: String,
    @SerializedName("attributes") val attributes : PredictiveMaintenanceAPIDeviceAttributes,
    @SerializedName("thingArn") val thingArn : String,
    @SerializedName("version") val version : Int,
)

internal data class PredictiveMaintenanceAPIError(
    @SerializedName("errorType") val errorType : String,
    @SerializedName("errorMessage") val errorMessage : String,
    @SerializedName("trace") val trace : Array<String> = emptyArray()
)