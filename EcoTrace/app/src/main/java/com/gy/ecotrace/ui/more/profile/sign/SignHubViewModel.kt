package com.gy.ecotrace.ui.more.profile.sign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class SignHubViewModel(private val repository: Repository): ViewModel() {

    fun getUserEmail(login: String, password: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            callback(repository.getUserEmail(login, password))
        }
    }

    var user = DatabaseMethods.UserDatabaseMethods.User()

    // todo isOk*

//    fun isOkEmail(email: String, callback: (String?) -> Unit) {
//        viewModelScope.launch {
//            callback(repository.isOkEmail(email))
//        }
//    }

    fun signUp(e: String, p: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            ETAuth.getInstance().create(e, p, user) {
                callback(it)
            }
        }
    }

    fun sendForgotCode(email: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.sendForgotCode(email))
        }
    }

    fun checkCode(email: String, code: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.checkCode(email, code))
        }
    }

    fun applyPassword(email: String, code: Int, password: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.applyPassword(email, code, password))
        }
    }
}