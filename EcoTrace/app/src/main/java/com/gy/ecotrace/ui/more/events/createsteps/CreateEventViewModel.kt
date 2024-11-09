package com.gy.ecotrace.ui.more.events.createsteps

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.network.dto.CreateEventRequest
import com.gy.ecotrace.network.dto.ScheduleInput
import com.gy.ecotrace.network.dto.ZoneInput
import com.gy.ecotrace.repos.EventRepository
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapObject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.inject.Inject

data class DraftCoord(
    val isCircle: Boolean,
    val lat: Double,
    val lng: Double,
    val radius: Float?,
    val fill: String,
    val stroke: String,
    val name: String?
)

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val gson: Gson,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    lateinit var currentEvent: String
    var eventImage: Bitmap? = null

    var editEventId: Long? = null
        private set

    private val _eventData = MutableLiveData<DatabaseMethods.DataClasses.Event>()
    val eventData: LiveData<DatabaseMethods.DataClasses.Event> get() = _eventData

    fun getEvent() {
        _eventData.postValue(DatabaseMethods.DataClasses.Event())
    }

    fun loadForEdit(eventId: Long, onLoaded: (Boolean) -> Unit) {
        editEventId = eventId
        viewModelScope.launch {
            val event = runCatching { repository.refreshEvent(eventId) }.getOrNull() ?: repository.cachedEvent(eventId)
            if (event == null) { onLoaded(false); return@launch }

            val eventClass = DatabaseMethods.DataClasses.Event()
            eventClass.eventName = event.name
            eventClass.eventAbout = event.about
            eventClass.filters = event.filters.joinToString(",") { it.name }
            _eventData.postValue(eventClass)

            val schedule = runCatching { repository.schedule(eventId) }.getOrDefault(emptyList())
            schedule.firstOrNull()?.let { first ->
                val startSec = runCatching { Instant.parse(first.startsAt).epochSecond }.getOrNull()
                val endSec = first.endsAt?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() }
                if (startSec != null) addTime("${startSec}_${endSec ?: ""}", first.title)
            }

            val goals = runCatching { repository.goals(eventId) }.getOrDefault(emptyList())
            _eventGoals.postValue(goals.mapIndexed { i, g -> i.toString() to g.text }.toMap().toMutableMap())

            val zones = runCatching { repository.zones(eventId) }.getOrDefault(emptyList())
            _restoreDraftCoords.postValue(zones.map { z ->
                DraftCoord(
                    isCircle = z.isZone,
                    lat = z.latitude, lng = z.longitude,
                    radius = z.radiusM?.toFloat(),
                    fill = z.colorFill ?: "#3C0000FF",
                    stroke = z.colorStroke ?: "#FF0000FF",
                    name = z.label
                )
            })

            onLoaded(true)
        }
    }

    private val _eventTimes = MutableLiveData<MutableMap<String, String>>()
    val eventTimes: LiveData<MutableMap<String, String>> get() = _eventTimes

    fun getTimes() {}

    fun addTime(index: String, value: String) {
        val newMap = _eventTimes.value ?: mutableMapOf()
        newMap[index] = value
        _eventTimes.postValue(newMap)
    }

    fun removeTime(index: String?) {
        if (index != null) {
            val newMap = _eventTimes.value ?: return
            newMap.remove(index)
            _eventTimes.postValue(newMap)
        }
    }

    private val _eventGoals = MutableLiveData<MutableMap<String, String>>()
    val eventGoals: LiveData<MutableMap<String, String>> get() = _eventGoals
    private var lastVal: String = 0.toString()

    private val _toAddeventGoals = MutableLiveData<MutableList<String>>()
    val toAddeventGoals: LiveData<MutableList<String>> get() = _toAddeventGoals

    fun getGoals() {}
    fun addGoal(): String { lastVal = (lastVal.toInt() + 1).toString(); return lastVal }
    fun addGoal(index: String, value: String) {
        val newMap = _eventGoals.value ?: mutableMapOf()
        newMap[index] = value
        _eventGoals.postValue(newMap)
    }
    fun removeGoal(index: String) {
        val newMap = _eventGoals.value ?: return
        newMap.remove(index)
        _eventGoals.postValue(newMap)
    }

    private val _eventCoords = MutableLiveData<MutableMap<MapObject, DatabaseMethods.DataClasses.MapObject>>()
    val eventCoords: LiveData<MutableMap<MapObject, DatabaseMethods.DataClasses.MapObject>> get() = _eventCoords

    private val _toAddeventCoords = MutableLiveData<MutableList<DatabaseMethods.DataClasses.MapObject>>()
    val toAddeventCoords: LiveData<MutableList<DatabaseMethods.DataClasses.MapObject>> get() = _toAddeventCoords

    private val _restoreDraftCoords = MutableLiveData<List<DraftCoord>?>()
    val restoreDraftCoords: LiveData<List<DraftCoord>?> get() = _restoreDraftCoords

    fun clearRestoreDraftCoords() = _restoreDraftCoords.postValue(null)

    fun getCoords() {}

    private val coordsBackingMap = mutableMapOf<MapObject, DatabaseMethods.DataClasses.MapObject>()

    fun addCoord(obj: MapObject, coord: DatabaseMethods.DataClasses.MapObject) {
        coordsBackingMap[obj] = coord
        _eventCoords.postValue(coordsBackingMap)
    }

    fun getCoord(obj: MapObject): DatabaseMethods.DataClasses.MapObject = coordsBackingMap[obj]!!

    fun removeCoord(obj: MapObject) {
        coordsBackingMap.remove(obj)
        _eventCoords.postValue(coordsBackingMap)
    }

    private val draftPrefs get() =
        appContext.getSharedPreferences("event_draft", Context.MODE_PRIVATE)

    fun hasDraft(): Boolean = draftPrefs.contains("draft")

    fun saveDraft() {
        if (editEventId != null) return
        val event = _eventData.value ?: return
        val timeKey = _eventTimes.value?.keys?.firstOrNull()
        val parts = timeKey?.split("_")
        val startSec = parts?.getOrNull(0)?.toLongOrNull()
        val endSec   = parts?.getOrNull(1)?.takeIf { it.isNotBlank() }?.toLongOrNull()

        val coordList = coordsBackingMap.values.map { obj ->
            mapOf(
                "isCircle" to (obj.objectType == 0),
                "lat"      to obj.objectCenter.latitude,
                "lng"      to obj.objectCenter.longitude,
                "radius"   to obj.circleRadius,
                "fill"     to obj.fillColor,
                "stroke"   to obj.strokeColor,
                "name"     to obj.objectName
            )
        }

        val draft = mapOf(
            "name"     to (event.eventName ?: ""),
            "about"    to (event.eventAbout ?: ""),
            "filters"  to event.filters,
            "startSec" to startSec,
            "endSec"   to endSec,
            "coords"   to coordList
        )
        draftPrefs.edit().putString("draft", gson.toJson(draft)).apply()
    }

    fun clearDraft() = draftPrefs.edit().remove("draft").apply()

    @Suppress("UNCHECKED_CAST")
    fun restoreDraft() {
        val json = draftPrefs.getString("draft", null) ?: return
        try {
            val map = gson.fromJson<Map<String, Any?>>(json, object : TypeToken<Map<String, Any?>>() {}.type)

            val event = DatabaseMethods.DataClasses.Event()
            event.eventName  = map["name"] as? String ?: ""
            event.eventAbout = (map["about"] as? String).takeIf { !it.isNullOrBlank() }
            event.filters    = map["filters"] as? String ?: ""
            _eventData.postValue(event)

            val startSec = (map["startSec"] as? Double)?.toLong()
            val endSec   = (map["endSec"]   as? Double)?.toLong()
            if (startSec != null) addTime("${startSec}_${endSec ?: ""}", "")

            val rawCoords = map["coords"] as? List<Map<String, Any?>>
            if (!rawCoords.isNullOrEmpty()) {
                val coords = rawCoords.mapNotNull { m ->
                    val lat = (m["lat"] as? Double) ?: return@mapNotNull null
                    val lng = (m["lng"] as? Double) ?: return@mapNotNull null
                    DraftCoord(
                        isCircle = m["isCircle"] as? Boolean ?: false,
                        lat = lat, lng = lng,
                        radius = (m["radius"] as? Double)?.toFloat(),
                        fill   = m["fill"]   as? String ?: "#00000000",
                        stroke = m["stroke"] as? String ?: "#00000000",
                        name   = m["name"]   as? String
                    )
                }
                _restoreDraftCoords.postValue(coords)
            }
        } catch (_: Exception) { }
    }

    fun submitEvent(callback: (String?, String?) -> Unit) {
        viewModelScope.launch {
            val event  = _eventData.value ?: DatabaseMethods.DataClasses.Event()
            val times  = _eventTimes.value ?: emptyMap()
            val goalsMap = _eventGoals.value ?: emptyMap()
            val coords = coordsBackingMap

            val keys = times.keys.toList()
            val schedule = keys.map { key ->
                val parts = key.split("_")
                ScheduleInput(
                    title    = times[key].orEmpty().ifBlank { "—" },
                    startsAt = epochSecToIso(parts.getOrNull(0)?.toLongOrNull() ?: 0L),
                    endsAt   = parts.getOrNull(1)?.toLongOrNull()?.let { epochSecToIso(it) }
                )
            }
            val keyToIndex = keys.withIndex().associate { (i, k) -> k to i }
            val goals  = goalsMap.values.map { it.trim() }.filter { it.isNotEmpty() }
            val zones  = coords.values.map { obj ->
                ZoneInput(
                    label       = obj.objectName?.takeIf { it.isNotBlank() },
                    isZone      = obj.objectType == 0,
                    latitude    = obj.objectCenter.latitude,
                    longitude   = obj.objectCenter.longitude,
                    radiusM     = obj.circleRadius?.toInt(),
                    colorFill   = obj.fillColor,
                    colorStroke = obj.strokeColor,
                    scheduleIndex = obj.objectRelation?.let { keyToIndex[it] }
                )
            }
            val filters = event.filters.takeIf { it.isNotBlank() }
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }

            val request = CreateEventRequest(
                name     = event.eventName ?: "",
                about    = event.eventAbout?.takeIf { it.isNotBlank() },
                filters  = filters,
                schedule = schedule,
                goals    = goals.ifEmpty { null },
                zones    = zones.ifEmpty { null }
            )

            val editId = editEventId
            val result = if (editId != null) {
                runCatching { repository.update(editId, request, eventImage?.toPng()) }
            } else {
                runCatching { repository.create(request, eventImage?.toPng()) }
            }
            if (result.isSuccess && editId == null) clearDraft()
            callback(
                result.getOrNull()?.id?.toString(),
                if (result.isSuccess) null else result.exceptionOrNull()?.message
            )
        }
    }

    private fun epochSecToIso(sec: Long): String = Instant.ofEpochSecond(sec).toString()

    private fun Bitmap.toPng(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
