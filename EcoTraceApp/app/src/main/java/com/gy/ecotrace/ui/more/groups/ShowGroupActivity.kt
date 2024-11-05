package com.gy.ecotrace.ui.more.groups

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.ShowGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


class ShowGroupActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private var userRole = 3

    private var imageAttached = false
    private lateinit var attachedImage: Bitmap


    private val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
    private lateinit var showGroupViewModel: ShowGroupViewModel

    private val currentUser = ETAuth.getInstance().getUID()
    private lateinit var currentGroupCreator: String
    private val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")
    private var currentGroupName = ""
    private var canLeaveTheGroup = false
    private var currentUserRole = 4

    private var imageAttachedF = fun () {}
    private fun Int.toPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
    private fun animationColorChange(colorFrom: Int, colorTo: Int) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 300
        colorAnimation.addUpdateListener { animator ->
            window.statusBarColor = animator.animatedValue as Int
            toolbar.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }
    private fun requestPermissions() {
        listOf(android.Manifest.permission.CAMERA,  android.Manifest.permission.READ_EXTERNAL_STORAGE).forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(it), 100)
            }
        }
    }

    private fun reapplyJoinBtn(joinButton: MaterialButton, it: Boolean){
        Log.d("Button", "reapplying to $it")
        if (it) {
            joinButton.text = "В группе"
            joinButton.setTextColor(ContextCompat.getColor(applicationContext, R.color.ok_green))
            joinButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.dirt_white))
            joinButton.setOnClickListener{
                val builder = android.app.AlertDialog.Builder(this)
                builder.setTitle("Выход из группы")
                if (currentUser != currentGroupCreator) {

                    builder.setMessage("Вы действительно хотите покинуть эту группу?")
                    builder.setPositiveButton("Подтвердить") { dialog, which ->
                        showGroupViewModel.leaveGroup {
                            showGroupViewModel.isUserInThisGroup()
                        }
                        groupMenu(false)
                    }
                    builder.setNegativeButton("Отмена") { dialog, which ->
                        dialog.dismiss()
                    }
                } else {
                    builder.setMessage("Вы не можете покинуть эту группу, так как являетесь ее создателем! Для выхода из группы сначала надо ее удалить!")
                    builder.setNeutralButton("Понятно") { dialog, which ->
                        dialog.dismiss()
                    }
                }
                val dialog = builder.create()
                dialog.show()

            }
        } else {
            joinButton.text = "Присоединиться"
            joinButton.setTextColor(ContextCompat.getColor(applicationContext, R.color.dirt_white))
            joinButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.ok_green))
            joinButton.setOnClickListener{
                showGroupViewModel.joinGroup {
                    showGroupViewModel.isUserInThisGroup()
                }
            }
        }
    }

    private fun postCreator(available: Boolean){
        val postLayout = findViewById<RelativeLayout>(R.id.menuCreatePost)
        if (!available) postLayout.visibility = View.GONE
        else postLayout.visibility = View.VISIBLE
        postLayout.setOnClickListener {
            val intent = Intent(this, CreateGroupPostMenuActivity::class.java)
            intent.putExtra("groupName", currentGroupName)
            startActivity(intent)
        }
        findViewById<LinearLayout>(R.id.attachImage).setOnClickListener {
            showImageSourceDialog()
        }
        Glide.with(this@ShowGroupActivity)
            .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", currentUser))
            .circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(findViewById(R.id.currentUserImage))
        val publishButton = findViewById<Button>(R.id.publishPost)
        publishButton.setOnClickListener {
            if (imageAttached) {
                showGroupViewModel.createPost(null, attachedImage) {
                    if (!it) {
                        Toast.makeText(this@ShowGroupActivity, "Не удалось создать пост!", Toast.LENGTH_SHORT).show() // todo
                    }
                }
            }
        }
        imageAttachedF = fun () {
            postLayout.layoutParams.height = if (imageAttached) 350.toPx() else 150.toPx()
            publishButton.setBackgroundColor(ContextCompat.getColor(applicationContext,
                if (imageAttached) R.color.ok_green else R.color.silver))
            findViewById<ImageButton>(R.id.removeAttachedImage).visibility =
                if (imageAttached) View.VISIBLE else View.GONE
            findViewById<ImageView>(R.id.attachedImage).visibility =
                if (imageAttached) View.VISIBLE else View.GONE

        }
        findViewById<ImageButton>(R.id.removeAttachedImage).setOnClickListener {
            imageAttached = false
            imageAttachedF()
        }
    } // todo

    private fun fillPostLayout(mainLayout: View, post: DatabaseMethods.DataClasses.Post): Boolean {
        fun ending(comments: Int): String {
            return when{
                comments % 100 in 11..14 -> "ев"
                comments % 10 == 1 -> "й"
                comments % 10 in 2..4 -> "я"
                else -> "ев"
            }
        }

        var hasText = false

        if (post.postCreatorId == currentUser
            || (userRole <= 2 && userRole < post.postCreatorRole)
            ) {
            val postToolbar = mainLayout.findViewById<androidx.appcompat.widget.Toolbar>(R.id.postToolbar)
            postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
            postToolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.deletePost -> {
                        showGroupViewModel.deletePost(post.postId) {
                            if (it) {
                                (mainLayout.parent as LinearLayout).removeView(mainLayout)
                            } else {
                                Toast.makeText(this@ShowGroupActivity, "Не удалось удалить пост!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                true
            }
        }

        val loading: ShimmerFrameLayout = mainLayout.findViewById(R.id.postContentLoading)

        var hasImage = false
        val postImage: ImageView = mainLayout.findViewById(R.id.postContentImage)
        if (post.postContentImage != null) {
            hasImage = true
            Glide.with(this@ShowGroupActivity)
                .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("posts", post.postContentImage!!))
                .into(postImage)
                .runCatching {
                    loading.visibility = View.GONE
                    postImage.visibility = View.VISIBLE
                }
        }

        val postText = mainLayout.findViewById<TextView>(R.id.postContentText)
        if (post.postContentText != null) {
            if (!hasImage) {
                loading.visibility = View.GONE
            }
            postText.text =
                post.postContentText
            postText.visibility = View.VISIBLE
            hasText = true
        }

        if (!hasImage && !hasText) {
            Log.d("no content", "nothing found ${post.postId}")
            return false
        }
        else {
            Glide.with(this@ShowGroupActivity)
                .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", currentUser))
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into( mainLayout.findViewById(R.id.currentUserImage))

            val postComment: ImageButton = mainLayout.findViewById(R.id.postSendComment)
            val commentView: EditText = mainLayout.findViewById(R.id.postCreateCommentEntry)
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
                showGroupViewModel.sendComment(post.postId, commentView.text.toString()) {
                    if (!it) {
                        Toast.makeText(this@ShowGroupActivity, "Произошла ошибка!", Toast.LENGTH_SHORT).show()
                    } else {
                        showGroupViewModel.getNumComments(post.postId) {
                            val ending = ending(it)
                            mainLayout.findViewById<TextView>(R.id.commentsCountPost).text = "$it комментари$ending"
                        }
                        Toast.makeText(this@ShowGroupActivity, "Ваш комментарий отправлен!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val countComments = post.postCommentsCount
        val ending = ending(countComments.toInt())
        mainLayout.findViewById<TextView>(R.id.commentsCountPost).text = "$countComments комментари$ending"
        mainLayout.findViewById<TextView>(R.id.postCreatorName).text = post.postCreatorName
        Glide.with(this@ShowGroupActivity)
            .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", post.postCreatorId))
            .circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into( mainLayout.findViewById(R.id.postCreatorImage))
        mainLayout.findViewById<LinearLayout>(R.id.openCreatorProfileLayout).setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatching", post.postCreatorId)
            startActivity(
                Intent(this, ProfileActivity::class.java)
            )
        }


        val currentYear = LocalDate.now().year
        val sentTime = Instant.ofEpochMilli(post.postTime*1000)
            .atZone(ZoneId.of("UTC+3"))
        val localTime = sentTime.withZoneSameInstant(ZoneId.systemDefault())

        val formatter = DateTimeFormatter.ofPattern("dd MMMM${if (currentYear != sentTime.year) " yyyy" else ""}, HH:mm", Locale.getDefault())
        val formattedDateTime = localTime.format(formatter)
        mainLayout.findViewById<TextView>(R.id.postCreateTime).text = formattedDateTime



        mainLayout.findViewById<LinearLayout>(R.id.showPostComments).setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatchingPost", post.postId.toString())
            val intent = Intent(this, ShowPostWithCommentsActivity::class.java)
            intent.putExtra("data", Gson().toJson(post))
            startActivity(intent)
        }
        return true
    }

    private fun groupMenu(show: Boolean = true) {
        if (!show) {
            toolbar.menu.clear()
            return
        }
        if (currentUser == currentGroupCreator) {
            toolbar.inflateMenu(R.menu.popup_menu_group_creator)
        }
        else {
            toolbar.inflateMenu(R.menu.popup_menu_group)
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.editGroup -> {
                    val intent = Intent(this, CreateGroupActivity::class.java)
                    intent.putExtra("data", Gson().toJson(showGroupViewModel.group.value))
                    showGroupViewModel.getRules {
                        intent.putExtra("rules", Gson().toJson(it))
                        startActivity(intent)
                    }
                    true
                }


                R.id.deleteGroup -> {
                    if (currentUser == currentGroupCreator) {
                        val builder = android.app.AlertDialog.Builder(this)
                        builder.setTitle("Удаление группы")

                        builder.setMessage("Вы действительно хотите удалить эту группу?")
                        builder.setPositiveButton("Подтвердить") { dialog, which ->
                            showGroupViewModel.delete {
                                if (it) {
                                    finish()
                                } else {
                                    Toast.makeText(this@ShowGroupActivity, "Не удалось удалить группу!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        builder.setNegativeButton("Отмена") { dialog, which ->
                            dialog.dismiss()
                        }
                        val dialog = builder.create()
                        dialog.show()
                        true
                    } else false
                }

                else -> {
                    super.onOptionsItemSelected(it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_group)

        toolbar = findViewById(R.id.toolbar4)
        Globals().initToolbarIconBack(toolbar, applicationContext, R.color.pair2Color2)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        showGroupViewModel = ViewModelProvider(this, ShowGroupViewModelFactory(repository))[ShowGroupViewModel::class.java]
        showGroupViewModel.groupId = currentGroup

        findViewById<LinearLayout>(R.id.showGroupMembers).setOnClickListener {
            startActivity(
                Intent(this, ShowGroupMembersActivity::class.java)
            )
        }
        findViewById<LinearLayout>(R.id.showGroupRules).setOnClickListener {
            startActivity(
                Intent(this, ShowGroupRulesActivity::class.java)
            )
        }

        val joinButton = findViewById<MaterialButton>(R.id.joinGroup)
        val allPostsLayout: LinearLayout = findViewById(R.id.groupNewsLayout)
        val warning = findViewById<TextView>(R.id.noPostsWarning)
        val loadingPosts = findViewById<ShimmerFrameLayout>(R.id.loadingPosts)

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.mainSwipeRefresh)
        swipeRefresh.setOnRefreshListener {
            showGroupViewModel.getGroup()
            showGroupViewModel.isUserInThisGroup()
            allPostsLayout.removeAllViews()
//            showGroupViewModel.anyBreak = true
            toolbar.menu.clear()
            showGroupViewModel.getRole {
                userRole = it
                showGroupViewModel.getOldPosts(true)
            }
            loadingPosts.visibility = View.VISIBLE
//            showGroupViewModel.getNewPosts()
        }
        showGroupViewModel.getGroup()
        showGroupViewModel.isUserInThisGroup()
        showGroupViewModel.group.observe(this, Observer {
            swipeRefresh.isRefreshing = false
            it?.let {
                findViewById<TextView>(R.id.groupName).text = it.groupName
                findViewById<TextView>(R.id.groupAbout).text = it.groupAbout
                findViewById<TextView>(R.id.groupMembersCount).text = it.groupCountMembers.toString()

                Glide.with(this@ShowGroupActivity)
                    .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("groups", it.groupId))
                    .into(findViewById(R.id.groupImage))

                currentGroupName = it.groupName
                currentGroupCreator = it.groupCreatorId

                showGroupViewModel.getRole {
                    userRole = it
                    if (it == 0 && !showGroupViewModel.anyBreak) groupMenu(true)
                    showGroupViewModel.getOldPosts()
                }
            }
        })
        showGroupViewModel.userInGroup.observe(this) {
            reapplyJoinBtn(joinButton, it)
            postCreator(it)
            showGroupViewModel.getGroup()
            showGroupViewModel.anyBreak = true

            showGroupViewModel.getRole {
                userRole = it
            }
            loadingPosts.visibility = View.VISIBLE
        }





        showGroupViewModel.allFoundPosts.observe(this, Observer  {
            showGroupViewModel.fetching = false
            loadingPosts.visibility = View.GONE
            if (it == null) {
                return@Observer
            }
            warning.foreground = ColorDrawable(ContextCompat.getColor(this, R.color.transparent))
            if (it.isNotEmpty()) {
                warning.visibility = View.GONE
            }
            for (post in it.sortedBy{ post -> post.postId }.reversed()) {
                val postLayout = layoutInflater.inflate(R.layout.layout_user_post_in_group, null)
                val success = fillPostLayout(postLayout, post)
                if (success) {
                    if (showGroupViewModel.isNew) {
                        allPostsLayout.addView(postLayout, 0)
                        showGroupViewModel.isNew = false
                    } else {
                        allPostsLayout.addView(postLayout)
                    }
                }
            }
        })

        val separator: View = findViewById(R.id.separatorColor)
        val scrollView: ScrollView = findViewById(R.id.mainScrollViewGroupNews)
        var fHideTSee = false
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollBounds = Rect()
            scrollView.getHitRect(scrollBounds)
            if (!separator.getLocalVisibleRect(scrollBounds)) {
                if (!fHideTSee) {
                    animationColorChange(
                        ContextCompat.getColor(this, R.color.transparent),
                        ContextCompat.getColor(this, R.color.dirt_white)
                    )
                    Globals().initToolbarIconBack(toolbar, applicationContext, R.color.black)
                    toolbar.title = currentGroupName
                    fHideTSee = true
                }
            } else {
                if (fHideTSee) {
                    animationColorChange(
                        ContextCompat.getColor(this, R.color.dirt_white),
                        ContextCompat.getColor(this, R.color.transparent)
                    )
                    Globals().initToolbarIconBack(toolbar, applicationContext, R.color.pair2Color2)
                    toolbar.title = ""
                    fHideTSee = false
                }
            }

        }

        scrollView.setOnScrollChangeListener { view, _,_,_,_ ->
            if (scrollView.getChildAt(scrollView.childCount-1).bottom == scrollView.height+view.scrollY) {
                if (!showGroupViewModel.foundAll && !showGroupViewModel.fetching) {
                    showGroupViewModel.fetching = true
                    showGroupViewModel.getOldPosts()
                }
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
                        imageAttached = true
                        imageAttachedF()
                    }
                }
            }
        }
    }

    private fun addImageToLayout(imageBitmap: Bitmap) {
        findViewById<ImageView>(R.id.attachedImage).setImageBitmap(imageBitmap)
        attachedImage = imageBitmap
    }
}

