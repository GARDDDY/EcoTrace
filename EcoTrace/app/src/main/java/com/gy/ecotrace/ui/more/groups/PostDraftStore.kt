package com.gy.ecotrace.ui.more.groups

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream

object PostDraftStore {
    private const val PREFS = "post_drafts"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun draftDir(context: Context, groupId: Long) = File(context.cacheDir, "post_draft_$groupId")

    fun save(context: Context, groupId: Long, text: String?, images: List<Bitmap>) {
        val dir = draftDir(context, groupId)
        dir.deleteRecursively()
        if (text.isNullOrBlank() && images.isEmpty()) {
            clear(context, groupId)
            return
        }
        dir.mkdirs()
        images.forEachIndexed { i, bitmap ->
            runCatching {
                FileOutputStream(File(dir, "$i.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
            }
        }
        prefs(context).edit {
            putString("text_$groupId", text)
            putInt("imageCount_$groupId", images.size)
        }
    }

    fun load(context: Context, groupId: Long): Pair<String?, List<Bitmap>> {
        val text = prefs(context).getString("text_$groupId", null)
        val count = prefs(context).getInt("imageCount_$groupId", 0)
        val dir = draftDir(context, groupId)
        val images = (0 until count).mapNotNull { i ->
            runCatching { BitmapFactory.decodeFile(File(dir, "$i.png").absolutePath) }.getOrNull()
        }
        return text to images
    }

    fun hasDraft(context: Context, groupId: Long): Boolean {
        val (text, images) = load(context, groupId)
        return !text.isNullOrBlank() || images.isNotEmpty()
    }

    fun clear(context: Context, groupId: Long) {
        draftDir(context, groupId).deleteRecursively()
        prefs(context).edit {
            remove("text_$groupId")
            remove("imageCount_$groupId")
        }
    }
}
