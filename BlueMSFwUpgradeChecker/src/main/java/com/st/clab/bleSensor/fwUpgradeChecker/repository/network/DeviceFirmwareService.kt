package com.st.clab.bleSensor.fwUpgradeChecker.repository.network

import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

internal interface DeviceFirmwareService{
    @GET("deviceFirmwareReleases.json")
    suspend fun getFirmwareReleases(): FirmwareReleases


    companion object{

        fun buildInstance(baseUrl:String):DeviceFirmwareService{

            val gsonConvert = GsonBuilder()
                    .setDateFormat("dd-MM-yyyy")
                    .create()

            return Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gsonConvert))
                    .build()
                    .create(DeviceFirmwareService::class.java)
        }
    }
}