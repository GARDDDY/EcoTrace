package com.gy.ecotrace.ui.ratings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class RatingsViewModel(private val repository: Repository) : ViewModel() {

    private val _users = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Rating>>()
    val users: LiveData<MutableList<DatabaseMethods.DataClasses.Rating>> get() = _users

    fun getRating(userId: String) {
        viewModelScope.launch {
            _users.postValue(repository.getUserRating(userId))
        }
    }
}