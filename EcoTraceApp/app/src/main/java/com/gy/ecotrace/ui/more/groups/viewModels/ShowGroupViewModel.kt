package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.GroupRepository
import kotlinx.coroutines.launch

class ShowGroupViewModel(private val repository: Repository) : ViewModel() {
    companion object {
        private var instance : ShowGroupViewModel? = null
        fun getInstance(repository: Repository, shouldCreateNew: Boolean = false, isMain: Boolean = false): ShowGroupViewModel {
            if (shouldCreateNew) {
                instance = null
            }
            return instance ?: synchronized(ShowGroupViewModel::class.java) {
                instance ?: ShowGroupViewModel(repository).also { instance = it }
            }
        }
    }

    // get groups/groupId
    private val _group = MutableLiveData<DatabaseMethods.DataClasses.Group>()
    val group: LiveData<DatabaseMethods.DataClasses.Group> get() = _group
    fun getGroup(groupId: String) {
        viewModelScope.launch {
            val groupData = repository.getGroup(groupId)
            _group.postValue(groupData)
        }
    }

    // get and observe groups/groupId/posts
    private val _allFoundPosts = MutableLiveData<MutableMap<String, DatabaseMethods.DataClasses.Post>>()
    val allFoundPosts: LiveData<MutableMap<String, DatabaseMethods.DataClasses.Post>> get() = _allFoundPosts
    var lastPostId: Triple<Boolean, String?, Int> = Triple(true, null, 1)
    private var lastFoundMax = "0"
    private var showedEvents: MutableList<String> = mutableListOf()


    fun getAndObservePosts(groupId: String) {
        viewModelScope.launch {
            val posts = repository.getPosts(groupId, lastPostId)
            if (posts.first.isNotEmpty()) {
                lastPostId = posts.second
                if (lastFoundMax == "0") {
                    lastFoundMax = posts.first.first().postId
                }
                Log.d("testetetstetste", posts.toString())
                addNew(posts.first)
                observePosts(groupId)
            } else {
                _allFoundPosts.postValue(mutableMapOf())
            }
        }
    }

    private fun observePosts(groupId: String) {
        repository.observePosts(groupId, showedEvents, lastFoundMax) {
            Log.d("testobserve", it.toString())
            when (it.first) {
                true -> {
                    Log.d("observing", "added new")
                    viewModelScope.launch {
                        addNew(mutableListOf(it.second), false)
                    }
                }
                false -> {
                    Log.d("observing", "del old")
                    val old = _allFoundPosts.value ?: mutableMapOf()
                    old.remove(it.second.postId)
                    Log.d("tetruaywe", old.toString())
                    _allFoundPosts.postValue(old)
                    showedEvents.remove(it.second.postId)
                }
            }
        }
    }



    // add post after fetch
    private suspend fun addNew(posts: List<DatabaseMethods.DataClasses.Post>, back: Boolean = true) {
        var currents = _allFoundPosts.value ?: mutableMapOf()
        val newEvents = posts.filter { post ->
            !showedEvents.contains(post.postId) && !currents.containsKey(post.postId)
        }
        newEvents.forEach {
            it.postCreatorName = GroupRepository.Functions().getUsernameOnly(it.postCreatorId)
        }
        Log.d("test", newEvents.toString())
        Log.d("test", showedEvents.toString())
        if (back) {
            Log.d("add to", "back")
            newEvents.forEach {
                currents[it.postId] = it
            }
//            _allFoundPosts.postValue(currents)
//            showedEvents.addAll(newEvents.map { it.postId })
        } else {
            Log.d("add to", "front")
            val newCurrents = mutableMapOf<String, DatabaseMethods.DataClasses.Post>()
            newEvents.forEach {
                newCurrents[it.postId] = it
            }
            newCurrents.putAll(currents)
            currents = newCurrents
//            _allFoundPosts.postValue(currents)
//            showedEvents.addAll(newEvents.map { it.postId })
        }
        _allFoundPosts.postValue(currents)
        showedEvents.addAll(newEvents.map { it.postId })
    }

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

    fun userRole(userId: String, groupId: String, callback: (Int) -> Unit) {
        viewModelScope.launch {
            callback(GroupRepository.Functions().userRoleAtLeastModerator(userId, groupId))
        }
    }






    private var lastComment: Pair<String?, String?> = Pair(null, null)
    private val _allFoundComments = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Comment>>()
    val allFoundComments: LiveData<MutableList<DatabaseMethods.DataClasses.Comment>> get() = _allFoundComments
    private var showedComments: MutableList<String> = mutableListOf()
    fun getPostComments(groupId: String, postId: String, searchAgain: Boolean = false) { // comments todo
        if (searchAgain) {
            _allFoundComments.value!!.clear()
        }
        viewModelScope.launch {
            val comments = repository.getComments(groupId, postId, lastComment)
            val newComments = comments.first.filter {
                !showedEvents.contains(it.commentId)
            }
            newComments.forEach {
                it.commentCreatorName = GroupRepository.Functions().getUsernameOnly(it.commentCreatorId)
            }
            val currents = _allFoundComments.value ?: mutableListOf()
            currents.addAll(newComments)
            _allFoundComments.postValue(currents)
            showedComments.addAll(newComments.map { it.commentId })
        }
    }

    fun sendComment(groupId: String, postId: String, userId: String, textContent: String?, imageURI: String?) { // comments todo
        if (textContent.isNullOrEmpty() && imageURI.isNullOrBlank()) {
            return
        }
//        viewModelScope.launch {
            repository.sendComment(groupId, postId, userId, textContent, imageURI) // todo
//            addComment(groupId, postId, textContent, imageURI)
//        }
    }

    private val _userInGroup = MutableLiveData<Boolean>()
    val userInGroup: LiveData<Boolean> get() = _userInGroup

    fun isUserInThisGroup(userId: String, groupId: String) {
        viewModelScope.launch {
            _userInGroup.postValue(repository.isUserInGroup(userId, groupId))
        }
    }

    fun leaveGroup(userId: String, groupId: String) {
        viewModelScope.launch {
            _userInGroup.postValue(!repository.leaveGroup(userId, groupId))
        }
    }

    fun joinGroup(userId: String, groupId: String) {
        viewModelScope.launch {
            _userInGroup.postValue(repository.joinGroup(userId, groupId))
        }
    }
}