package com.gy.ecotrace.repos

import com.google.gson.Gson
import com.gy.ecotrace.db.room.EcoTraceDatabase
import com.gy.ecotrace.db.room.toEntity
import com.gy.ecotrace.db.room.toRemote
import com.gy.ecotrace.network.EventApi
import com.gy.ecotrace.network.apiCall
import com.gy.ecotrace.network.dto.CreateEventRequest
import com.gy.ecotrace.network.dto.EventBounds
import com.gy.ecotrace.network.dto.EventGoal
import com.gy.ecotrace.network.dto.EventMember
import com.gy.ecotrace.network.dto.EventMembersPage
import com.gy.ecotrace.network.dto.EventScheduleItem
import com.gy.ecotrace.network.dto.EventZone
import com.gy.ecotrace.network.dto.EventsPage
import com.gy.ecotrace.network.dto.RemoteEvent
import com.gy.ecotrace.network.dto.ValidateResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val api: EventApi,
    private val gson: Gson
) {
    private val dao get() = EcoTraceDatabase.getInstance().eventDao()

    fun observeEvent(eventId: Long): Flow<RemoteEvent?> =
        dao.observe(eventId).map { it?.toRemote() }

    suspend fun refreshEvent(eventId: Long): RemoteEvent {
        val event = apiCall { api.get(eventId) }
        dao.upsert(event.toEntity())
        return event
    }

    suspend fun cachedEvent(eventId: Long): RemoteEvent? = dao.get(eventId)?.toRemote()

    suspend fun goals(eventId: Long): List<EventGoal> = apiCall { api.goals(eventId) }
    suspend fun schedule(eventId: Long): List<EventScheduleItem> = apiCall { api.schedule(eventId) }
    suspend fun zones(eventId: Long): List<EventZone> = apiCall { api.zones(eventId) }

    suspend fun list(
        filter: String? = null, from: String? = null, to: String? = null, after: Long? = null,
        name: String? = null,
        sort: String? = null,
        minMembers: Int? = null, maxMembers: Int? = null, hasMap: Boolean? = null,
        lat: Double? = null, lng: Double? = null, radiusKm: Double? = null
    ): EventsPage = apiCall { api.list(filter, from, to, after, name, sort, minMembers, maxMembers, hasMap, lat, lng, radiusKm) }

    suspend fun bounds(): EventBounds = apiCall { api.bounds() }

    private fun CreateEventRequest.toParts(imagePng: ByteArray?): Pair<RequestBody, MultipartBody.Part?> {
        val dataPart = gson.toJson(this).toRequestBody("application/json".toMediaTypeOrNull())
        val imagePart = imagePng?.let {
            MultipartBody.Part.createFormData("image", "event.png", it.toRequestBody("image/png".toMediaTypeOrNull()))
        }
        return dataPart to imagePart
    }

    suspend fun create(request: CreateEventRequest, imagePng: ByteArray? = null): RemoteEvent {
        val (dataPart, imagePart) = request.toParts(imagePng)
        return apiCall { api.create(dataPart, imagePart) }
    }

    suspend fun update(eventId: Long, request: CreateEventRequest, imagePng: ByteArray? = null): RemoteEvent {
        val (dataPart, imagePart) = request.toParts(imagePng)
        val updated = apiCall { api.update(eventId, dataPart, imagePart) }
        dao.upsert(updated.toEntity())
        return updated
    }

    suspend fun delete(eventId: Long): Boolean {
        val ok = apiCall { api.delete(eventId) }.deleted
        if (ok) dao.delete(eventId)
        return ok
    }

    suspend fun members(eventId: Long, username: String? = null, after: Long? = null): EventMembersPage =
        apiCall { api.members(eventId, username, after) }

    suspend fun membersPreview(eventId: Long, limit: Int = 5): List<EventMember> =
        apiCall { api.membersPreview(eventId, limit) }.items

    suspend fun join(eventId: Long): Boolean = apiCall { api.join(eventId) }.joined
    suspend fun leave(eventId: Long): Boolean = apiCall { api.leave(eventId) }.left

    suspend fun setRole(eventId: Long, userId: Long, role: String): Boolean =
        apiCall { api.setRole(eventId, userId, mapOf("role" to role)) }.updated

    suspend fun validate(eventId: Long, userId: Long): ValidateResponse =
        apiCall { api.validate(eventId, userId) }
}
