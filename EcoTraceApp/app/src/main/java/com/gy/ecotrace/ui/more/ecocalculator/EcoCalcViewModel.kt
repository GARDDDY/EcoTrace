package com.gy.ecotrace.ui.more.ecocalculator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository

class EcoCalcViewModel(private val repository: Repository) : ViewModel() {

    var currentPage = 0

    private val _answers = MutableLiveData<
            MutableList<DatabaseMethods.DataClasses.EcoCalcSaveData>>()
    val answers: LiveData<
            MutableList<DatabaseMethods.DataClasses.EcoCalcSaveData>>
        get() = _answers

    private val _additional = MutableLiveData<MutableMap<String, Double>>()
    val additional: LiveData<MutableMap<String, Double>> get() = _additional

    fun addAnswer(value: DatabaseMethods.DataClasses.EcoCalcSaveData) {
        val previous = _answers.value!! //?: mutableListOf()
//        val x = previous[value.question]?: DatabaseMethods.DataClasses.EcoCalcSaveData()
        previous[value.question] = value
        _answers.postValue(previous)
    }


    fun saveAdditionalData() {

    }

    fun init(amount: Int) {
        _answers.value = MutableList(amount)
            { DatabaseMethods.DataClasses.EcoCalcSaveData()}
    }


    fun saveData(calcType: Int, callback: (Boolean) -> Unit) {
        repository.saveEcoData(answers.value!!, calcType) {
            callback(it)
        }
    }


}