package com.gy.ecotrace.network.dto

data class ProfileEditRequest(
    val username: String? = null,
    val aboutMe: String? = null,
    val countryCode: String? = null,
    val fullname: String? = null,
    val filters: List<Int>? = null
)

data class PrivacyRequest(
    val nameVisibility: String? = null,
    val countryVisibility: String? = null,
    val friendsVisibility: String? = null,
    val groupsVisibility: String? = null
)

data class UserSearchPage(
    val items: List<RemoteUser> = emptyList(),
    val hasMore: Boolean = false
)

data class UpdatedResponse(val updated: Boolean = false)
