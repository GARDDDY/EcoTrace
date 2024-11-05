package com.gy.ecotrace.ui.more.groups.createsteps

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.CreateGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel

class CreateGroupStep1 : Fragment() {

    private lateinit var sharedViewModel: CreateGroupViewModel

    private lateinit var groupImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedViewModel = ViewModelProvider(requireActivity(), CreateGroupViewModelFactory(
            Repository(
                DatabaseMethods.UserDatabaseMethods(),
                DatabaseMethods.ApplicationDatabaseMethods()
            )
        )
        )[CreateGroupViewModel::class.java]
        return inflater.inflate(R.layout.activitylayout_create_group_step1, container, false)
    }

    private fun isGoodName(name: String, callback: (Boolean) -> Unit) {
        sharedViewModel.isGroupNameAvailable(name) {
            if (it != null) {
                Toast.makeText(requireActivity(), it, Toast.LENGTH_SHORT).show()
                callback(false)
            } else callback(true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")

        val groupName: EditText = view.findViewById(R.id.groupName)
        groupName.setText(sharedViewModel.groupClass.groupName)
        groupName.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if (p0.toString().length < 5) {
                    return
                }
                isGoodName(p0.toString()) {
                    val group = sharedViewModel.groupClass
                    group.groupName = if (it) p0.toString() else ""
                    groupName.backgroundTintList = ContextCompat.getColorStateList(requireContext(), if (it) R.color.ok_green else R.color.red_no)
                    sharedViewModel.applyGroupData(group)
                }
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        val groupAbout: EditText = view.findViewById(R.id.groupAbout)
        groupAbout.setText(sharedViewModel.groupClass.groupAbout)
        groupAbout.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                val group = sharedViewModel.groupClass
                group.groupAbout = p0?.toString()
                sharedViewModel.applyGroupData(group)
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        val groupTags: LinearLayout = view.findViewById(R.id.groupTags)
        val activeTags = try{sharedViewModel.groupClass.filters!!.split(',').map { it.toInt() }.toMutableList()} catch (e: Exception) {
            emptyList()}

        val tags: Array<Pair<String, String>> = Globals.getInstance().getGroupsFilters()
        val colors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
        for (dat in tags.indices) {
            val filter = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
            filter.text = tags[dat].first
            filter.textSize = 18F
            filter.setTextColor(Color.parseColor(colors[dat].second))

            val isActive = activeTags.contains(dat+1)

            filter.setBackgroundColor(
                if (isActive)
                    Color.parseColor(colors[dat].first)
                else ContextCompat.getColor(requireContext(),R.color.transparent)
            )
            filter.isActivated = isActive
            filter.rippleColor = ColorStateList.valueOf(Color.parseColor(colors[dat].second))
            filter.isClickable = true

            filter.setOnClickListener {
                filter.isActivated = !filter.isActivated
                val group = sharedViewModel.groupClass
                val groupFilters = try{group.filters!!.split(',').map { it.toInt() }.toMutableList()} catch (e: Exception) { emptyList<Int>() }.toMutableList()

                if(!filter.isActivated) {
                    filter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
                    groupFilters.remove(dat+1)
                }
                else {
                    filter.setBackgroundColor(Color.parseColor(colors[dat].first))
                    groupFilters.add(dat+1)
                }
                groupFilters.sorted()
                group.filters = groupFilters.sorted().joinToString(",")

                sharedViewModel.applyGroupData(group)
            }

            groupTags.addView(filter)

        }

        val groupType = view.findViewById<Spinner>(R.id.groupType)

        groupType.setSelection(sharedViewModel.groupClass.groupType ?: 0)
        groupType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val group = sharedViewModel.groupClass
                group.groupType = position
                sharedViewModel.applyGroupData(group)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        groupImage = view.findViewById(R.id.groupImage)
        if (currentGroup != "0") {
            Glide.with(this@CreateGroupStep1)
                .load(
                    DatabaseMethods.ApplicationDatabaseMethods()
                        .getImageLink("groups", currentGroup)
                )
                .into(groupImage)
        }
        groupImage.setOnClickListener {
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
                                requireActivity().contentResolver.openInputStream(
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
        groupImage.setImageBitmap(imageBitmap)
        sharedViewModel.groupImage = imageBitmap
    }
}