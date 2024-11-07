package com.gy.ecotrace.ui.more.ecocalculator

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository

class EcoCalcViewModel(private val repository: Repository) : ViewModel() {

    var currentPage = 0

    private val answers: MutableMap<Int, DatabaseMethods.DataClasses.EcoCalcSaveData> = mutableMapOf()

    fun addAnswer(value: DatabaseMethods.DataClasses.EcoCalcSaveData) {
        answers[value.question] = value
        _end.postValue(isEnoughAnswers())
    }

    fun getAnswer(value: Int): DatabaseMethods.DataClasses.EcoCalcSaveData {
        return answers[value] ?: DatabaseMethods.DataClasses.EcoCalcSaveData()
    }

    private val _end = MutableLiveData<Boolean>()
    val end: LiveData<Boolean> get() = _end


    fun saveAdditionalData() {

    }

    private var allQuestions = -1
    fun init(amount: Int) {
        allQuestions = amount
    }


    fun saveData(calcType: Int, callback: (String) -> Unit) {
        repository.saveEcoData(answers.values.toMutableList(), calcType) {
            callback(it)
        }
        Log.d("save", answers.toString())

    }

    fun isEnoughAnswers(): Boolean {
        Log.d("enough", "${answers.size} --- $allQuestions")
        return answers.size == allQuestions
    }


}