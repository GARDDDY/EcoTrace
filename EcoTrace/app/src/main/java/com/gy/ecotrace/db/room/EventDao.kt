package com.gy.ecotrace.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM event_cache WHERE id = :id")
    fun observe(id: Long): Flow<EventEntity?>

    @Query("SELECT * FROM event_cache WHERE id = :id")
    suspend fun get(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: EventEntity)

    @Query("DELETE FROM event_cache WHERE id = :id")
    suspend fun delete(id: Long)
}
