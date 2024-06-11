package com.gy.ecotrace.ui.more.friends

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class SearcherViewModel(private val repository: Repository) : ViewModel() {
    private val _userFriends = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Friendship>>()
    val stringUserFriendsFilter = MutableLiveData<String>()
    val filteredUserFriends = MediatorLiveData<List<DatabaseMethods.DataClasses.Friendship>>().apply {
        addSource(_userFriends) { filterUserFriends() }
        addSource(stringUserFriendsFilter) { filterUserFriends() }
    }

    private fun filterUserFriends() {
        val query = stringUserFriendsFilter.value ?: ""
        val friends = _userFriends.value ?: mutableListOf()
        filteredUserFriends.value = if (query.isEmpty()) {
            friends
        } else {
            friends.filter { it.username.startsWith(query, ignoreCase = true) }
        }
    }

    fun getUserFriends(userId: String) {
        viewModelScope.launch {
            val friends = repository.getFriends(userId)
            _userFriends.postValue(friends)
        }
    }

    val tags = mutableListOf<Triple<String, String, Pair<String, String>>>(
        Triple("Активный", "\"Активный\" пользователь всегдат\nготов присоединиться к новым мероприятиям", Pair("#00FA9A", "#1A3329")),
        Triple("Веселый", "\"Веселый\" пользователь", Pair("#FF7F50", "#33201A")),
        Triple("Ветеран", "Пользователь, зарегистрировавшийся довольно давно (награда)", Pair("#00CED1", "#1A3333")),
        Triple("В сети", "Пользователь часто бывает в сети", Pair("#DA70D6", "#331A32"))
    )

    private val filtersSearchNew = MutableList(tags.size) { false }
    fun reapplyFilter(filterIndex: Int) {
        filtersSearchNew[filterIndex] = !filtersSearchNew[filterIndex]
    }

    private val _userFoundFilter = MutableLiveData<MutableList<DatabaseMethods.DataClasses.FiltersFriendship>>()
    val stringFriendsFilter = MutableLiveData<String>()
    val filteredFriends = MediatorLiveData<List<DatabaseMethods.DataClasses.FiltersFriendship>>().apply {
        addSource(_userFoundFilter) { filterFriends() }
        addSource(stringFriendsFilter) { filterFriends() }
    }

    private fun filterFriends() {
        val query = stringFriendsFilter.value ?: ""
        val friends = _userFoundFilter.value ?: mutableListOf()
        filteredFriends.value = if (query.isEmpty()) {
            friends
        } else {
            friends.filter { it.username.startsWith(query, ignoreCase = true) }
        }
    }

    private var lastFound: String? = null
    private var lastFilters: MutableList<Int>? = null
    fun searchFor() {
        val filters = mutableListOf<Int>()
        filtersSearchNew.forEachIndexed { index, it ->
            if (it) {
                filters.add(index)
            }
        }

        if (filters.size == 0) {
            _userFoundFilter.postValue(mutableListOf())
            return
        }

        var newTags = true
        if (filters == lastFilters) newTags = false

        viewModelScope.launch {
            if (newTags) lastFound = null
            val users = repository.findUsersWithFilters(filters, lastFound)
            lastFound = users.first
            val currents = (if (newTags) {
                mutableListOf()
            } else {
                _userFoundFilter.value ?: mutableListOf()
            }).apply {
                addAll(users.second)
            }
            Log.d("jj$newTags", currents.toString())
            lastFilters = filters
            _userFoundFilter.postValue(currents)
        }
    }


}