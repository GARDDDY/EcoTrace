package com.gy.ecotrace.ui.more.events.showtabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.events.ShowEventViewModel

class ShowEventViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShowEventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShowEventViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}