package com.gy.ecotrace.network.dto

data class RatingItem(
    val id: Long = 0,
    val username: String = "",
    val experience: Long = 0,
    val countryCode: String? = null,
    val avatarKey: String? = null
)

data class RatingPage(
    val items: List<RatingItem> = emptyList(),
    val hasMore: Boolean = false
)
