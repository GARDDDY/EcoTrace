package com.gy.ecotrace.ui.more.groups

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setMargins
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.GroupRepository
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupPostMenuViewModel
import com.yandex.mapkit.search.Line
import org.w3c.dom.Text

class CreateGroupPostMenuActivity : AppCompatActivity() {
    private var hasContent = false
    private var attachedImage: Bitmap? = null
    private var hasImg = false
    private var contentText: String? = null
    private lateinit var createPostViewModel: CreateGroupPostMenuViewModel
    private val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")
    private val currentUser = Globals.getInstance().getString("CurrentlyLogged")

    private lateinit var publish: Button
    private var actualText: String = ""
    private val links: MutableMap<Int, DatabaseMethods.DataClasses.EcoTraceLinkResource> = mutableMapOf() // Link id, <Link, String, DeleteNext

    private var imageAttachedF = fun () {}

    private fun reHasContent() {

        publish.isClickable = hasContent
        if (hasContent) {
            publish.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.ok_green))
        } else {
            publish.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.silver))
        }

        publish.setOnClickListener {
            if (hasImg) {
                val imageName = "$currentGroup-$currentUser"
                createPostViewModel.uploadImage("posts", imageName, attachedImage!!) {
                    createPostViewModel.createPost(currentGroup, currentUser, contentText, it)
                }
            } else createPostViewModel.createPost(currentGroup, currentUser, contentText, null)
        }
    }

    private fun imageLoadWithLoading(folder: String, imageId: String, element: ImageView, placeHolder: Int, circle: Boolean = true) {
        var img = Glide.with(this@CreateGroupPostMenuActivity)
            .load(Globals().getImgUrl(folder, imageId))
            .placeholder(placeHolder)
            .skipMemoryCache(true)

        if (circle) img = img.circleCrop()
        img.into(element)
        element.foreground =
            ColorDrawable(ContextCompat.getColor(applicationContext, R.color.transparent))
    }

    private fun ecoTraceResourcesLinks(data: MutableMap<String, String>, linkType: String,
                                       chooser: LinearLayout, dropdown: Spinner, applyBtn: ImageButton,
                                       callback: (String) -> Unit) {
        if (data.isEmpty()) {
            Toast.makeText(this, "Вы не состоите ни в одной из этих активностей!", Toast.LENGTH_SHORT).show()
            return
        }
        chooser.visibility = View.VISIBLE
        val array = data.values.toTypedArray()
        val adapter = ArrayAdapter(applicationContext, R.layout.widget_custom_spinner_item, array)
        adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
        dropdown.adapter = adapter

        applyBtn.setOnClickListener { _ ->
            val groupIdInArray = dropdown.selectedItemId
            val groupId = data.keys.toTypedArray()[groupIdInArray.toInt()]
            val groupName = data.values.toTypedArray()[groupIdInArray.toInt()]

            val linkClass = DatabaseMethods.DataClasses.EcoTraceLinkResource(
                "${linkType}s/$groupId",
                groupIdInArray.toInt(),
                groupName,
                groupName
            )
            links[links.size+1] = linkClass

            callback("@link${links.size}#${linkType}($groupName)")
        }
    }

    private fun reApplyLink() {

    }

    private fun removeLink() {
        
    }

    private fun addLinks(data: MutableMap<String, String>, linkType: String, linkDropdown: Spinner,
                         dropdown: Spinner,
                         applyBtn: ImageButton, deleteBtn: ImageButton,
                         description: EditText) {
        val arrayLinks = links.filter { it.value.resourceObject.contains(linkType) }
            .map{ "Ссылка ${it.key}" }
        if (arrayLinks.isEmpty()) {
            Toast.makeText(applicationContext, "Ссылки не найдены!", Toast.LENGTH_SHORT).show()
            return
        }
        val array = data.values.toTypedArray()
        val adapterLinks = ArrayAdapter(applicationContext, R.layout.widget_custom_spinner_item, arrayLinks)
        val adapter = ArrayAdapter(applicationContext, R.layout.widget_custom_spinner_item, array)
        adapterLinks.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
        adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)

        linkDropdown.adapter = adapterLinks
        dropdown.adapter = adapter

        linkDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLink = links[Regex("\\d+").find(arrayLinks[position])?.value?.toInt()]
                selectedLink?.let {
                    dropdown.setSelection(it.resourceIdInList)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        applyBtn.setOnClickListener { _ ->

            reApplyLink()
        }
        deleteBtn.setOnClickListener { _ ->


            removeLink()
        }
        description.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.contains(Regex("[@#]+"))) {
                    val filteredString = s.replace(Regex("[@#]+"), "")
                    if (filteredString != s.toString()) {
                        description.setText(filteredString)
                        description.setSelection(filteredString.length)
                        Toast.makeText(applicationContext, "Нельзя использовать символы @ и #", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_group_post_menu)
        createPostViewModel = CreateGroupPostMenuViewModel(
            Repository(
                DatabaseMethods.UserDatabaseMethods(),
                DatabaseMethods.ApplicationDatabaseMethods()
            )
        )
        publish = findViewById(R.id.publishPost)

        GroupRepository.DataStorage().groupData.let {
            findViewById<TextView>(R.id.currentGroupName).text = it.groupName
        }
        imageLoadWithLoading(
            "users",
            currentUser,
            findViewById(R.id.userImage),
            R.drawable.baseline_person_24
        )


        val attachImg: LinearLayout = findViewById(R.id.attachImage)
        val postImg: ImageView = findViewById(R.id.postImage)

        attachImg.setOnClickListener {
            showImageSourceDialog()
            reHasContent()
        }

        imageAttachedF = fun() {
            publish.setBackgroundColor(
                ContextCompat.getColor(
                    applicationContext,
                    if (hasImg) R.color.ok_green else R.color.silver
                )
            )
//            findViewById<ImageButton>(R.id.removeAttachedImage).visibility =
//                if (hasImg) View.VISIBLE else View.GONE
            findViewById<ImageView>(R.id.postImage).visibility =
                if (hasImg) View.VISIBLE else View.GONE

        }
//        findViewById<ImageButton>(R.id.removeAttachedImage).setOnClickListener {
//            imageAttached = false
//            imageAttachedF()
//        }

        val postContent: EditText = findViewById(R.id.postTextContent)
        var setTextProgram = false
        postContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(stringBefore: CharSequence?, changeStart: Int, previousCount: Int, newCount: Int) {
                Log.d("TextWatcher", "$stringBefore $changeStart $previousCount $newCount")
                if (newCount - previousCount < 0  && !setTextProgram) {
                    setTextProgram = true
                    var delTextPos = Pair(changeStart, changeStart + previousCount)

                    for (linkType in arrayOf("group", "event")) {
                        val regex = Regex("@link\\d+#${linkType}([^)@#]+)")
                        val matches = regex.findAll(stringBefore.toString())

                        matches.forEach { match ->
                            Log.d("find test", match.value)
                            val linkId = Regex("\\d+").find(match.value)!!.value.toInt()
                            links.remove(linkId)
                            val startPos = match.range.first
                            val endPos = match.range.last + 1

                            if (delTextPos.first in startPos..endPos
                                || delTextPos.second in startPos..endPos)
                            {
                                if (startPos <= delTextPos.first) {
                                    delTextPos = Pair(startPos, delTextPos.second)
                                }
                                if (endPos >= delTextPos.second) {
                                    delTextPos = Pair(delTextPos.first, endPos)
                                }
                            }
                        }
                    }

                    postContent.setText(stringBefore.toString()
                        .removeRange(delTextPos.first, delTextPos.second))
                    postContent.setSelection(postContent.text.length)
                    setTextProgram = false
                }
            }

            override fun onTextChanged(s: CharSequence?, changeStart: Int, removedCount: Int, addedCount: Int) {}
        })


        val addLink: LinearLayout = findViewById(R.id.addLink)


        addLink.setOnClickListener {
            val linkLayout = layoutInflater.inflate(R.layout.layout_popup_add_link_to, null)
            val window =
                PopupWindow(linkLayout, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, true)
            window.showAtLocation(findViewById<RelativeLayout>(R.id.main), Gravity.BOTTOM, 0, 0)
            linkLayout.findViewById<TextView>(R.id.otherLinks).setOnClickListener {
                linkLayout.findViewById<LinearLayout>(R.id.linkPaster).visibility = View.VISIBLE
                linkLayout.findViewById<ImageButton>(R.id.applyLink).setOnClickListener {
                    val linkDest = linkLayout.findViewById<EditText>(R.id.pasteLink).text.toString()
                    if (linkDest.startsWith("http")) {
                        actualText = "${postContent.text ?: ""}[link web:$linkDest](Ссылка)"
                        postContent.setText(actualText)
                        window.dismiss()
                    } else {
                        Toast.makeText(
                            this,
                            "Не удалось определить введенный текст как ссылку!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            linkLayout.findViewById<TextView>(R.id.attachGroup).setOnClickListener {
                createPostViewModel.getUserGroups(currentUser) {
                    ecoTraceResourcesLinks(
                        mutableMapOf(),
                        "group",
                        linkLayout.findViewById(R.id.groupChooser),
                        linkLayout.findViewById(R.id.chooseFromGroups),
                        linkLayout.findViewById(R.id.applyGroup)
                    )
                    { text -> // placeholder
                        val previousText = postContent.text
                        postContent.setText("$previousText$text")

                        window.dismiss()
                    }
                }
            }

            linkLayout.findViewById<TextView>(R.id.attachEvent).setOnClickListener {
                createPostViewModel.getUserEvents(currentUser) {

                    ecoTraceResourcesLinks(
                        mutableMapOf(),
                        "event",
                        linkLayout.findViewById(R.id.eventChooser),
                        linkLayout.findViewById(R.id.chooseFromEvents),
                        linkLayout.findViewById(R.id.applyEvent)
                    )
                    { text -> // placeholder
                        val previousText = postContent.text
                        postContent.setText("$previousText$text")

                        window.dismiss()
                    }
                }
            }

            val editLinks: LinearLayout = findViewById(R.id.editLinks)

            editLinks.setOnClickListener {
                val editLayout = layoutInflater.inflate(R.layout.layout_popup_all_added_links, null)
                val windowEdit =
                    PopupWindow(editLayout, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, true)
                windowEdit.showAtLocation(findViewById<RelativeLayout>(R.id.main), Gravity.BOTTOM, 0, 0)
                editLayout.findViewById<TextView>(R.id.attachGroup).setOnClickListener {
                    editLayout.findViewById<LinearLayout>(R.id.groupChanger).visibility = View.VISIBLE
                    Log.d("ghf", "grjeigg")
                    createPostViewModel.getUserGroups(currentUser) {
                        addLinks(
                            mutableMapOf(), "group", editLayout.findViewById(R.id.chooseFromAddedGroups), editLayout.findViewById(R.id.chooseFromGroups),
                            editLayout.findViewById(R.id.applyGroup), editLayout.findViewById(R.id.removeAddedGroup), editLayout.findViewById(R.id.editTextText2))
                    }
                }

//                editLayout.findViewById<TextView>(R.id.attachEvent).setOnClickListener {
//                    createPostViewModel.getUserEvents(currentUser) {
//
//                        ecoTraceResourcesLinks(
//                            it,
//                            "event",
//                            linkLayout.findViewById(R.id.eventChooser),
//                            linkLayout.findViewById(R.id.chooseFromEvents),
//                            linkLayout.findViewById(R.id.applyEvent)
//                        )
//                        { text -> // placeholder
//                            val previousText = postContent.text
//                            postContent.setText("$previousText$text")
//
//                            window.dismiss()
//                        }
//                    }
//                }
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
                        hasImg = true
                        imageAttachedF()
                    }
                }
            }
        }
    }

    private fun addImageToLayout(imageBitmap: Bitmap) {
        findViewById<ImageView>(R.id.postImage).setImageBitmap(imageBitmap)
        attachedImage = imageBitmap
    }
}