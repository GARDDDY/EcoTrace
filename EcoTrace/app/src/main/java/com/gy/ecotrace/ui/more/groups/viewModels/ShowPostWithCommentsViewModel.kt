package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.CommentEvent
import com.gy.ecotrace.network.dto.GroupComment
import com.gy.ecotrace.repos.GroupRepository
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class ShowPostWithCommentsViewModel @Inject constructor(
    private val repository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var postId: Long = 0
    var groupId: Long = 0
    var myRole: String? = null

    var textComment: String? = null
    var imageComment: Bitmap? = null

    private val _comments = MutableLiveData<List<GroupComment>>()
    val comments: LiveData<List<GroupComment>> get() = _comments

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

    private val _newComment = MutableLiveData<GroupComment>()
    val newComment: LiveData<GroupComment> get() = _newComment
    private val _deletedComment = MutableLiveData<Long>()
    val deletedComment: LiveData<Long> get() = _deletedComment

    private var after: Long? = null
    var foundAll = false
    private var observing = false

    fun loadRole() {
        viewModelScope.launch {
            myRole = runCatching {
                repository.cachedGroup(groupId)?.myRole ?: repository.refreshGroup(groupId).myRole
            }.getOrNull()
        }
    }

    fun loadComments() {
        viewModelScope.launch {
            val page = runCatching { repository.comments(postId, after) }.getOrNull() ?: return@launch
            foundAll = !page.hasMore
            page.items.lastOrNull()?.let { after = it.id }
            _comments.postValue(page.items)
        }
    }

    fun observeComments() {
        if (observing) return
        observing = true
        viewModelScope.launch {
            repository.commentEvents(postId).collect { ev ->
                when (ev) {
                    is CommentEvent.New -> _newComment.postValue(ev.comment)
                    is CommentEvent.Deleted -> _deletedComment.postValue(ev.id)
                }
            }
        }
    }

    fun sendComment(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { repository.createComment(postId, textComment, imageComment?.toPng()) }
            result.getOrNull()?.let { _newComment.postValue(it) }
            callback(result.isSuccess)
        }
    }

    fun deleteComment(commentId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch { callback(runCatching { repository.deleteComment(commentId) }.getOrDefault(false)) }
    }

    fun deletePost(callback: (Boolean) -> Unit) {
        viewModelScope.launch { callback(runCatching { repository.deletePost(postId) }.getOrDefault(false)) }
    }

    private fun Bitmap.toPng(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
