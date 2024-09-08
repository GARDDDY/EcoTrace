package com.gy.ecotrace.ui.more.events.createsteps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.yandex.mapkit.map.MapObject
import kotlinx.coroutines.launch

class CreateEventViewModel(private val repository: Repository): ViewModel() {

    private val _x = MutableLiveData<String>()
    val x: LiveData<String> get() = _x

    fun text(text: String = "Hello") {
        _x.postValue(text)
    }


    private val _eventData = MutableLiveData<DatabaseMethods.DataClasses.Event>()
    val eventData: LiveData<DatabaseMethods.DataClasses.Event> get() = _eventData

    fun getEvent(eventId: String) {
        if (eventId == "0") {
            _eventData.postValue(DatabaseMethods.DataClasses.Event())
        } else {
            viewModelScope.launch {
                val eventData = repository.getEvent(eventId)
                eventData.filters = eventData.filters.split(':')[1]
                _eventData.postValue(eventData)
            }
        }
    }

    private val _eventTimes = MutableLiveData<MutableMap<String, String>>()
    val eventTimes: LiveData<MutableMap<String, String>> get() = _eventTimes

    fun addTime(index: String, value: String) {
        val newMap = _eventTimes.value ?: mutableMapOf()
        newMap[index] = value
        _eventTimes.postValue(newMap)
    }

    fun removeTime(index: String) {
        val newMap = _eventTimes.value!!
        newMap.remove(index)
        _eventTimes.postValue(newMap)
    }

    private val _eventGoals = MutableLiveData<MutableMap<String, String>>()
    val eventGoals: LiveData<MutableMap<String, String>> get() = _eventGoals

    fun addGoal(index: String, value: String) {
        val newMap = _eventGoals.value ?: mutableMapOf()
        newMap[index] = value
        _eventGoals.postValue(newMap)
    }

    fun removeGoal(index: String) {
        val newMap = _eventGoals.value!!
        newMap.remove(index)
        _eventGoals.postValue(newMap)
    }

    private val _eventCoords = MutableLiveData<MutableMap<MapObject, DatabaseMethods.DataClasses.MapObject>>()
    val eventCoords: LiveData<MutableMap<MapObject, DatabaseMethods.DataClasses.MapObject>> get() = _eventCoords

    fun addCoord(obj: MapObject, coord: DatabaseMethods.DataClasses.MapObject) {
        val newList = _eventCoords.value ?: mutableMapOf()
        newList[obj] = coord
        _eventCoords.postValue(newList)
    }

    fun getCoord(obj: MapObject): DatabaseMethods.DataClasses.MapObject {
        return _eventCoords.value!![obj]!!
    }
    fun removeCoord(obj: MapObject) {
        _eventCoords.value!!.remove(obj)
    }




    fun applyEventData(eventName: String = "", eventAbout: String? = null, eventFilters: String = "", eventStart: String = "") {
        val current = eventData.value
//        val name = current?.eventName ?: eventName
//        val about = current?.eventAbout ?: eventAbout
//        val filters = current?.filters ?: eventFilters
//        val start = current?.filters ?: eventStart
//
//        current.
    }
}