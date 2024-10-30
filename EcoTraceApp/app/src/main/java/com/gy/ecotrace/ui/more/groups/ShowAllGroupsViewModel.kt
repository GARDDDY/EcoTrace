package com.gy.ecotrace.ui.more.groups

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.Globals
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class ShowAllGroupsViewModel(private val repository: Repository): ViewModel() {
    var isUpdating = false

    private val _groups = MutableLiveData<HashMap<String, String>?>()
    private var lastId: String? = null
    val groups: LiveData<HashMap<String, String>?> get() = _groups

    private val filtersSearchNew = MutableList(Globals.getInstance().getGroupsFilters().size) { false }
    fun reapplyFilter(filterIndex: Int) {
        filtersSearchNew[filterIndex] = !filtersSearchNew[filterIndex]
    }

    private val _groupFoundFilter = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Group>>()
    val groupsFound: LiveData<MutableList<DatabaseMethods.DataClasses.Group>> get() = _groupFoundFilter
    private var startAtId: String? = null
    var foundAll = false


    fun getGroups(updateAllGot: Boolean = true) {
        viewModelScope.launch {
            if (updateAllGot) {
                startAtId = null
                foundAll = false
            }

            val filters = filtersSearchNew.mapIndexedNotNull {
                    index, value -> if (value) (index + 1).toString() else null
            }.joinToString(",")


            val data = repository.findGroupsWithFilters(filters, startAtId)

            Log.d("data", data.toString())

            startAtId = data.first.first
            foundAll = data.first.second
            val groups = data.second

            _groupFoundFilter.postValue(groups)
        }
    }

    private val _userGroups = MutableLiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserGroup>>()
    private var lastIdUser: String? = null
    val userGroups: LiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserGroup>> get() = _userGroups

    fun getUserGroups(userId: String) {
        viewModelScope.launch {
            val groups = repository.getUserGroups(userId, lastIdUser)
            _userGroups.postValue(groups)
        }
    }
}