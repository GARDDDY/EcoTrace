package com.gy.ecotrace.ui.more.groups

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.CreateGroupPostMenuViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupPostMenuViewModel

class CreateGroupPostMenuActivity : AppCompatActivity() {

    private val currentUser = ETAuth.getInstance().getUID()
    private lateinit var showGroupViewModel: CreateGroupPostMenuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_group_post_menu)

        Glide.with(this@CreateGroupPostMenuActivity)
            .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", currentUser))
            .circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(findViewById(R.id.userImage))

        val groupName = intent.getStringExtra("groupName")

        findViewById<TextView>(R.id.currentGroupName).text = groupName

        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        showGroupViewModel = ViewModelProvider(this, CreateGroupPostMenuViewModelFactory(repository))[CreateGroupPostMenuViewModel::class.java]
        showGroupViewModel.groupId = Globals.getInstance().getString("CurrentlyWatchingGroup")

        val publish: Button = findViewById(R.id.publishPost)

        publish.setOnClickListener {
            showGroupViewModel.createPost {
                if (it) {
                    finish()
                } else {
                    Toast.makeText(this@CreateGroupPostMenuActivity, "Произошла ошибка! Повторите снова", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val textContent = findViewById<EditText>(R.id.postTextContent)

        textContent.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                showGroupViewModel.textContent = p0?.toString()

                publish.backgroundTintList = when (p0.isNullOrEmpty()) {
                    true -> {
                        if (showGroupViewModel.image != null) { ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.ok_green)) }
                        else { ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.silver)) }
                    }
                    false -> ColorStateList.valueOf(
                        ContextCompat.getColor(applicationContext, R.color.ok_green))
                }
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })


        val imageAttacher = findViewById<LinearLayout>(R.id.attachImage)
        val imageContent = findViewById<ImageView>(R.id.postImage)
        imageAttacher.setOnClickListener {
            showImageSourceDialog()
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
        findViewById<ImageView>(R.id.postImage).setImageBitmap(imageBitmap)
        showGroupViewModel.image = imageBitmap
    }
}