package com.gy.ecotrace.network.dto

data class FriendItem(
    val id: Long = 0,
    val username: String = "",
    val avatarKey: String? = null,
    val experience: Long = 0
)

data class FriendsPage(
    val items: List<FriendItem> = emptyList(),
    val hasMore: Boolean = false,
    val hidden: Boolean = false              // true if hidden by a privacy setting
)

// code: 0 no relation, 1 I sent a request, 2 request received, 3 friends
data class FriendshipStatus(
    val code: Int = 0,
    val self: Boolean = false
)

data class FriendActionResponse(
    val code: Int = 0,
    val removed: Boolean = false
)

data class UserGroupItem(
    val id: Long = 0,
    val name: String = "",
    val about: String? = null,
    val role: String = "participant",       // enum: participant<assistant<manager<creator
    val membersCount: Int = 0,
    val rulesImageKey: String? = null
)

data class UserGroupsPage(
    val items: List<UserGroupItem> = emptyList(),
    val hasMore: Boolean = false,
    val hidden: Boolean = false
)

data class UserEventItem(
    val id: Long = 0,
    val name: String = "",
    val status: String? = null,
    val role: String = "participant",       // enum: participant<assistant<creator
    val validated: Boolean = false,
    val membersCount: Int = 0,
    val timing: Int = -1                    // 0 upcoming, 1 ongoing, 2 past
)

data class UserEventsPage(
    val items: List<UserEventItem> = emptyList(),
    val hasMore: Boolean = false,
    val hidden: Boolean = false
)

data class GraphDataset(
    val calculator: Int = 0,
    val name: String = "",
    val color: String = "128, 128, 128",    // "r, g, b"
    val data: List<Double?> = emptyList()   // null = gap in the series
)

data class GraphData(
    val unit: String = "",
    val days: List<String> = emptyList(),
    val labels: List<String> = emptyList(), // d.m
    val datasets: List<GraphDataset> = emptyList()
)
