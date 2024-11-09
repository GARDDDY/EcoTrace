package com.gy.ecotrace.ui.more.profile.change

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.PrivacyRequest
import com.gy.ecotrace.network.dto.ProfileEditRequest
import com.gy.ecotrace.network.dto.RemoteUser
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileDraft(
    var username: String? = null,
    var aboutMe: String? = null,
    var countryCode: String? = null,
    var fullname: String? = null,
    var filters: MutableList<Int> = mutableListOf(),
    var nameVisibility: String? = null,
    var countryVisibility: String? = null,
    var friendsVisibility: String? = null,
    var groupsVisibility: String? = null
)

@HiltViewModel
class ChangeProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _profile = MutableLiveData<RemoteUser?>()
    val profile: LiveData<RemoteUser?> get() = _profile

    val draft = ProfileDraft()
    var avatarPng: ByteArray? = null
    private var initialized = false

    private var original: ProfileDraft? = null

    var firstUsername: String = ""
    var firstAboutMe: String? = null

    fun load() {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                if (user != null && !initialized) {
                    initialized = true
                    draft.username = user.username
                    draft.aboutMe = user.aboutMe
                    draft.countryCode = user.countryCode
                    draft.fullname = user.fullname
                    draft.filters = user.filters?.toMutableList() ?: mutableListOf()
                    draft.nameVisibility = user.nameVisibility
                    draft.countryVisibility = user.countryVisibility
                    draft.friendsVisibility = user.friendsVisibility
                    draft.groupsVisibility = user.groupsVisibility
                    original = draft.copy(filters = draft.filters.toMutableList())
                    firstUsername = user.username
                    firstAboutMe = user.aboutMe
                }
                _profile.postValue(user)
            }
        }
        viewModelScope.launch { runCatching { userRepository.refreshCurrentUser() } }
    }

    fun isDirty(): Boolean = avatarPng != null || draft != original

    fun save(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val edit = ProfileEditRequest(
                username = draft.username,
                aboutMe = draft.aboutMe,
                countryCode = draft.countryCode,
                fullname = draft.fullname,
                filters = draft.filters
            )
            val okProfile = runCatching { userRepository.updateProfile(edit, avatarPng) }.isSuccess

            // Бэк отвечает 400, если запрос приватности пустой — шлём только когда есть что слать.
            val hasPrivacy = draft.nameVisibility != null || draft.countryVisibility != null ||
                draft.friendsVisibility != null || draft.groupsVisibility != null
            val okPrivacy = !hasPrivacy || runCatching {
                userRepository.updatePrivacy(
                    PrivacyRequest(
                        nameVisibility = draft.nameVisibility,
                        countryVisibility = draft.countryVisibility,
                        friendsVisibility = draft.friendsVisibility,
                        groupsVisibility = draft.groupsVisibility
                    )
                )
            }.isSuccess

            if (okProfile && okPrivacy) {
                avatarPng = null
                original = draft.copy(filters = draft.filters.toMutableList())
            }
            callback(okProfile && okPrivacy)
        }
    }

    fun checkUsername(username: String?, callback: (Boolean) -> Unit) {
        callback(!username.isNullOrBlank())
    }
}
