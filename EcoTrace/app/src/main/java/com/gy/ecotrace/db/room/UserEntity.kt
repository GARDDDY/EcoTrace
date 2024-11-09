package com.gy.ecotrace.db.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gy.ecotrace.db.DatabaseMethods

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val fullname: String?,
    val gender: Int,
    val countryCode: String?,
    val filters: String?,
    val aboutMe: String?,
    val experience: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

fun UserEntity.toDomain() = DatabaseMethods.UserDatabaseMethods.User(
    userId = userId,
    username = username,
    fullname = fullname,
    gender = gender,
    country_code = countryCode,
    filters = filters,
    about_me = aboutMe,
    experience = experience
)

fun DatabaseMethods.UserDatabaseMethods.User.toEntity() = UserEntity(
    userId = userId,
    username = username,
    fullname = fullname,
    gender = gender,
    countryCode = country_code,
    filters = filters,
    aboutMe = about_me,
    experience = experience
)
