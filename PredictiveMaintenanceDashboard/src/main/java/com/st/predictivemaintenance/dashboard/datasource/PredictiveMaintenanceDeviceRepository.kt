package com.st.predictivemaintenance.dashboard.datasource

import com.st.login.AuthData
import com.st.predictivemaintenance.dashboard.datasource.local.PredictiveMaintenanceDatabase
import com.st.predictivemaintenance.dashboard.datasource.local.PredictiveMaintenanceEntityDevice
import com.st.predictivemaintenance.dashboard.datasource.remote.PredictiveMaintenanceService
import com.st.predictivemaintenance.dashboard.models.PredictiveMaintenanceDevice
import com.st.predictivemaintenance.dashboard.utils.Result
import java.lang.RuntimeException

class PredictiveMaintenanceDeviceRepository(
    private val db: PredictiveMaintenanceDatabase,
    private val service: PredictiveMaintenanceService
) {

    suspend fun getDevices() : Result<List<PredictiveMaintenanceDevice>> {
        val deviceDao = db.deviceDao()

        val deviceList = mutableListOf<PredictiveMaintenanceDevice>()
        for (device in deviceDao.getAll()) {
            val predictiveMaintenanceDevice = PredictiveMaintenanceDevice(
                device.id,
                device.name,
                device.deviceType,
                device.certificate,
                device.privateKey
            )
            deviceList.add(predictiveMaintenanceDevice)
        }

        return Result.Success(deviceList)

    }

    suspend fun getDevice(deviceId : String, authData: AuthData) : Result<PredictiveMaintenanceDevice> {
        val deviceDao = db.deviceDao()
        val deviceExists = deviceDao.deviceExists(deviceId)
        if (deviceExists) {
            val predictiveMaintenanceEntityDevice = deviceDao.getById(deviceId)

            val predictiveMaintenanceDevice = PredictiveMaintenanceDevice(
                deviceId,
                predictiveMaintenanceEntityDevice.name,
                predictiveMaintenanceEntityDevice.deviceType,
                predictiveMaintenanceEntityDevice.certificate,
                predictiveMaintenanceEntityDevice.privateKey
            )
            return Result.Success(predictiveMaintenanceDevice)
        }

        return Result.Error(RuntimeException("unknown device $deviceId"))

    }

    suspend fun addDevice(device: PredictiveMaintenanceDevice, authData: AuthData) : Result<Unit> {
        val synchronizationResult = synchronizeWithRemoteDatabase(authData)
        if (synchronizationResult is Result.Error) {
            return synchronizationResult
        }

        val predictiveMaintenanceDevice : PredictiveMaintenanceDevice

        val deviceDao = db.deviceDao()
        when (val result = service.addDevice(device, authData)) {
            is Result.Error -> {
                return result
            }
            is Result.Success<PredictiveMaintenanceDevice> -> {
                predictiveMaintenanceDevice = result.data
                val predictiveMaintenanceEntityDevice = PredictiveMaintenanceEntityDevice(
                    predictiveMaintenanceDevice.deviceId,
                    predictiveMaintenanceDevice.name,
                    predictiveMaintenanceDevice.deviceType,
                    predictiveMaintenanceDevice.certificate,
                    predictiveMaintenanceDevice.privateKey
                )
                deviceDao.insertAll(predictiveMaintenanceEntityDevice)

                return Result.Success(Unit)
            }
        }

    }

    suspend fun deleteDevice(device : PredictiveMaintenanceDevice, authData: AuthData) : Result<Unit> {
        val result = service.deleteDevice(device.deviceId, authData)
        when (result) {
            is Result.Error -> {
                return result
            }
            is Result.Success -> {

                val deviceDao = db.deviceDao()
                deviceDao.deleteById(device.deviceId)

                return Result.Success(Unit)
            }
        }

    }

    private suspend fun synchronizeWithRemoteDatabase(authData: AuthData) : Result<Unit> {
        val result = service.getDevices(authData)
        when (result) {
            is Result.Error -> {
                return result
            }
            is Result.Success -> {
                val existingDevicesIds = mutableListOf<String>()
                for (device in result.data) {
                    existingDevicesIds.add(device.deviceId)
                }

                val deviceDao = db.deviceDao()
                deviceDao.deleteAllMissingIds(existingDevicesIds)

                return Result.Success(Unit)
            }
        }
    }

}