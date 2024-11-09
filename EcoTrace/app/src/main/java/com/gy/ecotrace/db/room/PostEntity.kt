package com.gy.ecotrace.db.room

import androidx.room.Entity
import androidx.room.Index
import com.gy.ecotrace.db.DatabaseMethods

@Entity(
    tableName = "posts",
    primaryKeys = ["groupId", "postId"],
    indices = [Index("groupId")]
)
data class PostEntity(
    val postId: Int,
    val groupId: String,
    val postTime: Long,
    val postCreatorId: String,
    val postCreatorName: String,
    val postContentText: String?,
    val postContentImage: String?,
    val postCommentsCount: Long,
    val postCreatorRole: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

fun PostEntity.toDomain() = DatabaseMethods.DataClasses.Post(
    postId = postId,
    postTime = postTime,
    postCreatorId = postCreatorId,
    postCreatorName = postCreatorName,
    postContentText = postContentText,
    postContentImage = postContentImage,
    postCommentsCount = postCommentsCount,
    postCreatorRole = postCreatorRole
)

fun DatabaseMethods.DataClasses.Post.toEntity(groupId: String) = PostEntity(
    postId = postId,
    groupId = groupId,
    postTime = postTime,
    postCreatorId = postCreatorId,
    postCreatorName = postCreatorName,
    postContentText = postContentText,
    postContentImage = postContentImage,
    postCommentsCount = postCommentsCount,
    postCreatorRole = postCreatorRole
)
