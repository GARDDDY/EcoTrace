package com.gy.ecotrace.db.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gy.ecotrace.network.dto.RemoteEvent

@Entity(tableName = "event_cache")
data class EventEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val about: String?,
    val creatorId: Long,
    val status: String?,
    val startCondition: String?,
    val startThreshold: Int?,
    val imageKey: String?,
    val createdAt: String?,
    val creatorUsername: String?,
    val membersCount: Int,
    val startsAt: String?,
    val endsAt: String?,
    val myRole: String?,
    val myValidated: Boolean,
    val timing: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

fun EventEntity.toRemote() = RemoteEvent(
    id = id, name = name, about = about, creatorId = creatorId, status = status,
    startCondition = startCondition, startThreshold = startThreshold, imageKey = imageKey,
    createdAt = createdAt, creatorUsername = creatorUsername, membersCount = membersCount,
    startsAt = startsAt, endsAt = endsAt, myRole = myRole, myValidated = myValidated, timing = timing
)

fun RemoteEvent.toEntity() = EventEntity(
    id = id, name = name, about = about, creatorId = creatorId, status = status,
    startCondition = startCondition, startThreshold = startThreshold, imageKey = imageKey,
    createdAt = createdAt, creatorUsername = creatorUsername, membersCount = membersCount,
    startsAt = startsAt, endsAt = endsAt, myRole = myRole, myValidated = myValidated, timing = timing
)
