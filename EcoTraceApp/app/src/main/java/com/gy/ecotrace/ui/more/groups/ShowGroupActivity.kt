package com.gy.ecotrace.ui.more.groups

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.GroupRepository
import com.gy.ecotrace.ui.more.groups.additional.ShowGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class ShowGroupActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar

    private var imageAttached = false
    private lateinit var attachedImage: Bitmap


    private val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
    private lateinit var showGroupViewModel: ShowGroupViewModel

    private val currentUser = Globals.getInstance().getString("CurrentlyLogged")
    private lateinit var currentGroupCreator: String
    private val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")
    private var currentGroupName = ""
    private lateinit var userGroupAbilities: DatabaseMethods.DataClasses.UserGroupAbilities
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
    private fun imageLoadWithLoading(folder: String, imageId: String, element: ImageView, placeHolder: Int, circle: Boolean = true) {
        var img = Glide.with(this@ShowGroupActivity)
            .load(Globals().getImgUrl(folder, imageId))
            .placeholder(placeHolder)
            .skipMemoryCache(true)

        if (circle) img = img.circleCrop()
        img.into(element)
        element.foreground =
            ColorDrawable(ContextCompat.getColor(applicationContext, R.color.transparent))
    }

    private fun reapplyJoinBtn(joinButton: MaterialButton, it: Boolean){ // todo
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
                        showGroupViewModel.leaveGroup(currentUser, currentGroup)
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
                showGroupViewModel.joinGroup(currentUser, currentGroup)
            }
        }
    }

    private fun postCreator(available: Boolean){
        val postLayout = findViewById<RelativeLayout>(R.id.menuCreatePost)
        if (!available) postLayout.visibility = View.GONE
        else postLayout.visibility = View.VISIBLE
        postLayout.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CreateGroupPostMenuActivity::class.java
                )
            )
        }
        findViewById<LinearLayout>(R.id.attachImage).setOnClickListener {
            showImageSourceDialog()
        }
        val publishButton = findViewById<Button>(R.id.publishPost)
        publishButton.setOnClickListener {
            if (imageAttached) {
                val imageName = "$currentGroup-$currentUser"
                showGroupViewModel.uploadImage("posts", imageName, attachedImage) {
                    showGroupViewModel.createPost(currentGroup, currentUser, null, it)

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
    }

    private fun fillPostLayout(mainLayout: View, post: DatabaseMethods.DataClasses.Post): Boolean {
        var hasText = false

        if ((post.postCreatorId != currentGroupCreator && currentUser == currentGroupCreator) ||
            (currentUser == post.postCreatorId || userGroupAbilities.deletePosts)) {
            val postToolbar = mainLayout.findViewById<Toolbar>(R.id.postToolbar)
            postToolbar.inflateMenu(R.menu.popup_menu_group_posts)
            postToolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.deletePost -> showGroupViewModel.deletePost(currentGroup, post.postId)
                }
                true
            }
        }

        val postText = mainLayout.findViewById<TextView>(R.id.postContentText)
        if (post.postContentText != null) {
            postText.text =
                post.postContentText
            hasText = true
        } else {
            postText.visibility = View.GONE
        }

        var hasImage = false
        val postImage: ImageView = mainLayout.findViewById(R.id.postContentImage)
        if (post.postContentImageURI != null) {
            imageLoadWithLoading("posts", post.postContentImageURI!!, postImage, R.drawable.baseline_email_24, false)
            hasImage = true
        } else {
            postImage.visibility = View.GONE
        }

        if (!hasImage && !hasText) {
            Log.d("no content", "nothing found ${post.postId}")
            return false
        }

        if (currentUser == "0") {
            mainLayout.findViewById<LinearLayout>(R.id.bottomInfoPost).visibility =
                View.GONE
        }
        else {
            imageLoadWithLoading("users",
                currentUser,
                mainLayout.findViewById(R.id.currentUserImage),
                R.drawable.baseline_person_24)

            mainLayout.findViewById<LinearLayout>(R.id.copyThisPostText)
                .setOnClickListener {
                    val clipboard : ClipboardManager =
                        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Текст скопирован", post.postContentText)
                    clipboard.setPrimaryClip(clip)
                }

            mainLayout.findViewById<LinearLayout>(R.id.likeThisPost).setOnClickListener {
                //todo
            }

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

            }
        } // if user != 0 end if

        val countComments = post.postCommentsCount
        val ending = when{
            countComments % 100 in 11..14 -> "ев"
            countComments % 10 == 1L -> "й"
            countComments % 10 in 2..4 -> "я"
            else -> "ев"
        }
        mainLayout.findViewById<TextView>(R.id.commentsCountPost).text = "$countComments комментари$ending"
        mainLayout.findViewById<TextView>(R.id.postCreatorName).text = post.postCreatorName
        imageLoadWithLoading("users", post.postCreatorId, mainLayout.findViewById(R.id.postCreatorImage), R.drawable.baseline_person_24)
        mainLayout.findViewById<LinearLayout>(R.id.openCreatorProfileLayout).setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatching", post.postCreatorId)
            startActivity(
                Intent(this, ProfileActivity::class.java)
            )
        }


        val currentYear = LocalDate.now().year
        val sentTime = ZonedDateTime.parse(post.postId, DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(
            ZoneId.of("UTC")))
        val localTime = sentTime.withZoneSameInstant(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("dd MMMM${if (currentYear != sentTime.year) " yyyy" else ""}, HH:mm", Locale.getDefault())
        val formattedDateTime = localTime.format(formatter)
        mainLayout.findViewById<TextView>(R.id.postCreateTime).text = formattedDateTime


        mainLayout.findViewById<LinearLayout>(R.id.showPostComments).setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatchingPost", post.postId)
            startActivity(
                Intent
                    (
                    this,
                    ShowPostWithCommentsActivity::class.java)
            )
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
                    startActivity(
                        Intent(this, CreateGroupActivity::class.java)
                    )
                    true
                }


                R.id.deleteGroup -> {
                    if (currentUser == currentGroupCreator) {
                        val builder = android.app.AlertDialog.Builder(this)
                        builder.setTitle("Удаление группы")

                        builder.setMessage("Вы действительно хотите безвозвртно удалить данную группу?")
                        builder.setPositiveButton("Подтвердить") { dialog, which ->

                            //finish()
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
        showGroupViewModel = ViewModelProvider(this, ShowGroupViewModelFactory.getInstance(repository, true))[ShowGroupViewModel::class.java]
        requestPermissions()
        showGroupViewModel.getGroup(currentGroup)

        Glide.get(this).clearMemory()
        setContentView(R.layout.activity_show_group)
        toolbar = findViewById(R.id.toolbar4)
        Globals().initToolbarIconBack(toolbar, applicationContext, R.color.pair2Color2)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        imageLoadWithLoading("groups", currentGroup, findViewById(R.id.groupImage), R.drawable.round_family_restroom_24, false)
        imageLoadWithLoading("users", currentUser, findViewById(R.id.currentUserImage), R.drawable.baseline_person_24)

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
        var canSendPosts = false

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.mainSwipeRefresh)
        swipeRefresh.setOnRefreshListener { // update group info only!!
            showGroupViewModel.getGroup(currentGroup)
        }
        showGroupViewModel.getGroup(currentGroup)
        userGroupAbilities = GroupRepository.Functions().userAbilitiesInGroup(4)
        showGroupViewModel.group.observe(this, Observer {
            swipeRefresh.isRefreshing = false
            it?.let {
                Log.d("new group data!", it.toString())
                findViewById<TextView>(R.id.groupName).text = it.groupName
                findViewById<TextView>(R.id.groupAbout).text = it.groupAbout ?: "Нет описания"
                findViewById<TextView>(R.id.groupMembersCount).text = it.groupCountMembers.toString()
                currentGroupName = it.groupName
                currentGroupCreator = it.groupCreatorId
                if (currentUser == currentGroupCreator) {
                    canLeaveTheGroup = false
                    userGroupAbilities = GroupRepository.Functions().userAbilitiesInGroup(0)
                    currentUserRole = 0
                } else {
                    showGroupViewModel.isUserInThisGroup(currentUser, currentGroup)
                    showGroupViewModel.userInGroup.observe(this) { inGroup ->
                        canSendPosts = inGroup
                        postCreator(inGroup)
                        reapplyJoinBtn(joinButton, inGroup)
                        if (inGroup) {
                            showGroupViewModel.userRole(currentUser, currentGroup) { userRole ->
                                currentUserRole = userRole
                                userGroupAbilities =
                                    GroupRepository.Functions().userAbilitiesInGroup(userRole)
                                Log.d("userRole", userRole.toString())
                                if (userRole <= 1) {
                                    Log.d("userRole groupMenu", "$userRole <= 1 ${userRole <= 1}")
                                    groupMenu()
                                }
                            }
                        }
                    }
                }

                GroupRepository.DataStorage().groupData = it
            }
        })





        val allPostsLayout: LinearLayout = findViewById(R.id.groupNewsLayout)
        val warning = findViewById<TextView>(R.id.noPostsWarning)

        val addedPosts = mutableListOf<String>()
        showGroupViewModel.getAndObservePosts(currentGroup)
        showGroupViewModel.allFoundPosts.observe(this, Observer  {
            Log.d("got observe", it.toString())
            var isNew = addedPosts.isNotEmpty()
            warning.foreground = ColorDrawable(ContextCompat.getColor(this, R.color.transparent))
            if (it.isNotEmpty()) {
                warning.visibility = View.GONE
            }
            GroupRepository.DataStorage().groupPosts.plus(it)
            for (postKey in it.keys) {
                Log.d("checking...", postKey)
                if (!addedPosts.contains(postKey)) {
                    val post = it[postKey]!!
                    val postLayout =
                        layoutInflater.inflate(R.layout.layout_user_post_in_group, null)
                    val success = fillPostLayout(postLayout, post)
                    if (success) {
                        addedPosts.add(postKey)
                        if (isNew) {
                            allPostsLayout.addView(postLayout, 0)
                        } else {
                            allPostsLayout.addView(postLayout)
                        }
                    }
                } else {
                    isNew = false
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
                Log.d("scrollview", "bottom ${showGroupViewModel.lastPostId}")
                if (showGroupViewModel.lastPostId.first) {
                    showGroupViewModel.getAndObservePosts(currentGroup)
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

