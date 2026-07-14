package com.gy.ecotrace.ui.more.groups

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.gy.ecotrace.R

const val MAX_ATTACHED_IMAGES = 6

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun renderImageStrip(
    context: Context,
    scrollHost: HorizontalScrollView,
    container: LinearLayout,
    urls: List<String>,
    maxSingleWidthPx: Int = Resources.getSystem().displayMetrics.widthPixels - 32.toPx()
) {
    container.removeAllViews()
    if (urls.isEmpty()) {
        scrollHost.visibility = View.GONE
        return
    }
    scrollHost.visibility = View.VISIBLE
    val single = urls.size == 1
    urls.forEach { url ->
        val iv = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                if (single) maxSingleWidthPx else 220.toPx(),
                if (single) LinearLayout.LayoutParams.WRAP_CONTENT else 220.toPx()
            ).apply { marginEnd = 6.toPx() }
            scaleType = if (single) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = single
            setBackgroundColor(context.getColor(R.color.dirt2_white))
        }
        Glide.with(context).load(url).into(iv)
        container.addView(iv)
    }
}

fun renderEditableThumbnails(
    context: Context,
    scrollHost: View,
    container: LinearLayout,
    images: List<Bitmap>,
    onRemoveAt: (Int) -> Unit
) {
    container.removeAllViews()
    scrollHost.visibility = if (images.isEmpty()) View.GONE else View.VISIBLE
    images.forEachIndexed { index, bitmap ->
        val thumb = LayoutInflater.from(context).inflate(R.layout.layout_editable_image_thumb, container, false)
        thumb.findViewById<ImageView>(R.id.thumbImage).setImageBitmap(bitmap)
        thumb.findViewById<ImageButton>(R.id.thumbRemove).setOnClickListener { onRemoveAt(index) }
        container.addView(thumb)
    }
}

fun buildImagePickerChooser(context: Context): Intent {
    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    val chooserIntent = Intent.createChooser(galleryIntent, "Добавьте изображение, используя")
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
    return chooserIntent
}

fun extractPickedBitmaps(activity: Activity, data: Intent?): List<Bitmap> {
    if (data == null) return emptyList()
    val extras = data.extras
    if (extras != null && extras.containsKey("data")) {
        return listOfNotNull(extras.get("data") as? Bitmap)
    }
    val clipData = data.clipData
    if (clipData != null) {
        return (0 until clipData.itemCount).mapNotNull { i ->
            runCatching {
                val uri = clipData.getItemAt(i).uri
                BitmapFactory.decodeStream(activity.contentResolver.openInputStream(uri))
            }.getOrNull()
        }
    }
    val uri = data.data ?: return emptyList()
    return listOfNotNull(
        runCatching { BitmapFactory.decodeStream(activity.contentResolver.openInputStream(uri)) }.getOrNull()
    )
}
