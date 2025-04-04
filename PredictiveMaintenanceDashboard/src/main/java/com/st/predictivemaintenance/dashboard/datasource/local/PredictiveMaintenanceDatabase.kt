package com.st.predictivemaintenance.dashboard.datasource.local

import android.content.Context
import androidx.room.*
import com.st.BlueSTSDK.Node

@Entity(tableName = "predictive_maintenance_device")
data class PredictiveMaintenanceEntityDevice(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "device_type")
    val deviceType : Node.Type,
    @ColumnInfo(name = "certificate")
    val certificate: String,
    @ColumnInfo(name = "private_key")
    val privateKey: String,
)

@Dao
interface PredictiveMaintenanceEntityDeviceDao {
    @Query("SELECT COUNT(*) FROM predictive_maintenance_device WHERE id = (:id)")
    suspend fun deviceExists(id: String): Boolean

    @Query("SELECT * FROM predictive_maintenance_device WHERE id = (:id)")
    suspend fun getById(id: String): PredictiveMaintenanceEntityDevice

    @Query("SELECT * FROM predictive_maintenance_device")
    suspend fun getAll(): List<PredictiveMaintenanceEntityDevice>

    @Query("SELECT * FROM predictive_maintenance_device WHERE id IN (:ids)")
    suspend fun loadAllByIds(ids: List<String>): List<PredictiveMaintenanceEntityDevice>

    @Insert
    suspend fun insertAll(vararg devices: PredictiveMaintenanceEntityDevice)

    @Delete
    suspend fun delete(device: PredictiveMaintenanceEntityDevice)

    @Query("DELETE FROM predictive_maintenance_device WHERE id = (:id)")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM predictive_maintenance_device WHERE id NOT IN (:ids)")
    suspend fun deleteAllMissingIds(ids: List<String>)
}

@Database(entities = [PredictiveMaintenanceEntityDevice::class], version = 4)
abstract class PredictiveMaintenanceDatabase : RoomDatabase() {

    abstract fun deviceDao(): PredictiveMaintenanceEntityDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: PredictiveMaintenanceDatabase? = null

        fun instance(context: Context): PredictiveMaintenanceDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PredictiveMaintenanceDatabase::class.java,
                    "PredictiveMaintenanceDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }

        }
    }
}