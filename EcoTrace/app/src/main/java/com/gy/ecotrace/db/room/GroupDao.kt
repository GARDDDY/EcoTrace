package com.gy.ecotrace.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM group_cache WHERE id = :id")
    fun observe(id: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM group_cache WHERE id = :id")
    suspend fun get(id: Long): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: GroupEntity)

    @Query("DELETE FROM group_cache WHERE id = :id")
    suspend fun delete(id: Long)
}
