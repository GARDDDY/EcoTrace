package com.gy.ecotrace.ui.more.ecocalculator

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class EcoResultsViewModel(private val repository: Repository): ViewModel() {

    fun getNumCalculatorImages(calcType: Int, callback: (Int) -> Unit) {
        viewModelScope.launch {
            callback(repository.getCountCalculators(calcType))
        }
    }


    fun getCalcImage(calcType: Int, image: Int, callback: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            callback(repository.getCalcImage(calcType, image))
        }
    }

    fun getCalcAdvices(calcType: Int) {

    }

}