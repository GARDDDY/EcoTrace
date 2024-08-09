package com.gy.ecotrace.ui.more.ecocalculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.db.Repository

class EcoCalcViewModelFactory (
        private val repository: Repository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EcoCalcViewModel::class.java)) {
                return EcoCalcViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
}