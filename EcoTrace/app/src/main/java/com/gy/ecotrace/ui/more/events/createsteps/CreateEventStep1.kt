package com.gy.ecotrace.ui.more.events.createsteps

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
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods

class CreateEventStep1: Fragment() {

    private val sharedViewModel: CreateEventViewModel by activityViewModels()
    private lateinit var eventImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_create_event_step1, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.eventData.observe(viewLifecycleOwner, Observer { eventClass ->

            val eventNameEntry: EditText = view.findViewById(R.id.createEventName)
            val eventAboutEntry: EditText = view.findViewById(R.id.createEventAbout)

            val filtersLayout: LinearLayout = view.findViewById(R.id.chosenTags)
            eventImage = view.findViewById<ImageView>(R.id.eventImage)
            eventImage.setOnClickListener {
                showImageSourceDialog()
            }

            if (eventClass.eventId != "0") {
                Glide.with(this@CreateEventStep1)
                    .load(
                        DatabaseMethods.ApplicationDatabaseMethods()
                            .getImageLink("events", eventClass.eventId ?: "0")
                    ).placeholder(R.color.silver)
                    .into(eventImage)
            }

            eventNameEntry.setText(eventClass.eventName)
            eventAboutEntry.setText(eventClass.eventAbout)

            eventNameEntry.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().length > 5) {
                        eventNameEntry.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
//                        sharedViewModel.applyEventData(eventName = s.toString())
                        eventClass.eventName = s.toString()
                    } else {
                        eventNameEntry.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0,
                            R.drawable.baseline_do_not_disturb_24, 0
                        )
                        eventClass.eventName = ""
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            eventAboutEntry.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable?) {
                    eventClass.eventAbout = s.toString()
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            val usedTags = try {
                eventClass.filters.split(',').map { it.toInt()-1 }.toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            val tags: Array<Pair<String, String>> = Globals.getInstance().getEventsFilters()
            val colors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
            for (dat in tags.indices) {
                val layout = LinearLayout(context)
                layout.orientation = LinearLayout.HORIZONTAL
                val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layout.layoutParams = layoutParams

                val isActive = usedTags.contains(dat)

                val filter = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
                filter.text = tags[dat].first
                filter.textSize = 18F
                filter.setTextColor(Color.parseColor(colors[dat].second))
                filter.setBackgroundColor(
                    if (isActive)
                        Color.parseColor(colors[dat].first)
                    else ContextCompat.getColor(requireContext(),R.color.transparent)
                )
                filter.isActivated = isActive
                filter.rippleColor = ColorStateList.valueOf(Color.parseColor(colors[dat].second))
                filter.isClickable = true
                layout.addView(filter)

                filter.setOnClickListener {
                    filter.isActivated = !filter.isActivated
                    if(!filter.isActivated) filter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
                    else filter.setBackgroundColor(Color.parseColor(colors[dat].first))
                }



                filtersLayout.addView((layout))

            }

            eventClass.eventCountMembers = 1
        })


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
        eventImage.setImageBitmap(imageBitmap)
        sharedViewModel.eventImage = imageBitmap
    }

}