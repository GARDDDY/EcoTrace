package com.gy.ecotrace.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, PostEntity::class, UserProfileEntity::class, GroupEntity::class, EventEntity::class],
    version = 5,
    exportSchema = false
)
abstract class EcoTraceDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun groupDao(): GroupDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var instance: EcoTraceDatabase? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(
                            context.applicationContext,
                            EcoTraceDatabase::class.java,
                            "ecotrace.db"
                        )
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
        }

        fun getInstance(): EcoTraceDatabase {
            return instance
                ?: throw IllegalStateException("EcoTraceDatabase is not initialized. Call initialize() in App.onCreate().")
        }
    }
}
