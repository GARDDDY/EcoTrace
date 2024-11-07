package com.gy.ecotrace.ui.more.profile.change

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.customs.ETAuth
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
            classRules.clear()
            classRules.putAll(rules)
            _rules.postValue(classRules.toMutableMap())
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
            _private.postValue(private.copy())
        }
    }

    fun applyPrivate(private: DatabaseMethods.UserDatabaseMethods.UserPrivate) {
        classPrivate = private
    }

    private val _public = MutableLiveData<DatabaseMethods.UserDatabaseMethods.UserChange>()
    val public: LiveData<DatabaseMethods.UserDatabaseMethods.UserChange> get() = _public
    var firstUsername = "NONE?"
    var firstAboutMe: String? = null
    var classPublic = DatabaseMethods.UserDatabaseMethods.UserChange()

    var profileImage: Bitmap? = null

    fun getImage(userId: String): String {
        return DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", userId)
    }

    fun getPublic() {
        viewModelScope.launch {
            val userData = repository.getUser(null)
            if (userData != null) {
                firstUsername = userData.username
                firstAboutMe = userData.about_me
                _public.postValue(DatabaseMethods.UserDatabaseMethods.UserChange(
                    username = userData.username,
                    country_code = userData.country_code,
                    about_me = userData.about_me,
                    filters = userData.filters,
                    fullname = userData.fullname
                ))
            }
        }
    }

    fun applyPublic(public: DatabaseMethods.UserDatabaseMethods.UserChange) {
        classPublic = public
    }

    fun checkUsername(username: String?, callback: (Boolean) -> Unit) {
        if (username == null) {
            callback(false)
        }
        viewModelScope.launch {
            callback(false)//repository.isUsernameAvailable(username)) todo
        }
    }


    fun save(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (classPublic != public.value) {
                Log.d("changes", "in public")
                ETAuth.getInstance().set(classPublic) {
                    if (!it) { }
                }
            }
            if (classRules != rules.value) {
                Log.d("changes", "in rules")
                ETAuth.getInstance().setRules(classRules) {}
            }
            callback(true)
        }
    }

}