package com.gy.ecotrace.db.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gy.ecotrace.network.dto.RemoteGroup

@Entity(tableName = "group_cache")
data class GroupEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val creatorId: Long,
    val type: String,
    val about: String?,
    val rules: String?,
    val rulesImageKey: String?,
    val imageGenerated: Boolean = false,
    val createdAt: String?,
    val creatorUsername: String?,
    val creatorAvatarKey: String?,
    val membersCount: Int,
    val myRole: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

fun GroupEntity.toRemote() = RemoteGroup(
    id = id,
    name = name,
    creatorId = creatorId,
    type = type,
    about = about,
    rules = rules,
    rulesImageKey = rulesImageKey,
    imageGenerated = imageGenerated,
    createdAt = createdAt,
    creatorUsername = creatorUsername,
    creatorAvatarKey = creatorAvatarKey,
    membersCount = membersCount,
    myRole = myRole
)

fun RemoteGroup.toEntity() = GroupEntity(
    id = id,
    name = name,
    creatorId = creatorId,
    type = type,
    about = about,
    rules = rules,
    rulesImageKey = rulesImageKey,
    imageGenerated = imageGenerated,
    createdAt = createdAt,
    creatorUsername = creatorUsername,
    creatorAvatarKey = creatorAvatarKey,
    membersCount = membersCount,
    myRole = myRole
)
