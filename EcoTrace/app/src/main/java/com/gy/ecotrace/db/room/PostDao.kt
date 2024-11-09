package com.gy.ecotrace.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE groupId = :groupId ORDER BY postTime DESC")
    suspend fun getPostsForGroup(groupId: String): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE groupId = :groupId")
    suspend fun clearGroup(groupId: String)

    @Query("DELETE FROM posts WHERE cachedAt < :threshold")
    suspend fun deleteStale(threshold: Long)
}
