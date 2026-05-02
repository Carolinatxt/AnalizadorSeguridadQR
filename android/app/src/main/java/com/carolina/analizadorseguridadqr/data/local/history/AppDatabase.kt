package com.carolina.analizadorseguridadqr.data.local.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ScanHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "analizador_qr.db",
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

// Decisión V0:
// exportSchema = false porque esta primera versión solo tiene un esquema inicial.
// Si en el futuro se cambia la estructura de scan_history,
// se deberá incrementar version y escribir una migración explícita de Room.
