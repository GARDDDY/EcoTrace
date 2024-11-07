package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class CreateGroupPostMenuViewModel(private val repository: Repository) : ViewModel() {

    lateinit var groupId: String
    var textContent: String? = null
    var image: Bitmap? = null

    fun createPost(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val status = repository.createPost(groupId, textContent, image)
            callback(status)
        }
    }
}