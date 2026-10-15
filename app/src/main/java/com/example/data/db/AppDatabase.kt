package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WidgetInstance::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun widgetInstanceDao(): WidgetInstanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dot_widgets.db"
                )
                .fallbackToDestructiveMigration(false)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
