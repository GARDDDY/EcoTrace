package com.gy.ecotrace.ui.more.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class SignHubViewModel(private val repository: Repository): ViewModel() {

    fun getUserEmail(login: String, password: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            callback(repository.getUserEmail(login, password))
        }
    }
}