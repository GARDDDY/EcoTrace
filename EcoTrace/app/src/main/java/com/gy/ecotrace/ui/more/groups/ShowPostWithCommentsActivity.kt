package com.gy.ecotrace.ui.more.groups

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.dto.GroupComment
import com.gy.ecotrace.network.dto.GroupPost
import com.gy.ecotrace.ui.more.groups.viewModels.ShowPostWithCommentsViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ShowPostWithCommentsActivity : AppCompatActivity() {

    private val showPostViewModel: ShowPostWithCommentsViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private val currentUser: Long get() = tokenStore.userId
    private val commentViews = HashMap<Long, View>()

    private lateinit var attachImage: ImageButton
    private lateinit var sendComment: ImageButton

    private fun groupRoleRank(role: String?): Int = when (role) {
        "creator" -> 3; "manager" -> 2; "assistant" -> 1; else -> 0
    }

    private fun imageUrl(bucket: String, key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/$bucket/$it" }

    private fun formatTime(iso: String?): String = try {
        val z = Instant.parse(iso).atZone(ZoneId.systemDefault())
        z.format(
            DateTimeFormatter.ofPattern(
                "dd MMMM${if (z.year != LocalDate.now().year) " yyyy" else ""}, HH:mm",
                Locale.getDefault()
            )
        )
    } catch (e: Exception) {
        ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_post_with_comments)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val post = Gson().fromJson(intent.getStringExtra("post"), GroupPost::class.java)
        val groupId = Globals.getInstance().getString("CurrentlyWatchingGroup").toLongOrNull() ?: 0

        showPostViewModel.postId = post.id
        showPostViewModel.groupId = groupId
        showPostViewModel.loadRole()

        findViewById<TextView>(R.id.postCreatorName).text = post.authorUsername
        findViewById<TextView>(R.id.postCreateTime).text = formatTime(post.createdAt)
        Glide.with(this).load(R.drawable.baseline_person_24).circleCrop()
            .into(findViewById(R.id.postCreatorImage))

        if (post.text != null) {
            findViewById<TextView>(R.id.postContentText).apply {
                text = post.text
                visibility = View.VISIBLE
            }
        }
        if (post.imageKey != null) {
            findViewById<ImageView>(R.id.postContentImage).apply {
                Glide.with(this@ShowPostWithCommentsActivity)
                    .load(imageUrl("posts", post.imageKey))
                    .placeholder(R.drawable.layout_loading_shimmer)
                    .into(this)
                visibility = View.VISIBLE
            }
        }

        val canModeratePost = post.authorId == currentUser ||
            (groupRoleRank(showPostViewModel.myRole) >= 1 &&
                groupRoleRank(showPostViewModel.myRole) > groupRoleRank(post.authorRole))
        if (canModeratePost) {
            val postToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
            postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
            postToolbar.setOnMenuItemClickListener {
                if (it.itemId == R.id.deletePost) {
                    showPostViewModel.deletePost { ok ->
                        if (ok) finish()
                        else Toast.makeText(this, "Не удалось удалить пост!", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
        }

        showPostViewModel.loadCurrentUserAvatar()
        showPostViewModel.currentUserAvatarKey.observe(this) { key ->
            Glide.with(this).load(imageUrl("users", key)).circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(findViewById(R.id.currentUserImage))
        }

        val commentsLayout: LinearLayout = findViewById(R.id.commentsLayout)
        showPostViewModel.loadComments()
        showPostViewModel.observeComments()

        showPostViewModel.comments.observe(this) { page ->
            findViewById<ShimmerFrameLayout>(R.id.loadingComments).visibility = View.GONE
            page.forEach { renderComment(commentsLayout, it, atTop = false) }
        }
        showPostViewModel.newComment.observe(this) { comment ->
            if (comment.postId == post.id) renderComment(commentsLayout, comment, atTop = false)
        }
        showPostViewModel.deletedComment.observe(this) { id ->
            commentViews.remove(id)?.let { (it.parent as? LinearLayout)?.removeView(it) }
        }

        val commentText = findViewById<EditText>(R.id.postCreateCommentEntry)
        sendComment = findViewById(R.id.postSendComment)
        commentText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                showPostViewModel.textComment = p0?.toString()
                sendComment.visibility = if (p0.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        sendComment.setOnClickListener {
            showPostViewModel.sendComment { ok ->
                if (ok) {
                    commentText.setText("")
                    showPostViewModel.imageComment = null
                    attachImage.imageTintList =
                        ContextCompat.getColorStateList(applicationContext, R.color.silver)
                    Toast.makeText(applicationContext, "Комментарий опубликован", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Произошла ошибка!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        attachImage = findViewById(R.id.postAttachImage)
        attachImage.setOnClickListener {
            if (showPostViewModel.imageComment == null) showImageSourceDialog() else showPopup()
        }
    }

    private fun renderComment(container: LinearLayout, comment: GroupComment, atTop: Boolean) {
        if (commentViews.containsKey(comment.id)) return
        val commentLayout = layoutInflater.inflate(R.layout.layout_user_comment_in_user_post_in_group, null)

        var hasText = false
        if (!comment.text.isNullOrEmpty()) {
            commentLayout.findViewById<TextView>(R.id.postContentText).apply {
                text = comment.text
                visibility = View.VISIBLE
            }
            hasText = true
        }
        var hasImage = false
        if (comment.imageKey != null) {
            commentLayout.findViewById<ImageView>(R.id.postContentImage).apply {
                Glide.with(this@ShowPostWithCommentsActivity).load(imageUrl("comments", comment.imageKey)).into(this)
                visibility = View.VISIBLE
            }
            hasImage = true
        }
        if (!hasImage && !hasText) return

        val canModerate = comment.authorId == currentUser ||
            (groupRoleRank(showPostViewModel.myRole) >= 1 &&
                groupRoleRank(showPostViewModel.myRole) > groupRoleRank(comment.authorRole))
        if (canModerate) {
            val postToolbar = commentLayout.findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
            postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
            postToolbar.setOnMenuItemClickListener {
                if (it.itemId == R.id.deletePost) {
                    showPostViewModel.deleteComment(comment.id) { ok ->
                        if (ok) commentViews.remove(comment.id)?.let { v -> (v.parent as? LinearLayout)?.removeView(v) }
                        else Toast.makeText(this, "Не удалось удалить комментарий!", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
        }

        commentLayout.findViewById<LinearLayout>(R.id.openCreatorProfileLayout).setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatching", comment.authorId.toString())
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        commentLayout.findViewById<TextView>(R.id.postCreatorName).text = comment.authorUsername
        Glide.with(this).load(R.drawable.baseline_person_24).circleCrop()
            .into(commentLayout.findViewById(R.id.postCreatorImage))
        commentLayout.findViewById<TextView>(R.id.postCreateTime).text = formatTime(comment.createdAt)

        commentViews[comment.id] = commentLayout
        if (atTop) container.addView(commentLayout, 0) else container.addView(commentLayout)
    }

    private fun showImageSourceDialog() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        val chooserIntent = Intent.createChooser(galleryIntent, "Добавьте изображение, используя")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        startActivityForResult(chooserIntent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            data?.let {
                val extras = it.extras
                val bitmap = if (extras != null && extras.containsKey("data")) {
                    extras.get("data") as Bitmap
                } else {
                    BitmapFactory.decodeStream(it.data?.let { uri -> contentResolver.openInputStream(uri) })
                }
                addImageToLayout(bitmap)
            }
        }
    }

    private fun addImageToLayout(imageBitmap: Bitmap) {
        showPostViewModel.imageComment = imageBitmap
        attachImage.imageTintList = ContextCompat.getColorStateList(applicationContext, R.color.ok_green)
        sendComment.visibility = View.VISIBLE
    }

    private fun showPopup() {
        val popupView = layoutInflater.inflate(R.layout.layout_attached_image_comment, null)
        val popupWindow = PopupWindow(
            popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )
        popupWindow.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
        popupWindow.isOutsideTouchable = true
        val imageAttached = popupView.findViewById<ImageView>(R.id.attachedImage)
        imageAttached.setImageBitmap(showPostViewModel.imageComment)

        popupView.findViewById<Button>(R.id.removeImage).setOnClickListener {
            imageAttached.setImageDrawable(null)
            showPostViewModel.imageComment = null
            attachImage.imageTintList = ContextCompat.getColorStateList(applicationContext, R.color.silver)
            popupWindow.dismiss()
        }
        popupView.findViewById<Button>(R.id.reattachImage).setOnClickListener {
            showImageSourceDialog()
            sendComment.visibility = if (showPostViewModel.textComment == null) View.GONE else View.VISIBLE
            popupWindow.dismiss()
        }
        popupWindow.showAtLocation(findViewById(R.id.main), Gravity.CENTER, 0, 0)
    }
}
