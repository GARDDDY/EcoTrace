package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class ShowPostWithCommentsViewModel(private val repository: Repository): ViewModel() {

    var postId = -1
    lateinit var groupId: String

    var textComment: String? = null
    var imageComment: Bitmap? = null


    private val _comments = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Comment>>()
    val comments: LiveData<MutableList<DatabaseMethods.DataClasses.Comment>> get() = _comments

    private var lastGot: Int? = null

    fun getComments() {
        viewModelScope.launch {
            val comments = repository.getComments(groupId, postId, lastGot)
            _comments.postValue(comments)
        }
    }
    fun getRole(callback: (Int) -> Unit) {
        viewModelScope.launch {
            callback(repository.getUserRoleInGroup(groupId))
        }
    }

    fun deletePost(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.deletePost(groupId, postId))
        }
    }

    fun deleteComment(commentId: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.deleteComment(groupId, postId, commentId))
        }
    }


    fun sendComment(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.sendComment(groupId, postId, textComment, imageComment))
        }
    }

}