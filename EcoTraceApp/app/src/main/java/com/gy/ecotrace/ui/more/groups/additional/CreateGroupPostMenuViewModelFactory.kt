package com.gy.ecotrace.ui.more.groups.additional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupPostMenuViewModel
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel

class CreateGroupPostMenuViewModelFactory(
    private val repository: Repository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateGroupPostMenuViewModel::class.java)) {
            return CreateGroupPostMenuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}