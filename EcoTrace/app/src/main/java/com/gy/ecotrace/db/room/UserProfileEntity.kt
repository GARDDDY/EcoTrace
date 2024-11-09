package com.gy.ecotrace.db.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gy.ecotrace.network.dto.RemoteUser

// filters stored as comma-separated string, e.g. "1,2,3"
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long,
    val username: String,
    val email: String?,
    val fullname: String?,
    val gender: String?,
    val aboutMe: String?,
    val countryCode: String?,
    val experience: Long,
    val avatarKey: String?,
    val nameVisibility: String?,
    val countryVisibility: String?,
    val friendsVisibility: String?,
    val groupsVisibility: String?,
    val createdAt: String?,
    val filters: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

fun UserProfileEntity.toRemote() = RemoteUser(
    id = id,
    username = username,
    email = email,
    fullname = fullname,
    gender = gender,
    aboutMe = aboutMe,
    countryCode = countryCode,
    experience = experience,
    avatarKey = avatarKey,
    nameVisibility = nameVisibility,
    countryVisibility = countryVisibility,
    friendsVisibility = friendsVisibility,
    groupsVisibility = groupsVisibility,
    createdAt = createdAt,
    filters = filters?.takeIf { it.isNotBlank() }?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
)

fun RemoteUser.toProfileEntity() = UserProfileEntity(
    id = id,
    username = username,
    email = email,
    fullname = fullname,
    gender = gender,
    aboutMe = aboutMe,
    countryCode = countryCode,
    experience = experience,
    avatarKey = avatarKey,
    nameVisibility = nameVisibility,
    countryVisibility = countryVisibility,
    friendsVisibility = friendsVisibility,
    groupsVisibility = groupsVisibility,
    createdAt = createdAt,
    filters = filters?.joinToString(",")
)
