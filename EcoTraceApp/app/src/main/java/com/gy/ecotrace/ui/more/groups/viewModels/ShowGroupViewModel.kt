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

class ShowGroupViewModel(private val repository: Repository) : ViewModel() {
    lateinit var groupId: String

    private val _group = MutableLiveData<DatabaseMethods.DataClasses.Group>()
    val group: LiveData<DatabaseMethods.DataClasses.Group> get() = _group
    fun getGroup() {
        viewModelScope.launch {
            val groupData = repository.getGroup(groupId)
            _group.postValue(groupData)
        }
    }

    fun getRole(callback: (Int) -> Unit) {
        viewModelScope.launch {
            callback(repository.getUserRoleInGroup(groupId))
        }
    }

    private val _allFoundPosts = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Post>?>()
    val allFoundPosts: LiveData<MutableList<DatabaseMethods.DataClasses.Post>?> get() = _allFoundPosts

    private var lastPostId: Int? = null
    var foundAll = false

    var fetching = false


    fun getOldPosts(new: Boolean = false) {
        viewModelScope.launch {
            if (new) {
                lastPostId = null
            }
            val posts = repository.getPosts(groupId, lastPostId)
            foundAll = !posts.first
            lastPostId = if ((posts.second?.toMutableList() ?: mutableListOf()).isNotEmpty()) posts.second?.last()?.postId else null
            if (lastNewPostId == null) {
                lastNewPostId = if ((posts.second?.toMutableList()
                        ?: mutableListOf()).isNotEmpty()
                ) posts.second?.first()?.postId else null
            }
            Log.d("test", lastNewPostId.toString())
            _allFoundPosts.postValue(posts.second?.toMutableList())
        }
    }

    var isNew = false

    fun getNumComments(postId: Int, callback: (Int) -> Unit) {
        viewModelScope.launch {
            callback(repository.getNumComments(groupId, postId))
        }
    }

    private var lastNewPostId: Int? = null
    fun getNewPosts() {
        viewModelScope.launch {
            val posts = repository.getNewPosts(groupId, lastNewPostId)
            Log.d("testgothere", posts?.toMutableList().toString())
            if (posts != null) {
                lastNewPostId = posts.last().postId
                isNew = true
                _allFoundPosts.postValue(posts.toMutableList())
            }
        }
    }

    fun createPost(textContent: String?, imageData: Bitmap?, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val created = repository.createPost(groupId, textContent, imageData)
            if (created) {
                lastPostId = null
                foundAll = false
                getOldPosts()
            }
            callback(created)
        }
    }

    fun deletePost(postId: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.deletePost(groupId, postId))
        }
    }

    fun sendComment(postId: Int, textContent: String?, callback: (Boolean) -> Unit) { // todo
        if (textContent.isNullOrEmpty()) {
            return
        }
        viewModelScope.launch {
            val sent = repository.sendComment(groupId, postId, textContent, null)
            callback(sent) // todo
        }
    }

    private val _userInGroup = MutableLiveData<Boolean>()
    val userInGroup: LiveData<Boolean> get() = _userInGroup

    fun isUserInThisGroup() {
        viewModelScope.launch {
            _userInGroup.postValue(repository.isUserInGroup(groupId))
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _userInGroup.postValue(!repository.leaveGroup(groupId))
        }
    }

    fun joinGroup() {
        viewModelScope.launch {
            _userInGroup.postValue(repository.joinGroup(groupId))
        }
    }

    fun delete(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.deleteGroup(groupId))
        }
    }
}