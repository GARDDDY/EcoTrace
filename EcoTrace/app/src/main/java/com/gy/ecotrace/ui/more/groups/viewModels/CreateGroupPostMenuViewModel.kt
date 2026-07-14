package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.repos.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class CreateGroupPostMenuViewModel @Inject constructor(
    private val repository: GroupRepository
) : ViewModel() {

    var groupId: Long = 0
    var textContent: String? = null
    val images: MutableList<Bitmap> = mutableListOf()

    fun createPost(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(runCatching { repository.createPost(groupId, textContent, images.map { it.toPng() }) }.isSuccess)
        }
    }

    private fun Bitmap.toPng(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
