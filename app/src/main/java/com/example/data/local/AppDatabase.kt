package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.IoTDevice
import com.example.data.model.SensorLog
import com.example.data.model.AutomationRule
import com.example.data.model.FirmwareRelease

@Database(
    entities = [
        IoTDevice::class,
        SensorLog::class,
        AutomationRule::class,
        FirmwareRelease::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun sensorDao(): SensorLogDao
    abstract fun automationDao(): AutomationDao
    abstract fun firmwareDao(): FirmwareDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "craft_iot_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
