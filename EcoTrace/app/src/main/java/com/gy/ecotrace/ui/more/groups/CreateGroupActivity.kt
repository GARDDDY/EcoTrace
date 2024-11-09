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
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateGroupActivity : AppCompatActivity() {

    private val sharedViewModel: CreateGroupViewModel by viewModels()
    private lateinit var groupImage: ImageView

    private var isBinding = false

    private var nameValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_group)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        groupImage = findViewById(R.id.groupImage)
        groupImage.setOnClickListener { showImageSourceDialog() }

        val nameField: TextInputEditText = findViewById(R.id.groupName)
        val aboutField: TextInputEditText = findViewById(R.id.groupAbout)
        val rulesField: TextInputEditText = findViewById(R.id.groupRules)
        val typeToggle: MaterialButtonToggleGroup = findViewById(R.id.groupTypeToggle)
        val createGroup: Button = findViewById(R.id.finishCreateGroup)

        val editGroupId = intent.getLongExtra("EditGroupId", 0).takeIf { it != 0L }

        fun setSaveEnabled(enabled: Boolean) {
            createGroup.backgroundTintList = ContextCompat.getColorStateList(
                applicationContext, if (enabled) R.color.ok_green else R.color.silver
            )
            createGroup.isClickable = enabled
        }
        setSaveEnabled(false)

        nameField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (isBinding) return
                val name = p0?.toString().orEmpty()
                if (name.length < 5) {
                    nameValid = false
                    setSaveEnabled(false)
                    return
                }
                sharedViewModel.isGroupNameAvailable(name) { error ->
                    nameValid = error == null
                    sharedViewModel.groupName = if (nameValid) name else null
                    nameField.backgroundTintList = ContextCompat.getColorStateList(
                        applicationContext, if (nameValid) R.color.ok_green else R.color.red_no
                    )
                    setSaveEnabled(nameValid)
                    if (error != null) Toast.makeText(this@CreateGroupActivity, error, Toast.LENGTH_SHORT).show()
                }
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        aboutField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (!isBinding) sharedViewModel.groupAbout = p0?.toString()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        rulesField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (!isBinding) sharedViewModel.groupRulesText = p0?.toString()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        typeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isBinding && isChecked) {
                sharedViewModel.groupType = if (checkedId == R.id.typeClosed) 1 else 0
            }
        }

        createGroup.setOnClickListener {
            sharedViewModel.submitGroup { error ->
                if (error == null) {
                    Toast.makeText(this, if (editGroupId == null) "Группа создана!" else "Изменения применены!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (editGroupId != null) {
            createGroup.text = "Сохранить"
            sharedViewModel.loadForEdit(editGroupId) { ok ->
                if (!ok) {
                    Toast.makeText(this, "Не удалось загрузить группу для редактирования", Toast.LENGTH_SHORT).show()
                    finish()
                    return@loadForEdit
                }
                isBinding = true
                nameField.setText(sharedViewModel.groupName)
                aboutField.setText(sharedViewModel.groupAbout)
                rulesField.setText(sharedViewModel.groupRulesText)
                typeToggle.check(if (sharedViewModel.groupType == 1) R.id.typeClosed else R.id.typeOpen)
                sharedViewModel.groupImageKey?.let { key ->
                    Glide.with(this).load("${BuildConfig.SERVER_API_URI}uploads/groups/$key")
                        .placeholder(R.color.silver)
                        .into(groupImage)
                }
                isBinding = false
                nameValid = true
                setSaveEnabled(true)
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
                            val inputStream = selectedImageUri?.let { uri ->
                                contentResolver.openInputStream(uri)
                            }
                            addImageToLayout(BitmapFactory.decodeStream(inputStream))
                        }
                    }
                }
            }
        }
    }

    private fun addImageToLayout(imageBitmap: Bitmap) {
        groupImage.setImageBitmap(imageBitmap)
        sharedViewModel.groupImage = imageBitmap
    }
}
