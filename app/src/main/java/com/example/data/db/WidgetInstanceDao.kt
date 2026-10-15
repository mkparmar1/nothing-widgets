package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetInstanceDao {

    @Query("SELECT * FROM widget_instances WHERE appWidgetId = :id")
    suspend fun getById(id: Int): WidgetInstance?

    @Query("SELECT * FROM widget_instances")
    suspend fun getAll(): List<WidgetInstance>

    @Query("SELECT * FROM widget_instances")
    fun observeAll(): Flow<List<WidgetInstance>>

    @Query("SELECT COUNT(*) FROM widget_instances")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(instance: WidgetInstance)

    @Update
    suspend fun update(instance: WidgetInstance)

    @Query("DELETE FROM widget_instances WHERE appWidgetId = :id")
    suspend fun deleteById(id: Int)

    @Delete
    suspend fun delete(instance: WidgetInstance)
}
