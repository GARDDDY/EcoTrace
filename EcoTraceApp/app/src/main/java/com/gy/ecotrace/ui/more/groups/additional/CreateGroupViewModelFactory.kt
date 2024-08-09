package com.gy.ecotrace.ui.more.groups.additional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel

class CreateGroupViewModelFactory(
    private val repository: Repository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateGroupViewModel::class.java)) {
            return CreateGroupViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}