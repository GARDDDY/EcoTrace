package com.gy.ecotrace.ui.more.groups

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.NetworkMonitor
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.dto.GroupPost
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ShowGroupActivity : AppCompatActivity() {

    private val showGroupViewModel: ShowGroupViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private lateinit var toolbar: Toolbar

    private val currentUser: Long get() = tokenStore.userId
    private var currentGroupId: Long = 0
    private var currentGroupCreator: Long = -1
    private var currentGroupName = ""
    private var myRank = 0

    private var postsStarted = false
    private var menuConfigured = false
    private val postViews = HashMap<Long, View>()

    private val readStatePrefs by lazy { getSharedPreferences("group_read_state", MODE_PRIVATE) }
    private var lastReadIdAtOpen = 0L
    private var newestPostIdSeen = 0L
    private var readDividerHandled = false

    private var pinnedPostId: Long? = null

    private var myAvatarKey: String? = null
    private var createPostAvatarImage: ImageView? = null
    // currentUserImage во всех постах (лента + закреплённый) — привязываются заново,
    // когда currentUserAvatarKey приходит уже после того, как посты отрисованы.
    private val currentUserImageViews = java.util.WeakHashMap<ImageView, Unit>()

    private var toolbarAnimator: ValueAnimator? = null
    private lateinit var backIcon: android.graphics.drawable.Drawable

    private fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun groupRoleRank(role: String?): Int = when (role) {
        "creator" -> 3; "manager" -> 2; "assistant" -> 1; else -> 0
    }

    private fun imageUrl(bucket: String, key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/$bucket/$it" }

    private fun lastReadPostId(groupId: Long): Long = readStatePrefs.getLong("lastReadPost_$groupId", 0L)

    private fun saveLastReadPostId(groupId: Long, postId: Long) {
        if (postId > lastReadPostId(groupId)) {
            readStatePrefs.edit().putLong("lastReadPost_$groupId", postId).apply()
        }
    }

    private fun animateToolbarCollapse(collapsing: Boolean) {
        toolbarAnimator?.cancel()
        val bgFrom = ContextCompat.getColor(this, if (collapsing) R.color.transparent else R.color.dirt_white)
        val bgTo = ContextCompat.getColor(this, if (collapsing) R.color.dirt_white else R.color.transparent)
        val iconFrom = ContextCompat.getColor(this, if (collapsing) R.color.pair2Color2 else R.color.black)
        val iconTo = ContextCompat.getColor(this, if (collapsing) R.color.black else R.color.pair2Color2)
        val argb = ArgbEvaluator()
        toolbarAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener {
                val t = it.animatedValue as Float
                val bgColor = argb.evaluate(t, bgFrom, bgTo) as Int
                window.statusBarColor = bgColor
                toolbar.setBackgroundColor(bgColor)
            }
            start()
        }
    }

    private fun highlightNewPost(view: View) {
        val highlightColor = ContextCompat.getColor(this, R.color.dirt2_white)
        val normalColor = ContextCompat.getColor(this, R.color.dirt_white)
        view.setBackgroundColor(highlightColor)
        ValueAnimator.ofObject(ArgbEvaluator(), highlightColor, normalColor).apply {
            duration = 1500
            startDelay = 400
            addUpdateListener { view.setBackgroundColor(it.animatedValue as Int) }
            start()
        }
    }

    private fun buildReadDividerView(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8.toPx(), 0, 8.toPx())
        }
        val line = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1.toPx())
            setBackgroundColor(ContextCompat.getColor(this@ShowGroupActivity, R.color.silver))
        }
        val label = TextView(this).apply {
            text = "Прочитано ранее ↓"
            setTextColor(ContextCompat.getColor(this@ShowGroupActivity, R.color.silver))
            textSize = 13f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8.toPx(), 0, 0)
        }
        container.addView(line)
        container.addView(label)
        return container
    }

    private fun updatePinMenuTitle(postId: Long, isPinned: Boolean) {
        postViews[postId]?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
            ?.menu?.findItem(R.id.pinPost)?.title = if (isPinned) "Открепить" else "Закрепить"
    }

    private fun renderPinnedCard(post: GroupPost?) {
        val container = findViewById<LinearLayout>(R.id.pinnedPostContainer)
        container.removeAllViews()
        if (post == null) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val header = TextView(this).apply {
            text = "📌 Закреплено"
            setTextColor(ContextCompat.getColor(this@ShowGroupActivity, R.color.ok_green))
            textSize = 14f
            setPadding(0, 0, 0, 8.toPx())
        }
        container.addView(header)
        buildPostView(post)?.let { container.addView(it) }
    }

    private fun reapplyJoinBtn(joinButton: MaterialButton, member: Boolean) {
        if (member) {
            joinButton.text = "В группе"
            joinButton.setTextColor(ContextCompat.getColor(applicationContext, R.color.ok_green))
            joinButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.dirt_white))
            joinButton.setOnClickListener {
                val builder = android.app.AlertDialog.Builder(this)
                builder.setTitle("Выход из группы")
                if (currentUser != currentGroupCreator) {
                    builder.setMessage("Вы действительно хотите покинуть эту группу?")
                    builder.setPositiveButton("Подтвердить") { _, _ ->
                        showGroupViewModel.leave { }
                    }
                    builder.setNegativeButton("Отмена") { d, _ -> d.dismiss() }
                } else {
                    builder.setMessage("Вы не можете покинуть группу — вы её создатель. Сначала удалите группу.")
                    builder.setNeutralButton("Понятно") { d, _ -> d.dismiss() }
                }
                builder.create().show()
            }
        } else {
            joinButton.text = "Присоединиться"
            joinButton.setTextColor(ContextCompat.getColor(applicationContext, R.color.dirt_white))
            joinButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.ok_green))
            joinButton.setOnClickListener { showGroupViewModel.join { } }
        }
    }

    private fun postCreator(available: Boolean) {
        val postLayout = findViewById<RelativeLayout>(R.id.menuCreatePost)
        postLayout.visibility = if (available) View.VISIBLE else View.GONE
        if (!available) return

        fun openComposer(openPickerImmediately: Boolean) {
            val intent = Intent(this, CreateGroupPostMenuActivity::class.java)
            intent.putExtra("groupName", currentGroupName)
            intent.putExtra("openPickerImmediately", openPickerImmediately)
            startActivity(intent)
        }

        postLayout.setOnClickListener { openComposer(openPickerImmediately = false) }
        findViewById<ImageButton>(R.id.attachImageQuick).setOnClickListener {
            openComposer(openPickerImmediately = true)
        }
        createPostAvatarImage = postLayout.findViewById<ImageView>(R.id.currentUserImage).also {
            Glide.with(this).load(imageUrl("users", myAvatarKey)).circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(it)
        }
    }

    private fun commentEnding(comments: Int): String = when {
        comments % 100 in 11..14 -> "ев"
        comments % 10 == 1 -> "й"
        comments % 10 in 2..4 -> "я"
        else -> "ев"
    }

    private fun formatTime(iso: String?): String = try {
        val zoned = Instant.parse(iso).atZone(ZoneId.systemDefault())
        val pattern = "dd MMMM${if (zoned.year != LocalDate.now().year) " yyyy" else ""}, HH:mm"
        zoned.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    } catch (e: Exception) {
        ""
    }

    private fun buildPostView(post: GroupPost): View? {
        val mainLayout = layoutInflater.inflate(R.layout.layout_user_post_in_group, null)

        val canDelete = post.authorId == currentUser ||
            (myRank >= 1 && myRank > groupRoleRank(post.authorRole))
        val canPin = myRank >= 1
        if (canDelete || canPin) {
            val postToolbar = mainLayout.findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
            postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
            postToolbar.menu.findItem(R.id.deletePost).isVisible = canDelete
            postToolbar.menu.findItem(R.id.pinPost).apply {
                isVisible = canPin
                title = if (post.id == pinnedPostId) "Открепить" else "Закрепить"
            }
            postToolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.deletePost -> {
                        showGroupViewModel.deletePost(post.id) { ok ->
                            if (ok) removePostView(post.id)
                            else Toast.makeText(this, "Не удалось удалить пост!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.pinPost -> {
                        if (post.id == pinnedPostId) {
                            showGroupViewModel.unpinPost(post.id) { ok ->
                                if (!ok) Toast.makeText(this, "Не удалось открепить пост!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showGroupViewModel.pinPost(post.id) { ok ->
                                if (!ok) Toast.makeText(this, "Не удалось закрепить пост!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                true
            }
        }

        val loading: ShimmerFrameLayout = mainLayout.findViewById(R.id.postContentLoading)
        val hasImage = post.imageKeys.isNotEmpty()
        if (hasImage) {
            renderImageStrip(
                this,
                mainLayout.findViewById(R.id.postImagesScroll),
                mainLayout.findViewById(R.id.postImagesContainer),
                post.imageKeys.map { imageUrl("posts", it)!! }
            )
            loading.visibility = View.GONE
        }

        val postText = mainLayout.findViewById<TextView>(R.id.postContentText)
        var hasText = false
        if (!post.text.isNullOrEmpty()) {
            if (!hasImage) loading.visibility = View.GONE
            postText.text = post.text
            postText.visibility = View.VISIBLE
            hasText = true
        }
        if (!hasImage && !hasText) return null

        val currentUserImage = mainLayout.findViewById<ImageView>(R.id.currentUserImage)
        currentUserImageViews[currentUserImage] = Unit
        Glide.with(this).load(imageUrl("users", myAvatarKey)).circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(currentUserImage)

        val postComment: ImageButton = mainLayout.findViewById(R.id.postSendComment)
        val commentView: EditText = mainLayout.findViewById(R.id.postCreateCommentEntry)
        commentView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                postComment.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        val commentsCountView = mainLayout.findViewById<TextView>(R.id.commentsCountPost)
        var commentCount = post.commentsCount
        fun renderCommentCount() {
            commentsCountView.text = "$commentCount комментари${commentEnding(commentCount)}"
        }
        renderCommentCount()

        postComment.setOnClickListener {
            showGroupViewModel.sendComment(post.id, commentView.text.toString()) { ok ->
                if (ok) {
                    commentView.setText("")
                    commentCount++
                    renderCommentCount()
                    Toast.makeText(this, "Комментарий отправлен!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Произошла ошибка!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        mainLayout.findViewById<TextView>(R.id.postCreatorName).text = post.authorUsername
        Glide.with(this).load(imageUrl("users", post.authorAvatarKey)).circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(mainLayout.findViewById(R.id.postCreatorImage))
        mainLayout.findViewById<LinearLayout>(R.id.openCreatorProfileLayout).setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatching", post.authorId.toString())
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        mainLayout.findViewById<TextView>(R.id.postCreateTime).text = formatTime(post.createdAt)

        mainLayout.findViewById<LinearLayout>(R.id.showPostComments).setOnClickListener {
            val intent = Intent(this, ShowPostWithCommentsActivity::class.java)
            intent.putExtra("post", com.google.gson.Gson().toJson(post))
            startActivity(intent)
        }
        return mainLayout
    }

    private fun removePostView(postId: Long) {
        postViews.remove(postId)?.let { (it.parent as? LinearLayout)?.removeView(it) }
    }

    private fun renderPost(post: GroupPost, atTop: Boolean) {
        if (postViews.containsKey(post.id)) return
        val view = buildPostView(post) ?: return
        postViews[post.id] = view
        if (post.id > newestPostIdSeen) newestPostIdSeen = post.id
        val layout = findViewById<LinearLayout>(R.id.groupNewsLayout)
        if (atTop) {
            layout.addView(view, 0)
            highlightNewPost(view)
        } else {
            layout.addView(view)
        }
    }

    private fun startPosts() {
        if (postsStarted) return
        val warning = findViewById<TextView>(R.id.noPostsWarning)
        val loadingPosts = findViewById<ShimmerFrameLayout>(R.id.loadingPosts)
        if (!NetworkMonitor.isOnline.value) {
            loadingPosts.visibility = View.GONE
            warning.text = "Посты недоступны без сети"
            warning.visibility = View.VISIBLE
            return
        }
        postsStarted = true
        warning.visibility = View.GONE
        loadingPosts.visibility = View.VISIBLE
        showGroupViewModel.observePosts()
        showGroupViewModel.loadOldPosts(reset = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_group)

        currentGroupId = Globals.getInstance().getString("CurrentlyWatchingGroup").toLongOrNull() ?: 0
        showGroupViewModel.groupId = currentGroupId
        lastReadIdAtOpen = lastReadPostId(currentGroupId)
        newestPostIdSeen = lastReadIdAtOpen

        toolbar = findViewById(R.id.toolbar4)
        backIcon = ContextCompat.getDrawable(this, R.drawable.baseline_arrow_back_24)!!.mutate()
        backIcon.setColorFilter(ContextCompat.getColor(this, R.color.ok_green), android.graphics.PorterDuff.Mode.SRC_ATOP)
        toolbar.navigationIcon = backIcon
        toolbar.setNavigationOnClickListener { onBackPressed() }

        findViewById<LinearLayout>(R.id.showGroupMembers).setOnClickListener {
            startActivity(Intent(this, ShowGroupMembersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.showGroupRules).setOnClickListener {
            startActivity(Intent(this, ShowGroupRulesActivity::class.java))
        }

        val joinButton = findViewById<MaterialButton>(R.id.joinGroup)
        val allPostsLayout: LinearLayout = findViewById(R.id.groupNewsLayout)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.mainSwipeRefresh)
        swipeRefresh.setOnRefreshListener {
            showGroupViewModel.loadGroup()
            allPostsLayout.removeAllViews()
            postViews.clear()
            postsStarted = false
            startPosts()
            swipeRefresh.isRefreshing = false
        }

        showGroupViewModel.loadGroup()
        showGroupViewModel.group.observe(this, Observer { group ->
            swipeRefresh.isRefreshing = false
            group ?: return@Observer

            findViewById<TextView>(R.id.groupName).text = group.name
            findViewById<TextView>(R.id.groupAbout).text = group.about
            findViewById<TextView>(R.id.groupMembersCount).text = group.membersCount.toString()
            Glide.with(this).load(imageUrl("groups", group.rulesImageKey))
                .placeholder(R.drawable.baseline_person_24)
                .into(findViewById(R.id.groupImage))

            currentGroupName = group.name
            currentGroupCreator = group.creatorId
            myRank = groupRoleRank(group.myRole)

            reapplyJoinBtn(joinButton, group.myRole != null)
            postCreator(group.myRole != null)
            configureMenu(group.myRole)
            startPosts()
        })

        showGroupViewModel.loadCurrentUserAvatar()
        showGroupViewModel.currentUserAvatarKey.observe(this) { key ->
            myAvatarKey = key
            val targets = listOfNotNull(createPostAvatarImage) + currentUserImageViews.keys
            targets.forEach {
                Glide.with(this).load(imageUrl("users", key)).circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(it)
            }
        }

        showGroupViewModel.postsPage.observe(this) { page ->
            findViewById<ShimmerFrameLayout>(R.id.loadingPosts).visibility = View.GONE
            val warning = findViewById<TextView>(R.id.noPostsWarning)
            warning.foreground = ColorDrawable(ContextCompat.getColor(this, R.color.transparent))
            if (postViews.isEmpty() && page.isEmpty()) {
                warning.text = "Постов пока нет"
                warning.visibility = View.VISIBLE
            } else {
                warning.visibility = View.GONE
            }
            if (!readDividerHandled) {
                val firstReadIndex = page.indexOfFirst { it.id <= lastReadIdAtOpen }
                if (firstReadIndex >= 0) {
                    readDividerHandled = true
                    page.forEachIndexed { index, post ->
                        if (index == firstReadIndex && index > 0) {
                            findViewById<LinearLayout>(R.id.groupNewsLayout).addView(buildReadDividerView())
                        }
                        renderPost(post, atTop = false)
                    }
                } else {
                    page.forEach { renderPost(it, atTop = false) }
                }
            } else {
                page.forEach { renderPost(it, atTop = false) }
            }
        }

        showGroupViewModel.newPost.observe(this) { post ->
            if (post.groupId == currentGroupId) {
                findViewById<TextView>(R.id.noPostsWarning).visibility = View.GONE
                renderPost(post, atTop = true)
            }
        }
        showGroupViewModel.deletedPost.observe(this) { id -> removePostView(id) }

        showGroupViewModel.loadPinnedPost()
        showGroupViewModel.pinnedPost.observe(this) { post ->
            val previousPinnedId = pinnedPostId
            pinnedPostId = post?.id
            if (previousPinnedId != pinnedPostId) {
                previousPinnedId?.let { updatePinMenuTitle(it, false) }
                pinnedPostId?.let { updatePinMenuTitle(it, true) }
            }
            renderPinnedCard(post)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var wasOnline = NetworkMonitor.isOnline.value
                NetworkMonitor.isOnline.collect { online ->
                    if (online && !wasOnline && !postsStarted) startPosts()
                    wasOnline = online
                }
            }
        }

        setupScroll(allPostsLayout)
    }

    private fun configureMenu(myRole: String?) {
        if (menuConfigured) return
        toolbar.menu.clear()
        if (myRole == "creator") toolbar.inflateMenu(R.menu.popup_menu_group_creator)
        else if (myRole != null) toolbar.inflateMenu(R.menu.popup_menu_group)
        menuConfigured = true

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.editGroup -> {
                    if (currentUser == currentGroupCreator) {
                        startActivity(Intent(this, CreateGroupActivity::class.java).putExtra("EditGroupId", currentGroupId))
                    } else {
                        Toast.makeText(this, "Редактировать группу может только организатор", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.deleteGroup -> {
                    if (currentUser == currentGroupCreator) {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("Удаление группы")
                            .setMessage("Вы действительно хотите удалить эту группу?")
                            .setPositiveButton("Подтвердить") { _, _ ->
                                showGroupViewModel.deleteGroup { ok ->
                                    if (ok) {
                                        Globals.getInstance().setString("CurrentlyWatchingGroup", "")
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Не удалось удалить группу!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .setNegativeButton("Отмена") { d, _ -> d.dismiss() }
                            .show()
                        true
                    } else false
                }
                else -> super.onOptionsItemSelected(it)
            }
        }
    }

    private fun setupScroll(allPostsLayout: LinearLayout) {
        val separator: View = findViewById(R.id.separatorColor)
        val scrollView: ScrollView = findViewById(R.id.mainScrollViewGroupNews)
        var collapsed = false
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val bounds = Rect()
            scrollView.getHitRect(bounds)
            if (!separator.getLocalVisibleRect(bounds)) {
                if (!collapsed) {
                    animateToolbarCollapse(collapsing = true)
                    toolbar.title = currentGroupName
                    collapsed = true
                }
            } else if (collapsed) {
                animateToolbarCollapse(collapsing = false)
                toolbar.title = ""
                collapsed = false
            }
        }
        scrollView.setOnScrollChangeListener { view, _, _, _, _ ->
            val bottomGap = scrollView.getChildAt(scrollView.childCount - 1).bottom - (scrollView.height + view.scrollY)
            if (bottomGap <= 32.toPx()) {
                if (!showGroupViewModel.foundAll && !showGroupViewModel.fetching) {
                    showGroupViewModel.loadOldPosts()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Globals.getInstance().setString("CurrentlyWatchingGroup", "")
    }

    override fun onPause() {
        super.onPause()
        if (currentGroupId != 0L) saveLastReadPostId(currentGroupId, newestPostIdSeen)
    }
}
