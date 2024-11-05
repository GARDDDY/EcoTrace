package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class CreateGroupViewModel(private val repository: Repository): ViewModel() { // todo Group -> GroupChange
    private val _groupData = MutableLiveData<DatabaseMethods.DataClasses.GroupChange>()
    val groupData: LiveData<DatabaseMethods.DataClasses.GroupChange> get() = _groupData

    var groupImage: Bitmap? = null

    var groupClass = DatabaseMethods.DataClasses.GroupChange()

    var groupRulesText: String? = null
    var groupRulesImage: Bitmap? = null

    fun applyGroupData(group: DatabaseMethods.DataClasses.GroupChange, rules: Array<String?> = arrayOf(null, null)) {
        groupClass = group
        if (rules[0] != null) {
            groupClass.groupRulesText = rules[0]
        }
        if (rules[1] != null) {
            groupClass.groupRulesImage = rules[1]
        }
        _groupData.postValue(group)
    }

    fun applyGroupData(group: DatabaseMethods.DataClasses.Group, rules: Array<String?>) {
        val gr = DatabaseMethods.DataClasses.GroupChange(
            groupId = group.groupId,
            groupName = group.groupName,
            groupAbout = group.groupAbout,
            filters = group.filters,
            groupType = group.groupType
        )

        applyGroupData(gr, rules)
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