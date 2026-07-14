package com.gy.ecotrace.ui.more.groups

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private var isPinned = false
    private var toolbarMenuInflated = false
    private lateinit var currentPost: GroupPost

    private lateinit var attachImage: ImageButton
    private lateinit var sendComment: ImageButton
    private lateinit var commentImagesScroll: View
    private lateinit var commentImagesContainer: LinearLayout

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

        bindPost(post)

        showPostViewModel.loadRole {
            setupPostToolbar()

            val commentsLayout: LinearLayout = findViewById(R.id.commentsLayout)
            showPostViewModel.loadComments()
            showPostViewModel.observeComments()

            showPostViewModel.comments.observe(this) { page ->
                findViewById<ShimmerFrameLayout>(R.id.loadingComments).visibility = View.GONE
                page.forEach { renderComment(commentsLayout, it, atTop = false) }
            }
            showPostViewModel.newComment.observe(this) { comment ->
                if (comment.postId == currentPost.id) renderComment(commentsLayout, comment, atTop = false)
            }
            showPostViewModel.deletedComment.observe(this) { id ->
                commentViews.remove(id)?.let { (it.parent as? LinearLayout)?.removeView(it) }
            }

            setupCommentsPagination()
        }

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.postSwipeRefresh)
        swipeRefresh.setOnRefreshListener {
            showPostViewModel.refreshPost { refreshed -> refreshed?.let { bindPost(it) } }

            val commentsLayout: LinearLayout = findViewById(R.id.commentsLayout)
            commentsLayout.removeAllViews()
            commentViews.clear()
            showPostViewModel.resetComments()
            showPostViewModel.loadComments()
            swipeRefresh.isRefreshing = false
        }

        showPostViewModel.loadCurrentUserAvatar()
        showPostViewModel.currentUserAvatarKey.observe(this) { key ->
            Glide.with(this).load(imageUrl("users", key)).circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(findViewById(R.id.currentUserImage))
        }

        val commentText = findViewById<EditText>(R.id.postCreateCommentEntry)
        sendComment = findViewById(R.id.postSendComment)
        commentText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                showPostViewModel.textComment = p0?.toString()
                sendComment.visibility = if (p0.isNullOrEmpty() && showPostViewModel.imagesComment.isEmpty()) View.GONE else View.VISIBLE
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        sendComment.setOnClickListener {
            showPostViewModel.sendComment { ok ->
                if (ok) {
                    commentText.setText("")
                    clearAttachedImages()
                    Toast.makeText(applicationContext, "Комментарий опубликован", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Произошла ошибка!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        attachImage = findViewById(R.id.postAttachImage)
        attachImage.setOnClickListener { showImageSourceDialog() }

        commentImagesScroll = findViewById(R.id.commentImagesScroll)
        commentImagesContainer = findViewById(R.id.commentImagesContainer)
    }

    private fun bindPost(post: GroupPost) {
        currentPost = post
        isPinned = post.pinnedAt != null

        findViewById<TextView>(R.id.postCreatorName).text = post.authorUsername
        findViewById<TextView>(R.id.postCreateTime).text = formatTime(post.createdAt)
        Glide.with(this).load(imageUrl("users", post.authorAvatarKey)).circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(findViewById(R.id.postCreatorImage))

        val textView = findViewById<TextView>(R.id.postContentText)
        if (!post.text.isNullOrEmpty()) {
            textView.text = post.text
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }

        if (post.imageKeys.isNotEmpty()) {
            renderImageStrip(
                this,
                findViewById(R.id.postImagesScroll),
                findViewById(R.id.postImagesContainer),
                post.imageKeys.map { imageUrl("posts", it)!! }
            )
        } else {
            findViewById<HorizontalScrollView>(R.id.postImagesScroll).visibility = View.GONE
        }

        if (toolbarMenuInflated) updatePostToolbarState()
    }

    private fun setupPostToolbar() {
        val postToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
        postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
        toolbarMenuInflated = true
        postToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.deletePost -> {
                    showPostViewModel.deletePost { ok ->
                        if (ok) finish()
                        else Toast.makeText(this, "Не удалось удалить пост!", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.pinPost -> {
                    if (isPinned) {
                        showPostViewModel.unpinPost { ok ->
                            if (ok) { isPinned = false; updatePostToolbarState() }
                            else Toast.makeText(this, "Не удалось открепить пост!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showPostViewModel.pinPost { ok ->
                            if (ok) { isPinned = true; updatePostToolbarState() }
                            else Toast.makeText(this, "Не удалось закрепить пост!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            true
        }
        updatePostToolbarState()
    }

    private fun updatePostToolbarState() {
        val postToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
        val canDelete = currentPost.authorId == currentUser ||
            (groupRoleRank(showPostViewModel.myRole) >= 1 &&
                groupRoleRank(showPostViewModel.myRole) > groupRoleRank(currentPost.authorRole))
        val canPin = groupRoleRank(showPostViewModel.myRole) >= 1
        postToolbar.menu.findItem(R.id.deletePost).isVisible = canDelete
        postToolbar.menu.findItem(R.id.pinPost).apply {
            isVisible = canPin
            title = if (isPinned) "Открепить" else "Закрепить"
        }
    }

    private fun setupCommentsPagination() {
        val scrollView: ScrollView = findViewById(R.id.scrollViewCommentsMain)
        scrollView.setOnScrollChangeListener { view, _, _, _, _ ->
            val bottomGap = scrollView.getChildAt(scrollView.childCount - 1).bottom - (scrollView.height + view.scrollY)
            if (bottomGap <= 64 && !showPostViewModel.foundAll && !showPostViewModel.fetching) {
                showPostViewModel.loadComments()
            }
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
        val hasImage = comment.imageKeys.isNotEmpty()
        if (hasImage) {
            renderImageStrip(
                this,
                commentLayout.findViewById(R.id.postImagesScroll),
                commentLayout.findViewById(R.id.postImagesContainer),
                comment.imageKeys.map { imageUrl("comments", it)!! },
                maxSingleWidthPx = 220.toPx()
            )
        }
        if (!hasImage && !hasText) return

        val canModerate = comment.authorId == currentUser ||
            (groupRoleRank(showPostViewModel.myRole) >= 1 &&
                groupRoleRank(showPostViewModel.myRole) > groupRoleRank(comment.authorRole))
        if (canModerate) {
            val postToolbar = commentLayout.findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
            postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
            postToolbar.menu.findItem(R.id.pinPost).isVisible = false
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
        Glide.with(this).load(imageUrl("users", comment.authorAvatarKey)).circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(commentLayout.findViewById(R.id.postCreatorImage))
        commentLayout.findViewById<TextView>(R.id.postCreateTime).text = formatTime(comment.createdAt)

        commentViews[comment.id] = commentLayout
        if (atTop) container.addView(commentLayout, 0) else container.addView(commentLayout)
    }

    private fun showImageSourceDialog() {
        startActivityForResult(buildImagePickerChooser(this), REQUEST_PICK_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_IMAGES || resultCode != Activity.RESULT_OK) return

        val picked = extractPickedBitmaps(this, data)
        val room = MAX_ATTACHED_IMAGES - showPostViewModel.imagesComment.size
        if (room <= 0) {
            Toast.makeText(this, getString(R.string.tooManyImages, MAX_ATTACHED_IMAGES), Toast.LENGTH_SHORT).show()
            return
        }
        showPostViewModel.imagesComment.addAll(picked.take(room))
        renderAttachedImages()
        sendComment.visibility = View.VISIBLE
        attachImage.imageTintList = androidx.core.content.ContextCompat.getColorStateList(applicationContext, R.color.ok_green)
    }

    private fun renderAttachedImages() {
        renderEditableThumbnails(this, commentImagesScroll, commentImagesContainer, showPostViewModel.imagesComment) { index ->
            showPostViewModel.imagesComment.removeAt(index)
            renderAttachedImages()
            if (showPostViewModel.imagesComment.isEmpty()) clearAttachedImages()
        }
    }

    private fun clearAttachedImages() {
        showPostViewModel.imagesComment.clear()
        commentImagesScroll.visibility = View.GONE
        attachImage.imageTintList = androidx.core.content.ContextCompat.getColorStateList(applicationContext, R.color.silver)
        sendComment.visibility = if (showPostViewModel.textComment.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    companion object {
        private const val REQUEST_PICK_IMAGES = 100
    }
}
