package com.gy.ecotrace.ui.more.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.ShowGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ShowPostWithCommentsActivity : AppCompatActivity() {
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
        val showPostViewModel = ViewModelProvider(this, ShowGroupViewModelFactory.getInstance(repository))[ShowGroupViewModel::class.java]

        val currentPost = Globals.getInstance().getString("CurrentlyWatchingPost")
        val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")
        val currentUser = Globals.getInstance().getString("CurrentlyLogged")
        showPostViewModel.getPostComments(currentGroup, currentPost)
//        Log.d("testobserve", showPostViewModel.lastId.toString())
        val mainCommentScrollView = findViewById<ScrollView>(R.id.scrollViewCommentsMain)
        showPostViewModel.allFoundPosts.observe(this, Observer {
            mainCommentScrollView.foreground = ColorDrawable(ContextCompat.getColor(applicationContext, R.color.transparent))
            Log.d("Observing", "obs")
            val post = it[currentPost]!!
            var hasText = false
            val postText = findViewById<TextView>(R.id.postContentText)
            if (post.postContentText != null) {
                postText.text =
                    post.postContentText
                hasText = true
            } else {
                postText.visibility = View.GONE
            }
            var hasImage = false
            val postImage: ImageView = findViewById(R.id.postContentImage)
            if (post.postContentImageURI != null) {
                Glide.with(this)
                    .load(Globals().getImgUrl("posts", post.postContentImageURI!!))
                    .into(postImage)
                hasImage = true
            } else {
                postImage.visibility = View.GONE
            }

            if (!hasImage && !hasText) {
                return@Observer
            }

            if (currentUser == "0") {
                findViewById<LinearLayout>(R.id.bottomInfoPost).visibility =
                    View.GONE
            } else {
                Glide.with(this)
                    .load(Globals().getImgUrl("users", currentUser))
                    .circleCrop()
                    .into(findViewById(R.id.currentUserImage))


                findViewById<LinearLayout>(R.id.copyThisPostText)
                    .setOnClickListener {
                        val clipboard: ClipboardManager =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Текст скопирован", post.postContentText)
                        clipboard.setPrimaryClip(clip)
                    }
                findViewById<LinearLayout>(R.id.likeThisPost).setOnClickListener {

                }

                val commentView: EditText = findViewById(R.id.postCreateCommentEntry)
                val postComment: ImageButton = findViewById(R.id.postSendComment)
                commentView.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        postComment.visibility =
                            when (s.isNullOrEmpty()) {
                                true -> View.GONE
                                else -> View.VISIBLE
                            }
                    }

                    override fun beforeTextChanged(
                        p0: CharSequence?,
                        p1: Int,
                        p2: Int,
                        p3: Int
                    ) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                })
                postComment.setOnClickListener {
                    showPostViewModel.sendComment(currentGroup, currentPost, currentUser, commentView.text.toString(), null)
                }
            }

            findViewById<LinearLayout>(R.id.openCreatorProfileLayout).setOnClickListener {
                Globals.getInstance().setString("CurrentlyWatching", post.postCreatorId)
                startActivity(
                    Intent(
                        this,
                        ProfileActivity::class.java
                    )
                )
            }
            findViewById<TextView>(R.id.postCreatorName).text = post.postCreatorName
            Glide.with(this)
                .load(Globals().getImgUrl("users", post.postCreatorId))
                .circleCrop()
                .into(findViewById(R.id.postCreatorImage))

            val sentTime = ZonedDateTime.parse(
                post.postId,
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC"))
            )
            val localTime = sentTime.withZoneSameInstant(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("dd MMMM, HH:mm", Locale.getDefault())
            val formattedDateTime = localTime.format(formatter)
            findViewById<TextView>(R.id.postCreateTime).text = formattedDateTime

        })

        val commentsLayout: LinearLayout = mainCommentScrollView.children.first() as LinearLayout
        showPostViewModel.allFoundComments.observe(this, Observer{
            commentsLayout.removeAllViews()
            for (post in it) {
                val commentLayout = layoutInflater.inflate(R.layout.layout_user_comment_in_user_post_in_group, null)
                var hasText = false
                val postText = commentLayout.findViewById<TextView>(R.id.postContentText)
                if (post.commentContentText != null) {
                    postText.text =
                        post.commentContentText
                    hasText = true
                } else {
                    postText.visibility = View.GONE
                }
                var hasImage = false
                val postImage: ImageView = commentLayout.findViewById(R.id.postContentImage)
                if (post.commentContentImageURI != null) {
                    Glide.with(this)
                        .load(Globals().getImgUrl("posts", post.commentContentImageURI!!))
                        .into(postImage)
                    hasImage = true
                } else {
                    postImage.visibility = View.GONE
                }

                if (!hasImage && !hasText) {
                    Log.d("empty", post.toString())
                    return@Observer
                }

                    commentLayout.findViewById<LinearLayout>(R.id.copyThisPostText)
                        .setOnClickListener {
                            val clipboard: ClipboardManager =
                                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Текст скопирован", post.commentContentText)
                            clipboard.setPrimaryClip(clip)
                        }
                    commentLayout.findViewById<LinearLayout>(R.id.likeThisPost).setOnClickListener {

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

                val sentTime = ZonedDateTime.parse(
                    post.commentId,
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC"))
                )
                val localTime = sentTime.withZoneSameInstant(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("dd MMMM, HH:mm", Locale.getDefault())
                val formattedDateTime = localTime.format(formatter)
                commentLayout.findViewById<TextView>(R.id.postCreateTime).text = formattedDateTime

                commentsLayout.addView(commentLayout)
            }
        })

        val commentTextEdit: EditText = findViewById(R.id.postCreateCommentEntry)
        val sendComment: ImageButton = findViewById(R.id.postSendComment)
        var imageAttached = false
        commentTextEdit.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    sendComment.visibility = View.VISIBLE
                } else {
                    if (!imageAttached) sendComment.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        sendComment.setOnClickListener {
            showPostViewModel.sendComment(currentGroup, currentPost, currentUser, commentTextEdit.text.toString(), null)
            commentTextEdit.text.clear()
            showPostViewModel.getPostComments(currentGroup, currentPost, true)
        }
    }
}