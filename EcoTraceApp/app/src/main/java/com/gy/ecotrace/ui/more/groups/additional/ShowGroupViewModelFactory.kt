package com.gy.ecotrace.ui.more.groups.additional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel

class ShowGroupViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShowGroupViewModel::class.java)) {
            return ShowGroupViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}