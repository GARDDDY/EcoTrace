package com.gy.ecotrace.ui.more.friends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.FriendItem
import com.gy.ecotrace.network.dto.RemoteUser
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearcherViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _friends = MutableLiveData<MutableList<FriendItem>>()
    val friends: LiveData<MutableList<FriendItem>> get() = _friends

    private var friendsAfter: Long? = null
    var foundAllFriends = false
    var newFriends = true
    var updFriends = false

    var friendsSort: String? = null

    fun loadFriends(userId: Long, reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) { friendsAfter = null; foundAllFriends = false; newFriends = true }
            val page = runCatching {
                repository.getFriends(userId, friendsSort, friendsAfter)
            }.getOrNull() ?: run { updFriends = false; return@launch }
            friendsAfter = page.items.lastOrNull()?.id
            if (!page.hasMore) foundAllFriends = true
            val prev = if (reset) emptyList() else (_friends.value ?: emptyList())
            _friends.postValue((prev + page.items).toMutableList())
            updFriends = false
        }
    }

    private val _results = MutableLiveData<MutableList<RemoteUser>>()
    val results: LiveData<MutableList<RemoteUser>> get() = _results

    var query: String? = null
    var filter: String? = null
    var sortBy: String? = null
    var commonGroup: Boolean? = null
    var commonEvent: Boolean? = null
    private var searchAfter: Long? = null
    var foundAllUsers = false
    var updUsers = false

    fun search(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) { searchAfter = null; foundAllUsers = false }
            val page = runCatching {
                repository.searchUsers(
                    query?.takeIf { it.isNotBlank() }, filter, sortBy, commonGroup, commonEvent, searchAfter
                )
            }.getOrNull() ?: run { updUsers = false; return@launch }
            searchAfter = page.items.lastOrNull()?.id
            if (!page.hasMore) foundAllUsers = true
            val prev = if (reset) emptyList() else (_results.value ?: emptyList())
            _results.postValue((prev + page.items).toMutableList())
            updUsers = false
        }
    }
}
