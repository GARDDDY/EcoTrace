package com.gy.ecotrace.ui.more.friends

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.Globals
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class SearcherViewModel(private val repository: Repository) : ViewModel() {
    private val _userFriends = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Friendship>?>()
    val userFriends: LiveData<MutableList<DatabaseMethods.DataClasses.Friendship>?> get() = _userFriends


    var foundAll = false
    private var fGot: String? = null
    var newFriends = true
    var updFriends = false
    fun getUserFriends(userId: String, new: Boolean = false) {
        viewModelScope.launch {
            if (new) {
                fGot = null
                _userFriends.value?.clear()
                newFriends = true
            }
            val friends = repository.getUserFriends(userId, fGot)
            fGot = try {friends.last().userId} catch (e: Exception) { null }

            foundAll = fGot == null

            _userFriends.postValue(friends)
        }
    }

    private val filtersSearchNew = MutableList(Globals.getInstance().getUserFilters().size) { false }
    fun reapplyFilter(filterIndex: Int) {
        filtersSearchNew[filterIndex] = !filtersSearchNew[filterIndex]
    }

    private val _usersFoundFilter = MutableLiveData<MutableList<DatabaseMethods.DataClasses.FiltersFriendship>?>()
    val usersFoundFilter: LiveData<MutableList<DatabaseMethods.DataClasses.FiltersFriendship>?> get() = _usersFoundFilter

    private var lastFound: String? = null
    var foundAllUsers = false
    var filterName: String? = null
    var updUsers = false
    fun findAllUsers(new: Boolean = false) {
        viewModelScope.launch {
            val filters = filtersSearchNew.mapIndexedNotNull { index, value ->
                if (value) (index + 1).toString() else null
            }.joinToString(",")

            if (new) lastFound = null

            val data = repository.findUsersWithFilters(filters, lastFound, filterName)
            foundAllUsers = data.first.second
            lastFound = data.first.first

            _usersFoundFilter.postValue(data.second)
        }
    }


}