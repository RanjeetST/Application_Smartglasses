package com.st.clab.bleSensor.fwUpgradeChecker.repository.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.st.clab.bleSensor.fwUpgradeChecker.repository.DeviceFirmware

@Dao
internal interface DeviceFirmwareDao {

    @Query("SELECT * FROM DeviceFirmware WHERE boardType = :type AND name LIKE :fwName AND mcu LIKE :mcuType")
    suspend fun getFwForDevice(type:Byte,fwName:String, mcuType:String): DeviceFirmware?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(firmwares: List<DeviceFirmware>)

}
