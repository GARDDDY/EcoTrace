package com.gy.ecotrace.ui.more.ecocalculator

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class EcoResultsViewModel(private val repository: Repository): ViewModel() {

    fun getNumCalculatorImages(callback: (String) -> Unit) {
        viewModelScope.launch {
            callback(repository.getCountCalculators())
        }
    }


    fun getCalcImage(calcType: Int, image: Int, callback: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            callback(repository.getCalcImage(calcType, image))
        }
    }

    fun getCalcAdvices(calcType: Int, image: Int, callback: (Array<String>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getCalcAdvices(calcType, image))
        }
    }

}