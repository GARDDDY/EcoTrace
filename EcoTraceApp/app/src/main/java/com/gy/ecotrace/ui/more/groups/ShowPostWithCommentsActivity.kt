package com.gy.ecotrace.ui.more.groups

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.ShowGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.additional.ShowPostWithCommentsViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel
import com.gy.ecotrace.ui.more.groups.viewModels.ShowPostWithCommentsViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import org.w3c.dom.Text
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ShowPostWithCommentsActivity : AppCompatActivity() {

    private val currentUser = ETAuth.getInstance().guid()
    private var userRole: Int = 4

    private lateinit var showPostViewModel: ShowPostWithCommentsViewModel

    private lateinit var attachImage: ImageButton
    private lateinit var sendComment: ImageButton

    private fun tsToTime(timestamp: Long): String {
        val currentYear = LocalDate.now().year
        val sentTime = Instant.ofEpochMilli(timestamp*1000)
            .atZone(ZoneId.of("UTC+3"))
        val localTime = sentTime.withZoneSameInstant(ZoneId.systemDefault())

        val formatter = DateTimeFormatter.ofPattern("dd MMMM${if (currentYear != sentTime.year) " yyyy" else ""}, HH:mm", Locale.getDefault())
        return localTime.format(formatter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_post_with_comments)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            Globals.getInstance().setString("CurrentlyWatchingPost", "0")
            onBackPressed()
        }


        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        showPostViewModel = ViewModelProvider(this, ShowPostWithCommentsViewModelFactory(repository))[ShowPostWithCommentsViewModel::class.java]

        showPostViewModel.groupId = Globals.getInstance().getString("CurrentlyWatchingGroup")

        val postData = Gson().fromJson(intent.getStringExtra("data"), DatabaseMethods.DataClasses.Post::class.java)
        showPostViewModel.postId = postData.postId

        findViewById<TextView>(R.id.postCreatorName).text = postData.postCreatorName
        findViewById<TextView>(R.id.postCreateTime).text = tsToTime(postData.postTime)
        Glide.with(this@ShowPostWithCommentsActivity)
            .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", postData.postCreatorId))
            .circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(findViewById(R.id.postCreatorImage))

        if (postData.postContentText != null) {
            val textContent = findViewById<TextView>(R.id.postContentText)
            textContent.text = postData.postContentText
            textContent.visibility = View.VISIBLE
        }

        if (postData.postContentImage != null) {
            val imgContent = findViewById<ImageView>(R.id.postContentImage)
            Glide.with(this@ShowPostWithCommentsActivity)
                .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("posts", postData.postContentImage!!))
                .placeholder(R.drawable.layout_loading_shimmer)
                .into((imgContent))
            imgContent.visibility = View.VISIBLE
        }
        showPostViewModel.getRole { userRole ->
            this.userRole = userRole
            if (postData.postCreatorId == currentUser
                || (userRole <= 2 && userRole < postData.postCreatorRole)
            ) {
                val postToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
                postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
                postToolbar.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.deletePost -> {
                            showPostViewModel.deletePost {
                                if (it) {
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@ShowPostWithCommentsActivity,
                                        "Не удалось удалить пост!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    true
                }
            }
        }


        Glide.with(this@ShowPostWithCommentsActivity)
            .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", currentUser))
            .circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(findViewById(R.id.currentUserImage))


        showPostViewModel.getComments()


        val commentsLayout: LinearLayout = findViewById(R.id.commentsLayout)
        showPostViewModel.comments.observe(this, Observer{
            findViewById<ShimmerFrameLayout>(R.id.loadingComments).visibility = View.GONE

            for (post in it) {
                val commentLayout = layoutInflater.inflate(R.layout.layout_user_comment_in_user_post_in_group, null)
                var hasText = false
                val postText = commentLayout.findViewById<TextView>(R.id.postContentText)
                if (post.commentContentText != null) {
                    postText.text =
                        post.commentContentText
                    postText.visibility = View.VISIBLE
                    hasText = true
                }
                var hasImage = false
                val postImage: ImageView = commentLayout.findViewById(R.id.postContentImage)
                if (post.commentContentImage != null) {
                    Glide.with(this@ShowPostWithCommentsActivity)
                        .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("posts", post.commentContentImage!!))
                        .listener(object : RequestListener<Drawable>{ // important!!!
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                postImage.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                postImage.visibility = View.VISIBLE
                                return false
                            }
                        })
                        .into((postImage))
                    hasImage = true
                }

                if (!hasImage && !hasText) {
                    Log.d("empty", post.toString())
                    return@Observer
                }

                Log.d("comm del", "can del: ${post.commentCreatorId == currentUser
                        || (userRole <= 2 && userRole < post.commentCreatorRole)}")
                if (post.commentCreatorId == currentUser
                        || (userRole <= 2 && userRole < post.commentCreatorRole)
                ) {
                    val postToolbar = commentLayout.findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
                    postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
                    postToolbar.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.deletePost -> {
                                showPostViewModel.deleteComment(post.commentId) {
                                    if (it) {
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this@ShowPostWithCommentsActivity,
                                            "Не удалось удалить комментарий!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        true
                    }
                }

                commentLayout.findViewById<LinearLayout>(R.id.openCreatorProfileLayout).setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", post.commentCreatorId)
                    startActivity(
                        Intent(
                            this,
                            ProfileActivity::class.java
                        )
                    )
                }
                commentLayout.findViewById<TextView>(R.id.postCreatorName).text = post.commentCreatorName
                Glide.with(this)
                    .load(Globals().getImgUrl("users", post.commentCreatorId))
                    .circleCrop()
                    .into(commentLayout.findViewById(R.id.postCreatorImage))


                commentLayout.findViewById<TextView>(R.id.postCreateTime).text = tsToTime(post.commentTime)

                commentsLayout.addView(commentLayout)
            }

        })

        val commentText = findViewById<EditText>(R.id.postCreateCommentEntry)
        sendComment = findViewById(R.id.postSendComment)
        commentText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {

                showPostViewModel.textComment = p0?.toString()

                if (!p0.isNullOrEmpty()) {
                    sendComment.visibility = View.VISIBLE
                } else {
                    sendComment.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        attachImage = findViewById(R.id.postAttachImage)

        attachImage.setOnClickListener {
            if (showPostViewModel.imageComment == null) {
                showImageSourceDialog()
            } else {
                showPopup()
            }
        }
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
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                100 -> {
                    data?.let {
                        val extras = it.extras
                        if (extras != null && extras.containsKey("data")) {
                            val imageBitmap = extras.get("data") as Bitmap
                            addImageToLayout(imageBitmap)

                        } else {
                            val selectedImageUri: Uri? = it.data
                            val inputStream = selectedImageUri?.let { it1 ->
                                contentResolver.openInputStream(
                                    it1
                                )
                            }
                            addImageToLayout(BitmapFactory.decodeStream(inputStream))
                        }
                    }
                }
            }
        }
    }

    private fun addImageToLayout(imageBitmap: Bitmap) {
        showPostViewModel.imageComment = imageBitmap
        attachImage.imageTintList = ContextCompat.getColorStateList(applicationContext, R.color.ok_green)
        sendComment.visibility = View.VISIBLE
        showPopup()
    }

    private fun showPopup() {
        val popupView = layoutInflater.inflate(R.layout.layout_attached_image_comment, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
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

        popupWindow.showAtLocation(findViewById(R.id.main), Gravity.CENTER, 0,0)
    }
}