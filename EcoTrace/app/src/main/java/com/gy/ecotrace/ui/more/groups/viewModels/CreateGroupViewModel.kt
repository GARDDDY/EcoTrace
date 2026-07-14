package com.gy.ecotrace.ui.more.groups.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.CreateGroupRequest
import com.gy.ecotrace.repos.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val repository: GroupRepository
) : ViewModel() {

    var editGroupId: Long? = null
        private set

    var groupImage: Bitmap? = null
    var groupImageKey: String? = null
    var groupImageGenerated: Boolean = false
    var removeImageRequested: Boolean = false
    var groupName: String? = null
    var groupAbout: String? = null
    // 0 - открытая, 1 - закрытая
    var groupType: Int = 0
    var groupRulesText: String? = null

    fun loadForEdit(groupId: Long, onLoaded: (Boolean) -> Unit) {
        editGroupId = groupId
        viewModelScope.launch {
            val group = runCatching { repository.refreshGroup(groupId) }.getOrNull()
                ?: repository.cachedGroup(groupId)
            if (group == null) {
                onLoaded(false)
                return@launch
            }
            groupName = group.name
            groupAbout = group.about
            groupType = if (group.type == "closed") 1 else 0
            groupRulesText = group.rules
            groupImageKey = group.rulesImageKey
            groupImageGenerated = group.imageGenerated
            onLoaded(true)
        }
    }

    // Ничего не отправляет на сервер — фактическое удаление фото происходит
    // только при сохранении (submitGroup), как и остальные изменения формы.
    fun requestRemoveImage() {
        removeImageRequested = true
        groupImage = null
    }

    fun isGroupNameAvailable(name: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { repository.checkName(name, excludeId = editGroupId) }.getOrNull()
            callback(
                when {
                    result == null -> "Нет соединения с сервером"
                    result.available -> null
                    else -> result.reason ?: "Имя уже занято"
                }
            )
        }
    }

    fun submitGroup(callback: (String?) -> Unit) {
        viewModelScope.launch {
            val request = CreateGroupRequest(
                name = groupName ?: "",
                type = if (groupType == 1) "closed" else "open",
                about = groupAbout?.takeIf { it.isNotBlank() },
                rules = groupRulesText?.takeIf { it.isNotBlank() }
            )
            val editId = editGroupId
            val result = if (editId != null) {
                runCatching { repository.update(editId, request, groupImage?.toPng()) }
            } else {
                runCatching { repository.create(request, groupImage?.toPng()) }
            }
            if (result.isSuccess && editId != null && removeImageRequested && groupImage == null) {
                val removed = runCatching { repository.removeGroupImage(editId) }
                removed.getOrNull()?.let {
                    groupImageKey = it.rulesImageKey
                    groupImageGenerated = it.imageGenerated
                }
                removeImageRequested = false
                if (removed.isFailure) {
                    callback(removed.exceptionOrNull()?.message ?: "Не удалось убрать фото")
                    return@launch
                }
            }
            callback(
                if (result.isSuccess) null
                else result.exceptionOrNull()?.message ?: "Не удалось сохранить группу"
            )
        }
    }

    private fun Bitmap.toPng(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
