package com.st.predictivemaintenance.dashboard.datasource.remote

import com.google.gson.GsonBuilder
import com.st.BlueSTSDK.Node
import com.st.login.AuthData
import com.st.predictivemaintenance.dashboard.models.PredictiveMaintenanceDevice
import com.st.predictivemaintenance.dashboard.utils.Result
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.util.concurrent.TimeUnit

class PredictiveMaintenanceService {

    private val gson = GsonBuilder()
        .disableHtmlEscaping() // avoid escaping HTML characters
        .create()

    private var predictiveMaintenanceAPI : PredictiveMaintenanceAPI
    init {

        val okHttpClient = OkHttpClient
            .Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()

        predictiveMaintenanceAPI = Retrofit.Builder()
            .baseUrl("https://1k8p44lea1.execute-api.eu-west-1.amazonaws.com/live/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PredictiveMaintenanceAPI::class.java)

    }

    suspend fun addDevice(device: PredictiveMaintenanceDevice, authData: AuthData) : Result<PredictiveMaintenanceDevice> {

        val deviceId = device.deviceId
        val deviceName = device.name
        val deviceType = device.deviceType

        val predictiveMaintenanceDeviceAddPayload = PredictiveMaintenanceAPIDeviceAddPayload(
            deviceId,
            authData.accessKey,
            PredictiveMaintenanceAPIDeviceAttributes(
                name = deviceName,
                fab = deviceName
            ),
            PredictiveMaintenanceAPIDeviceConfiguration()
        )

        val response = predictiveMaintenanceAPI.addDevice(authData.token, predictiveMaintenanceDeviceAddPayload)
        if (!response.isSuccessful) {
            val predictiveMaintenanceAPIError = parseAPIErrorResponse(response)
            return Result.Error(Exception(predictiveMaintenanceAPIError.errorMessage))
        }

        val predictiveMaintenanceDeviceCertificate = gson.fromJson(
            response.body()!!.charStream(),
            PredictiveMaintenanceAPIDeviceCertificate::class.java
        )
        val certificatePem = predictiveMaintenanceDeviceCertificate.certificatePem
        val privateKey = predictiveMaintenanceDeviceCertificate.keyPair.privateKey

        val predictiveMaintenanceDevice = PredictiveMaintenanceDevice(
            deviceId,
            deviceName,
            deviceType,
            certificatePem,
            privateKey
        )

        return Result.Success(predictiveMaintenanceDevice)

    }

    suspend fun getDevices(authData: AuthData) : Result<List<PredictiveMaintenanceDevice>> {
        val response = predictiveMaintenanceAPI.getDevices(authData.token, authData.accessKey)
        if (!response.isSuccessful) {
            val predictiveMaintenanceAPIError = parseAPIErrorResponse(response)
            return Result.Error(Exception(predictiveMaintenanceAPIError.errorMessage))
        }

        val predictiveMaintenanceAPIGetDevicesResponse = gson.fromJson(
            response.body()!!.charStream(),
            PredictiveMaintenanceAPIGetDevicesResponse::class.java
        )

        val devices = mutableListOf<PredictiveMaintenanceDevice>()
        for (device in predictiveMaintenanceAPIGetDevicesResponse.things) {
            devices.add(
                PredictiveMaintenanceDevice(
                    device.thingName,
                    device.attributes.assetName,
                    Node.Type.GENERIC,
                    "",
                    ""
                )
            )
        }

        return Result.Success(devices)
    }

    suspend fun deleteDevice(deviceId : String, authData: AuthData) : Result<Unit> {

        val predictiveMaintenanceDeviceDeletePayload = PredictiveMaintenanceAPIDeviceDeletePayload(
            deviceId,
            authData.accessKey,
        )

        val response = predictiveMaintenanceAPI.deleteDevice(authData.token, predictiveMaintenanceDeviceDeletePayload)
        if (!response.isSuccessful) {
            val predictiveMaintenanceAPIError = parseAPIErrorResponse(response)
            return Result.Error(Exception(predictiveMaintenanceAPIError.errorMessage))
        }

        return Result.Success(Unit)

    }

    private fun parseAPIErrorResponse(errorResponse : Response<ResponseBody>) : PredictiveMaintenanceAPIError {
        val predictiveMaintenanceAPIError = gson.fromJson(
            errorResponse.errorBody()!!.charStream(),
            PredictiveMaintenanceAPIError::class.java
        )

        return predictiveMaintenanceAPIError
    }

}