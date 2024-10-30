package com.gy.ecotrace.ui.more.profile.change

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.ShowGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel
import com.yandex.mapkit.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ChangeProfile : AppCompatActivity() {

    val currentUser = ETAuth.getInstance().guid()


    private fun getFlagEmoji(countryCode: String): String {
        return try {
            val flag1 = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val flag2 = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            String(Character.toChars(flag1)) + String(Character.toChars(flag2))
        } catch (e: Exception) {
            "NONE"
        }
    }
    private fun getAllCountries(): HashMap<String, String> {
        val locales = Locale.getAvailableLocales()
        val countriesToTheirCodes = HashMap<String, String>()
        for (locale in locales) {
            val countryName = "${locale.displayCountry} ${getFlagEmoji(locale.country)}"
            val countryCode = locale.country
            countriesToTheirCodes[countryName] = countryCode
        }
        return countriesToTheirCodes
    }

    private lateinit var profileViewModel: ChangeProfileViewModel
    private lateinit var profileImage: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) { // todo BUGS
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_profile)
        val countriesToTheirCodes = getAllCountries()
        val toolbar: Toolbar = findViewById(R.id.toolbar2)
        val backIcon = ContextCompat.getDrawable(applicationContext, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(applicationContext, R.color.ok_green), PorterDuff.Mode.SRC_ATOP)
        toolbar.setNavigationIcon(backIcon)
        toolbar.setNavigationOnClickListener {
            profileViewModel.save {
                onBackPressed()
            }
        }

        profileViewModel = ViewModelProvider(this, ChangeProfileViewModelFactory(Repository(
            DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())))[ChangeProfileViewModel::class.java]


        profileViewModel.getPublic()
        profileImage = findViewById(R.id.profile_image_change_menu)
        val profileUsername = findViewById<TextInputEditText>(R.id.newUsername)
        val profileCountry = findViewById<Spinner>(R.id.newCountry_change_menu)
        val profileAbout = findViewById<TextInputEditText>(R.id.newAboutMe)

        profileImage.setOnClickListener {
            showImageSourceDialog()
        }


        val adapter = ArrayAdapter(applicationContext, R.layout.widget_custom_spinner_item, countriesToTheirCodes.keys.toList())
        adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
        profileCountry.adapter = adapter

        profileCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(selection: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val public = profileViewModel.classPublic
                Log.d("choosing", "${selection}, ${p1}, ${p2}, ${p3}")
                Log.d("New country", "${countriesToTheirCodes.keys.toList()[p2]}, ${countriesToTheirCodes.values.toList()[p2]}")
                public.country_code = countriesToTheirCodes.values.toList()[p2]
                profileViewModel.applyPublic(public)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        profileViewModel.public.observe(this, Observer{
            Glide.with(this)
                .load(profileViewModel.getImage(it.userId))
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(profileImage)

            profileUsername.setText(it.username)
            profileCountry.setSelection(countriesToTheirCodes.values.indexOf(it.country_code))
            profileAbout.setText(it.aboutMe)
        })

        val returnUsername = findViewById<ImageButton>(R.id.returnUsername)
        val usernameLayout = findViewById<TextInputLayout>(R.id.newUsernameLayout)
        profileUsername.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if (p0.toString() == profileViewModel.firstUsername) returnUsername.visibility = View.GONE
                else returnUsername.visibility = View.VISIBLE

                runnable?.let { handler.removeCallbacks(it) }

                runnable = Runnable {
                    if (profileUsername.isFocused) {
                        val public = profileViewModel.classPublic
                        profileViewModel.checkUsername(p0?.toString()) {
                            if (it || p0.toString() == profileViewModel.firstUsername) {
                                usernameLayout.boxStrokeColor = ContextCompat.getColor(applicationContext, R.color.ok_green)

                                public.username = p0.toString()
                                profileViewModel.applyPublic(public)
                            } else {
                                usernameLayout.boxStrokeColor = ContextCompat.getColor(applicationContext, R.color.red_no)
                            }
                        }
                    }
                }

                handler.postDelayed(runnable!!, 500)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        returnUsername.setOnClickListener {
            profileUsername.requestFocus()
            profileUsername.setText(profileViewModel.firstUsername)
        }

        val returnAboutMe = findViewById<ImageButton>(R.id.returnAboutMe)
        val maxLength = 256

        profileAbout.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if (p0.toString() == profileViewModel.firstAboutMe) { returnAboutMe.visibility = View.GONE }
                else returnAboutMe.visibility = View.VISIBLE
                val public = profileViewModel.classPublic
                if ((p0?.length ?: 0) <= maxLength) {
                    public.aboutMe = p0?.toString()
                    profileViewModel.applyPublic(public)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        returnAboutMe.setOnClickListener {
            profileAbout.setText(profileViewModel.firstAboutMe)
        }

        val filtersLayout: LinearLayout = findViewById(R.id.tagsLayout)
        val tagsColors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
        val allEventTags: Array<Pair<String, String>> = Globals.getInstance().getUserFilters()
        for (tag in allEventTags.indices) {
            val tagButton = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
            tagButton.text = allEventTags[tag].first
            tagButton.textSize = 18F
            tagButton.setTextColor(Color.parseColor(tagsColors[tag].second))
            tagButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.transparent))
            tagButton.rippleColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].second))
            tagButton.strokeColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))

            tagButton.setOnClickListener {
                val public = profileViewModel.classPublic
                val prevFilters = try {
                    public.filters!!.split(",").map { it.toInt() }.toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
                tagButton.isActivated = !tagButton.isActivated
                if (!tagButton.isActivated) {
                    tagButton
                        .setBackgroundColor(
                            ContextCompat
                                .getColor(applicationContext, R.color.transparent)
                        )
                    prevFilters.remove(tag)
                } else {
                    tagButton.setBackgroundColor(Color.parseColor(tagsColors[tag].first))
                    prevFilters.add(tag)
                    prevFilters.sort()
                }
                public.filters = prevFilters.joinToString { "," }
                profileViewModel.applyPublic(public)
            }

            filtersLayout.addView(tagButton)
        }


        profileViewModel.getPrivate()
        val profileFullname = findViewById<TextInputEditText>(R.id.newFullname)
        val profileEmail = findViewById<TextInputEditText>(R.id.newEmail)
        val profileEmailLayout = findViewById<TextInputLayout>(R.id.newEmailLayout)
        profileViewModel.private.observe(this, Observer {
            profileFullname.setText(it.fullname)
            profileEmailLayout.setHint(it.email)
        })
        profileFullname.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                val private = profileViewModel.classPrivate
                private.fullname = p0?.toString()
                profileViewModel.applyPrivate(private)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })


        profileViewModel.getRules()
        profileViewModel.rules.observe(this, Observer {
            for ((ruleString, ruleValue) in it) {
                val resId = resources.getIdentifier(ruleString, "id", packageName)
                val spinner = findViewById<Spinner>(resId)
                spinner.setSelection(ruleValue)

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(selection: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        val rules = profileViewModel.classRules
                        rules[ruleString] = p2
                        profileViewModel.applyRules(rules)
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                }
            }
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
        profileImage.setImageBitmap(imageBitmap)
        profileViewModel.profileImage = imageBitmap
    }

}