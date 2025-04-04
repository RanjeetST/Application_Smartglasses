package com.st.clab.bleSensor.fwUpgradeChecker.repository.db;

import android.content.Context;
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.st.clab.bleSensor.fwUpgradeChecker.repository.DeviceFirmware;

@Database(entities = [DeviceFirmware::class], version = 1, exportSchema = false)
internal abstract class DeviceFirmwareDatabase : RoomDatabase() {

    abstract fun deviceFirmware(): DeviceFirmwareDao

    companion object {
        @Volatile
        private var INSTANCE: DeviceFirmwareDatabase? = null

        fun getDatabase(context: Context): DeviceFirmwareDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        DeviceFirmwareDatabase::class.java,
                        "DeviceFirmwareDatabase"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}