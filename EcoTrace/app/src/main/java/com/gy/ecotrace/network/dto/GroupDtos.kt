package com.gy.ecotrace.network.dto

data class RemoteGroup(
    val id: Long = 0,
    val name: String = "",
    val creatorId: Long = 0,
    val type: String = "open",          // open | closed
    val about: String? = null,
    val rules: String? = null,
    val rulesImageKey: String? = null,
    val createdAt: String? = null,
    val creatorUsername: String? = null,
    val membersCount: Int = 0,
    val myRole: String? = null
)

data class GroupsPage(
    val items: List<RemoteGroup> = emptyList(),
    val hasMore: Boolean = false
)

data class CheckNameResponse(
    val available: Boolean = false,
    val reason: String? = null
)

data class CreateGroupRequest(
    val name: String,
    val type: String = "open",
    val about: String? = null,
    val rules: String? = null,
    val filters: List<Int>? = null
)

data class GroupPost(
    val id: Long = 0,
    val groupId: Long = 0,
    val authorId: Long = 0,
    val text: String? = null,
    val imageKey: String? = null,
    val pinnedAt: String? = null,
    val createdAt: String? = null,
    val authorUsername: String? = null,
    val authorAvatarKey: String? = null,
    val authorRole: String? = null,
    val commentsCount: Int = 0
)

data class PostsPage(
    val items: List<GroupPost> = emptyList(),
    val hasMore: Boolean = false
)

data class GroupMember(
    val userId: Long = 0,
    val role: String = "participant",
    val joinedAt: String? = null,
    val username: String = "",
    val avatarKey: String? = null,
    val postsCount: Int = 0
)

data class GroupMembersPage(
    val items: List<GroupMember> = emptyList(),
    val hasMore: Boolean = false,
    val total: Int = 0
)

data class GroupComment(
    val id: Long = 0,
    val postId: Long = 0,
    val authorId: Long = 0,
    val text: String? = null,
    val imageKey: String? = null,
    val createdAt: String? = null,
    val authorUsername: String? = null,
    val authorRole: String? = null
)

data class CommentsPage(
    val items: List<GroupComment> = emptyList(),
    val hasMore: Boolean = false
)

data class DeletedResponse(val deleted: Boolean = false)
data class JoinedResponse(val joined: Boolean = false)
data class LeftResponse(val left: Boolean = false)
data class UnpinnedResponse(val unpinned: Boolean = false)
