package com.gy.ecotrace.ui.more.groups.additional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel

class ShowGroupViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    companion object {
        private var instance : ShowGroupViewModelFactory? = null
        fun getInstance(repository: Repository, shouldCreateNew: Boolean = false): ShowGroupViewModelFactory {
            if (shouldCreateNew) {
                instance = null
            }
            createNew(shouldCreateNew)
            return instance ?: synchronized(ShowGroupViewModelFactory::class.java) {
                instance ?: ShowGroupViewModelFactory(repository).also { instance = it }
            }
        }
        private var createNewFlag = false
        private fun createNew(value: Boolean) {
            createNewFlag = value
        }
        fun createNew(): Boolean {
            return createNewFlag
        }
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        with(modelClass){
            when {
                isAssignableFrom(ShowGroupViewModel::class.java) -> ShowGroupViewModel.getInstance(repository, createNew())
                else -> throw IllegalArgumentException("Unknown viewModel class $modelClass")
            }
        } as T
}