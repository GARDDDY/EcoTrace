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
    private val _userFriends = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Friendship>?>()
    val userFriends: LiveData<MutableList<DatabaseMethods.DataClasses.Friendship>?> get() = _userFriends

    private var lastFoundFriend: String? = null
    var foundAll = false
    fun getUserFriends(userId: String) {
        viewModelScope.launch {
            val dataFriends = repository.getUserFriends(userId, lastFoundFriend)
            _userFriends.postValue(dataFriends)
        }
    }

    private val filtersSearchNew = MutableList(DatabaseMethods.DataClasses.UserFiltersSearchBy.size) { false }
    fun reapplyFilter(filterIndex: Int) {
        filtersSearchNew[filterIndex] = !filtersSearchNew[filterIndex]
    }

    private val _usersFoundFilter = MutableLiveData<MutableList<DatabaseMethods.DataClasses.FiltersFriendship?>>()
    val usersFoundFilter: LiveData<MutableList<DatabaseMethods.DataClasses.FiltersFriendship?>> get() = _usersFoundFilter

    private var lastFound: String? = null
    var foundAllUsers = false
    var filterName: String? = null
    fun findAllUsers() {
        viewModelScope.launch {
            val filters = filtersSearchNew.mapIndexedNotNull { index, value ->
                if (value) (index + 1).toString() else null
            }.joinToString(",")

            val data = repository.getObjectsFiltered(filters, lastFound, "users", filterName)
            foundAllUsers = data.first.second
            lastFound = data.first.first

            _usersFoundFilter.postValue(data.second)
        }
    }


}