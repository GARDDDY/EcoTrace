package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.GroupRepository
import kotlinx.coroutines.launch

class CreateGroupPostMenuViewModel(private val repository: Repository) : ViewModel() {


    fun uploadImage(folder: String, imageId: String, imageData: Bitmap, callback: (String) -> Unit) {
        viewModelScope.launch {
            callback(GroupRepository.Functions().uploadImage(folder, imageId, imageData))
        }

    }

    fun createPost(groupId: String, userId: String, textContent: String?, imageURI: String?) {
        viewModelScope.launch {
            GroupRepository.Functions().createPost(groupId, userId, textContent, imageURI)
        }
    }

    fun deletePost(groupId: String, postId: String) {
        viewModelScope.launch {
            GroupRepository.Functions().deletePost(groupId, postId)
        }
    }

    fun userRole(groupId: String, userId: String, callback: (Int) -> Unit) {
        viewModelScope.launch {
            callback(GroupRepository.Functions().userRoleAtLeastModerator(userId, groupId))
        }
    }

    private var groups: MutableList<DatabaseMethods.UserDatabaseMethods.UserGroup>? = null
    fun getUserGroups(userId: String, callback: (MutableList<DatabaseMethods.UserDatabaseMethods.UserGroup>) -> Unit) {
        viewModelScope.launch {
            if (groups != null) callback(groups!!)
            else {
                groups = repository.getUserGroups(userId, null)
                callback(groups!!)
            }
            Log.d("callbacked", groups.toString())
        }
    }

    private var events: MutableList<DatabaseMethods.UserDatabaseMethods.UserEvent>? = null
    fun getUserEvents(userId: String, callback: (MutableList<DatabaseMethods.UserDatabaseMethods.UserEvent>) -> Unit) {
        viewModelScope.launch {
            callback(events ?: repository.getUserEvents(userId, null) ?: mutableListOf())
        }
    }
}