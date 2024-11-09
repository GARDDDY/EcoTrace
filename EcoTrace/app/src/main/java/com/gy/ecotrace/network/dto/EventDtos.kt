package com.gy.ecotrace.network.dto

data class RemoteEvent(
    val id: Long = 0,
    val name: String = "",
    val about: String? = null,
    val creatorId: Long = 0,
    val status: String? = null,
    val startCondition: String? = null,  // start_condition: first_action|member_count
    val startThreshold: Int? = null,
    val imageKey: String? = null,
    val createdAt: String? = null,
    val creatorUsername: String? = null,
    val membersCount: Int = 0,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val myRole: String? = null,
    val myValidated: Boolean = false,
    val timing: Int = 0,                 // 0 upcoming / 1 ongoing / 2 past
    val hasMap: Boolean = false,
    val filters: List<EventTag> = emptyList()
)

data class EventTag(
    val name: String = "",
    val colorFg: String? = null,
    val colorBg: String? = null
)

data class EventBounds(
    val minStartsAt: String? = null,
    val maxStartsAt: String? = null,
    val minMembers: Int = 0,
    val maxMembers: Int = 0
)

data class EventsPage(
    val items: List<RemoteEvent> = emptyList(),
    val hasMore: Boolean = false
)

data class EventGoal(
    val id: Long = 0,
    val text: String = "",
    val position: Int = 0
)

data class EventScheduleItem(
    val id: Long = 0,
    val title: String = "",
    val startsAt: String? = null,
    val endsAt: String? = null,
    val position: Int = 0
)

data class EventZone(
    val id: Long = 0,
    val scheduleItemId: Long? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val label: String? = null,
    val isZone: Boolean = false,         // true - circle zone, false - point
    val radiusM: Int? = null,
    val colorFill: String? = null,
    val colorStroke: String? = null
)

data class EventMember(
    val userId: Long = 0,
    val role: String = "participant",
    val validated: Boolean = false,
    val username: String = "",
    val experience: Long = 0,
    val avatarKey: String? = null
)

data class EventMembersPage(
    val items: List<EventMember> = emptyList(),
    val hasMore: Boolean = false
)

data class CreateEventRequest(
    val name: String,
    val about: String? = null,
    val filters: List<String>? = null,
    val startCondition: String = "first_action",
    val startThreshold: Int? = null,
    val schedule: List<ScheduleInput>,
    val goals: List<String>? = null,
    val zones: List<ZoneInput>? = null
)

data class ScheduleInput(
    val title: String,
    val startsAt: String,
    val endsAt: String? = null
)

data class ZoneInput(
    val label: String? = null,
    val isZone: Boolean? = null,
    val latitude: Double,
    val longitude: Double,
    val radiusM: Int? = null,
    val colorFill: String? = null,
    val colorStroke: String? = null,
    val scheduleIndex: Int? = null
)

data class ValidateResponse(
    val validated: Boolean = false,
    val awarded: Int = 0,
    val message: String? = null
)
