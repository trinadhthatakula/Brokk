package com.valhalla.brokk.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM installation_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Query("DELETE FROM installation_history")
    suspend fun deleteAll()

    @Query("DELETE FROM installation_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
