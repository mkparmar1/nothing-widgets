package com.example.data.builder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomWidgetDao {
    @Query("SELECT * FROM custom_widgets ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CustomWidget>>

    @Query("SELECT * FROM custom_widgets ORDER BY updatedAt DESC")
    suspend fun getAll(): List<CustomWidget>

    @Query("SELECT * FROM custom_widgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CustomWidget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(widget: CustomWidget)

    @Delete
    suspend fun delete(widget: CustomWidget)
}
