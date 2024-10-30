package com.gy.ecotrace.ui.more.profile.change

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class ChangeProfileViewModel(private val repository: Repository) : ViewModel() {

    private val _rules = MutableLiveData<MutableMap<String, Int>>()
    val rules: LiveData<MutableMap<String, Int>> get() = _rules
    var classRules = mutableMapOf<String, Int>()

    fun getRules() {
        viewModelScope.launch {
            val rules = repository.getRules()
            classRules = rules
            _rules.postValue(rules)
        }
    }

    fun applyRules(rules: MutableMap<String, Int>) {
        classRules = rules
    }

    private val _private = MutableLiveData<DatabaseMethods.UserDatabaseMethods.UserPrivate>()
    val private: LiveData<DatabaseMethods.UserDatabaseMethods.UserPrivate> get() = _private
    var classPrivate = DatabaseMethods.UserDatabaseMethods.UserPrivate()

    fun getPrivate() {
        viewModelScope.launch {
            val private = repository.getPrivate()
            classPrivate = private
            _private.postValue(private)
        }
    }

    fun applyPrivate(private: DatabaseMethods.UserDatabaseMethods.UserPrivate) {
        classPrivate = private
    }

    private val _public = MutableLiveData<DatabaseMethods.UserDatabaseMethods.User>()
    val public: LiveData<DatabaseMethods.UserDatabaseMethods.User> get() = _public
    var firstUsername = "NONE?"
    var firstAboutMe: String? = null
    var classPublic = DatabaseMethods.UserDatabaseMethods.User()

    var profileImage: Bitmap? = null

    fun getImage(userId: String): String {
        return DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", userId)
    }

    fun getPublic() {
        viewModelScope.launch {
            val userData = repository.getUser(null)
            if (userData != null) {
                classPublic = userData
                firstUsername = userData.username
                firstAboutMe = userData.aboutMe
                _public.postValue(userData)
            }
        }
    }

    fun applyPublic(public: DatabaseMethods.UserDatabaseMethods.User) {
        classPublic = public
    }

    fun checkUsername(username: String?, callback: (Boolean) -> Unit) {
        if (username == null) {
            callback(false)
        }
        viewModelScope.launch {
            callback(false)//repository.isUsernameAvailable(username))
        }
    }

    fun setPassword() {

    }


    fun save(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (classPublic != public.value) {
                Log.d("changes", "in public")
//                repository.setUserPublic(classPublic)
            }
            if (classPrivate != private.value) {
                Log.d("changes", "in private")
//                repository.setUserPrivate(classPublic)
            }
            if (classRules != rules.value) {
                Log.d("changes", "in rules")
//                repository.setUserRules(classPublic)
            }
            callback(true)
        }
    }

}