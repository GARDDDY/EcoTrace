package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
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

    var groupImage: Bitmap? = null

    var groupClass = DatabaseMethods.DataClasses.Group()

    var groupRulesText: String? = null
    var groupRulesImage: Bitmap? = null

    fun applyGroupData(group: DatabaseMethods.DataClasses.Group) {
        groupClass = group
        _groupData.postValue(group)
    }

    fun isGroupNameAvailable(groupName: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            callback(repository.isGroupNameAvailable(groupName))
        }
    }

    fun createGroup(callback: (String?) -> Unit) {
        viewModelScope.launch {
            val status = repository.createGroup(groupClass, groupRulesText, groupRulesImage)
            callback(status)
        }
    }
}