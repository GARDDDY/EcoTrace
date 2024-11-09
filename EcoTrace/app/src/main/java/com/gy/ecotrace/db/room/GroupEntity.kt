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
    val createdAt: String?,
    val creatorUsername: String?,
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
    createdAt = createdAt,
    creatorUsername = creatorUsername,
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
    createdAt = createdAt,
    creatorUsername = creatorUsername,
    membersCount = membersCount,
    myRole = myRole
)
