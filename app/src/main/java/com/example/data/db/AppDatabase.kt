package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.builder.CustomWidget
import com.example.data.builder.CustomWidgetDao

@Database(entities = [WidgetInstance::class, CustomWidget::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun widgetInstanceDao(): WidgetInstanceDao
    abstract fun customWidgetDao(): CustomWidgetDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_widgets` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `layoutJson` TEXT NOT NULL, `sizesCsv` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dot_widgets.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
