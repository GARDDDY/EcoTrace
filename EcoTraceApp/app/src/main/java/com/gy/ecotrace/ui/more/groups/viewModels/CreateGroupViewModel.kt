package com.gy.ecotrace.ui.more.groups.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class CreateGroupViewModel(private val repository: Repository): ViewModel() {
    private val _groupData = MutableLiveData<DatabaseMethods.DataClasses.Group>()
    val groupData: LiveData<DatabaseMethods.DataClasses.Group> get() = _groupData

    private var groupClass = DatabaseMethods.DataClasses.Group()

    fun loadSavedData(savedData: DatabaseMethods.DataClasses.Group) {
        Log.d("Loading saved group data", savedData.groupName)
        groupClass = savedData
        _groupData.postValue(savedData)
    }

    fun applyGroupData(
        groupName: String? = groupClass.groupName,
        groupAbout: String? = groupClass.groupAbout,
        groupType: Int? = groupClass.groupType,
        groupTags: String = groupClass.filters
    ) {
        groupClass.groupName = groupName ?: ""
        groupClass.groupAbout = groupAbout
        groupClass.groupType = groupType ?: 2
        groupClass.filters = groupTags

        _groupData.postValue(groupClass)
    }

    fun isGroupNameAvailable(groupName: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.isGroupNameAvailable(groupName))
        }
    }

    fun createGroup(userId: String) {
        viewModelScope.launch {
            groupClass.groupCreatorId = userId
            repository.createGroup(groupClass)
        }
    }
}