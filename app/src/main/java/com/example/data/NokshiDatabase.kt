package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkspacePdfEntity::class, SignatureEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NokshiDatabase : RoomDatabase() {
    abstract fun workspacePdfDao(): WorkspacePdfDao
    abstract fun signatureDao(): SignatureDao

    companion object {
        @Volatile
        private var INSTANCE: NokshiDatabase? = null

        fun getInstance(context: Context): NokshiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NokshiDatabase::class.java,
                    "nokshi_pdf_db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
