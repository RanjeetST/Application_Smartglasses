package com.st.clab.bleSensor.fwUpgradeChecker.repository

import android.content.Context
import android.util.Log

import com.st.clab.bleSensor.fwUpgradeChecker.repository.db.DeviceFirmwareDao
import com.st.clab.bleSensor.fwUpgradeChecker.repository.db.DeviceFirmwareDatabase
import com.st.clab.bleSensor.fwUpgradeChecker.repository.network.DeviceFirmwareService
import java.io.IOException
import java.lang.Exception
import java.util.Date

import java.util.concurrent.Executors

internal class DeviceFirmwareRepository(
                               private val remoteSyncDao:RemoteSyncDao,
                               private val localDeviceFirmware : DeviceFirmwareDao,
                               private val remoteDeviceFirmware: DeviceFirmwareService) {

    suspend fun getLastFwFor(type:Byte,name:String, mcu:String): DeviceFirmware? {
        if(needRemoteSync()) {
            syncLocalDb()
        }
        return localDeviceFirmware.getFwForDevice(type,name,mcu)
    }

    private suspend fun syncLocalDb(){
        try {
            val remoteData = remoteDeviceFirmware.getFirmwareReleases()
            localDeviceFirmware.add(remoteData.firmwares)
            remoteSyncDao.lastSync = Date()
        }catch (e:Exception){
            Log.e(this::javaClass.name,"Error sync: "+e.localizedMessage)
            e.printStackTrace()
        }
    }

    private fun needRemoteSync():Boolean{
        val minSync = Date().time - MIN_REMOTE_SYNC_TIME_MS
        return remoteSyncDao.lastSync.before(Date(minSync))
    }

    companion object {
        private const val MIN_REMOTE_SYNC_TIME_MS = 24*60*60*1000L

        private var instance:DeviceFirmwareRepository? = null
        fun getInstance( ctx:Context, baseUrl:String):DeviceFirmwareRepository{
            synchronized(this){
                if(instance == null){
                    val sharedPreferences = ctx.getSharedPreferences(
                            DeviceFirmwareRepository::javaClass.name, Context.MODE_PRIVATE)
                    instance = DeviceFirmwareRepository(
                            RemoteSyncDao(sharedPreferences),
                            DeviceFirmwareDatabase.getDatabase(ctx).deviceFirmware(),
                            DeviceFirmwareService.buildInstance(baseUrl))
                }
                return instance!!
            }
        }

    }

}
