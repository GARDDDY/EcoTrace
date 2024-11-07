package com.gy.ecotrace.ui.news

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class NewsViewModel(private val repository: Repository) : ViewModel() {

    private val _data = MutableLiveData<MutableList<Int>>()
    val data: LiveData<MutableList<Int>> get() = _data

    fun getUpdates(since: Long, callback: (String) -> Unit) {
        viewModelScope.launch {
            val updates = repository.getUserUpdates(since)
            Log.d("res", updates.toString())
            _data.postValue(updates.first)
            callback(updates.second)
        }
    }

    private val _sites = MutableLiveData<MutableList<HashMap<String, String?>>>()
    val sites: LiveData<MutableList<HashMap<String, String?>>> get() = _sites
    fun getNews(undesirable: String) {
        viewModelScope.launch {
            val webNews = repository.getWebNews(undesirable)
            _sites.postValue(webNews)
        }
    }

    private val _edu = MutableLiveData<MutableList<Int>>()
    val edu: LiveData<MutableList<Int>> get() = _edu
    fun getEducations() {
        viewModelScope.launch {
            val educations = repository.getUserEducations()
            _edu.postValue(educations)
        }
    }

    private fun decodeUnicode(unicodeStr: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < unicodeStr.length) {
            val ch = unicodeStr[i]
            when {
                ch == '\\' && i + 1 < unicodeStr.length && unicodeStr[i + 1] == 'u' -> {
                    val code = unicodeStr.substring(i + 2, i + 6).toInt(16)
                    sb.append(code.toChar())
                    i += 6
                }
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    fun getText(url: String, callback: (String) -> Unit) {
        viewModelScope.launch {
            val translation = repository.getTranslation(url)
            val translatedString = decodeUnicode(translation.getAsJsonPrimitive("destination-text").toString()).trim('"')
            callback(translatedString)
        }
    }

}