package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.PostEvent
import com.gy.ecotrace.network.dto.GroupPost
import com.gy.ecotrace.network.dto.RemoteGroup
import com.gy.ecotrace.repos.GroupRepository
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class ShowGroupViewModel @Inject constructor(
    private val repository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    var groupId: Long = 0

    private val _group = MutableLiveData<RemoteGroup?>()
    val group: LiveData<RemoteGroup?> get() = _group

    private val _currentUserAvatarKey = MutableLiveData<String?>()
    val currentUserAvatarKey: LiveData<String?> get() = _currentUserAvatarKey

    private var collectingCurrentUser = false

    fun loadCurrentUserAvatar() {
        if (collectingCurrentUser) return
        collectingCurrentUser = true
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { _currentUserAvatarKey.postValue(it?.avatarKey) }
        }
    }

    private val _postsPage = MutableLiveData<List<GroupPost>>()
    val postsPage: LiveData<List<GroupPost>> get() = _postsPage

    private val _newPost = MutableLiveData<GroupPost>()
    val newPost: LiveData<GroupPost> get() = _newPost
    private val _deletedPost = MutableLiveData<Long>()
    val deletedPost: LiveData<Long> get() = _deletedPost

    private val _pinnedPost = MutableLiveData<GroupPost?>()
    val pinnedPost: LiveData<GroupPost?> get() = _pinnedPost

    val myRole: String? get() = _group.value?.myRole
    val isMember: Boolean get() = myRole != null

    private var oldestId: Long? = null
    var foundAll = false
    var fetching = false
    private var observing = false
    private var collectingGroup = false

    fun loadGroup() {
        if (!collectingGroup) {
            collectingGroup = true
            viewModelScope.launch { repository.observeGroup(groupId).collect { _group.postValue(it) } }
        }
        viewModelScope.launch { runCatching { repository.refreshGroup(groupId) } }
    }

    fun loadOldPosts(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) { oldestId = null; foundAll = false }
            if (fetching) return@launch
            fetching = true
            val page = runCatching { repository.posts(groupId, oldestId) }.getOrNull()
            fetching = false
            if (page == null) { _postsPage.postValue(emptyList()); return@launch }
            foundAll = !page.hasMore
            page.items.lastOrNull()?.let { oldestId = it.id }
            _postsPage.postValue(page.items)
        }
    }

    fun observePosts() {
        if (observing) return
        observing = true
        viewModelScope.launch {
            repository.postEvents(groupId).collect { ev ->
                when (ev) {
                    is PostEvent.New -> _newPost.postValue(ev.post)
                    is PostEvent.Deleted -> _deletedPost.postValue(ev.id)
                }
            }
        }
    }

    fun createPost(text: String?, image: Bitmap?, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { repository.createPost(groupId, text, image?.toPng()) }
            result.getOrNull()?.let { _newPost.postValue(it) }
            callback(result.isSuccess)
        }
    }

    fun deletePost(postId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch { callback(runCatching { repository.deletePost(postId) }.getOrDefault(false)) }
    }

    fun loadPinnedPost() {
        viewModelScope.launch {
            _pinnedPost.postValue(runCatching { repository.pinnedPost(groupId) }.getOrNull())
        }
    }

    fun pinPost(postId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { repository.pinPost(postId) }
            result.getOrNull()?.let { _pinnedPost.postValue(it) }
            callback(result.isSuccess)
        }
    }

    fun unpinPost(postId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { repository.unpinPost(postId) }.getOrDefault(false)
            if (ok) _pinnedPost.postValue(null)
            callback(ok)
        }
    }

    fun sendComment(postId: Long, text: String?, callback: (Boolean) -> Unit) {
        if (text.isNullOrBlank()) { callback(false); return }
        viewModelScope.launch { callback(runCatching { repository.createComment(postId, text) }.isSuccess) }
    }

    fun join(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { repository.join(groupId) }.getOrDefault(false)
            if (ok) runCatching { repository.refreshGroup(groupId) }
            callback(ok)
        }
    }

    fun leave(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { repository.leave(groupId) }.getOrDefault(false)
            if (ok) runCatching { repository.refreshGroup(groupId) }
            callback(ok)
        }
    }

    fun deleteGroup(callback: (Boolean) -> Unit) {
        viewModelScope.launch { callback(runCatching { repository.delete(groupId) }.getOrDefault(false)) }
    }

    private fun Bitmap.toPng(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
