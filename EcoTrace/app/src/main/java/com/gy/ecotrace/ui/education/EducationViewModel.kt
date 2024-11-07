package com.gy.ecotrace.ui.education

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch
import okhttp3.Callback

class EducationViewModel : ViewModel() {

    private val _facts = MutableLiveData<Boolean>()
    val facts: LiveData<Boolean> get() = _facts

    fun goto(){
        _facts.postValue(true)
    }

    private val useds = mutableMapOf<Int, Boolean>()

    fun isUsed(index: Int): Boolean {
        return useds[index] ?: false
    }
    fun used(index: Int) {
        useds[index] = true
    }

    private var count: Int = -1

    fun count(count: Int){
        this.count = count
    }

    fun isAllCompleted(): Boolean {
        return useds.size == count
    }

    private var isEnded = false
    private var anses: HashMap<String, Any?> = hashMapOf()

    private var numCorrect = 0
    fun controlEnded(data: HashMap<String, DatabaseMethods.DataClasses.EduTask>) {
        isEnded = true

        for ((n, d) in data) {
            if (d.answer is Number && d.answer.toString().toDouble().toInt() == (anses[n] ?: -1).toString().toInt()
                || d.answer is String && d.answer.toString() == anses[n].toString()) {
                numCorrect++
            }
        }
    }

    fun corrects(): Int {
        return numCorrect
    }


    fun control(num: String, ans: Any) {
        anses[num] = ans
    }

    fun end(eduType: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods()).sendEdu(eduType))
        }
    }
}