package com.gy.ecotrace.ui.more.groups

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.repos.UserRepository
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupPostMenuViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class CreateGroupPostMenuActivity : AppCompatActivity() {

    private val showGroupViewModel: CreateGroupPostMenuViewModel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var textContent: EditText
    private lateinit var charCounter: TextView
    private lateinit var publish: MaterialButton
    private lateinit var publishProgress: ProgressBar
    private lateinit var imagesScroll: HorizontalScrollView
    private lateinit var imagesContainer: LinearLayout
    private var published = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_group_post_menu)

        val toolbar: Toolbar = findViewById(R.id.toolbar8)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { confirmDiscardIfNeeded { finish() } }

        Glide.with(this)
            .load(R.drawable.baseline_person_24)
            .circleCrop()
            .into(findViewById(R.id.userImage))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userRepository.observeCurrentUser().collect { user ->
                    val avatarKey = user?.avatarKey ?: return@collect
                    Glide.with(this@CreateGroupPostMenuActivity)
                        .load("${BuildConfig.SERVER_API_URI}uploads/users/$avatarKey")
                        .placeholder(R.drawable.baseline_person_24)
                        .circleCrop()
                        .into(findViewById(R.id.userImage))
                }
            }
        }

        val groupName = intent.getStringExtra("groupName")
        findViewById<TextView>(R.id.currentGroupName).text = groupName

        showGroupViewModel.groupId =
            Globals.getInstance().getString("CurrentlyWatchingGroup").toLongOrNull() ?: 0

        publish = findViewById(R.id.publishPost)
        publishProgress = findViewById(R.id.publishProgress)
        textContent = findViewById(R.id.postTextContent)
        charCounter = findViewById(R.id.charCounter)
        imagesScroll = findViewById(R.id.postImagesScroll)
        imagesContainer = findViewById(R.id.postImagesContainer)

        textContent.filters = arrayOf(InputFilter.LengthFilter(MAX_POST_LENGTH))
        textContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                showGroupViewModel.textContent = p0?.toString()
                charCounter.text = getString(R.string.charCounter, p0?.length ?: 0, MAX_POST_LENGTH)
                updatePublishEnabled()
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        findViewById<Chip>(R.id.attachImageChip).setOnClickListener { showImageSourceDialog() }

        publish.setOnClickListener {
            publish.isEnabled = false
            publish.text = ""
            publishProgress.visibility = View.VISIBLE
            showGroupViewModel.createPost { success ->
                if (success) {
                    published = true
                    PostDraftStore.clear(this, showGroupViewModel.groupId)
                    finish()
                } else {
                    publishProgress.visibility = View.GONE
                    publish.text = getString(R.string.publish)
                    updatePublishEnabled()
                    Toast.makeText(this@CreateGroupPostMenuActivity, getString(R.string.postPublishError), Toast.LENGTH_SHORT).show()
                }
            }
        }

        restoreDraftIfAny()
        charCounter.text = getString(R.string.charCounter, textContent.text?.length ?: 0, MAX_POST_LENGTH)
        renderImages()
        updatePublishEnabled()

        if (intent.getBooleanExtra("openPickerImmediately", false)) {
            showImageSourceDialog()
        }
    }

    private fun restoreDraftIfAny() {
        val (text, images) = PostDraftStore.load(this, showGroupViewModel.groupId)
        if (!text.isNullOrBlank()) textContent.setText(text)
        showGroupViewModel.images.addAll(images.take(MAX_ATTACHED_IMAGES))
    }

    override fun onPause() {
        super.onPause()
        if (!published) {
            PostDraftStore.save(this, showGroupViewModel.groupId, showGroupViewModel.textContent, showGroupViewModel.images)
        }
    }

    override fun onBackPressed() {
        confirmDiscardIfNeeded { super.onBackPressed() }
    }

    private fun confirmDiscardIfNeeded(onConfirmed: () -> Unit) {
        val hasContent = !showGroupViewModel.textContent.isNullOrBlank() || showGroupViewModel.images.isNotEmpty()
        if (!hasContent) {
            onConfirmed()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.discardPostTitle)
            .setMessage(R.string.discardPostMessage)
            .setPositiveButton(R.string.discard) { dialog, _ ->
                dialog.dismiss()
                published = true
                PostDraftStore.clear(this, showGroupViewModel.groupId)
                onConfirmed()
            }
            .setNegativeButton(R.string.keepEditing) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updatePublishEnabled() {
        val hasText = !showGroupViewModel.textContent.isNullOrBlank()
        val hasImages = showGroupViewModel.images.isNotEmpty()
        publish.isEnabled = hasText || hasImages
    }

    private fun renderImages() {
        renderEditableThumbnails(this, imagesScroll, imagesContainer, showGroupViewModel.images) { index ->
            showGroupViewModel.images.removeAt(index)
            renderImages()
            updatePublishEnabled()
        }
    }

    private fun showImageSourceDialog() {
        startActivityForResult(buildImagePickerChooser(this), REQUEST_PICK_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_IMAGES || resultCode != Activity.RESULT_OK) return

        val picked = extractPickedBitmaps(this, data)
        val room = MAX_ATTACHED_IMAGES - showGroupViewModel.images.size
        if (room <= 0) {
            Toast.makeText(this, getString(R.string.tooManyImages, MAX_ATTACHED_IMAGES), Toast.LENGTH_SHORT).show()
            return
        }
        showGroupViewModel.images.addAll(picked.take(room))
        renderImages()
        updatePublishEnabled()
    }

    companion object {
        private const val MAX_POST_LENGTH = 2000
        private const val REQUEST_PICK_IMAGES = 100
    }
}
