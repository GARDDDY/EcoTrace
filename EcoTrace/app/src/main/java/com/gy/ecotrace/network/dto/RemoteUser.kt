package com.gy.ecotrace.network.dto

data class RemoteUser(
    val id: Long = 0,
    val username: String = "",
    val email: String? = null,
    val fullname: String? = null,
    val gender: String? = null,
    val aboutMe: String? = null,
    val countryCode: String? = null,
    val experience: Long = 0,
    val avatarKey: String? = null,
    val nameVisibility: String? = null,
    val countryVisibility: String? = null,
    val friendsVisibility: String? = null,
    val groupsVisibility: String? = null,
    val createdAt: String? = null,

    val filters: List<Int>? = null,
    val isOwner: Boolean? = null,
    val isFriend: Boolean? = null,

    val commonGroupsCount: Int? = null,
    val commonEventsCount: Int? = null
)
