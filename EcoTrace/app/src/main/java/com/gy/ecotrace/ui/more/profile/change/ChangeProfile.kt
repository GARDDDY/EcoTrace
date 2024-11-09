package com.gy.ecotrace.ui.more.profile.change

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.util.Locale

@AndroidEntryPoint
class ChangeProfile : AppCompatActivity() {

    private val profileViewModel: ChangeProfileViewModel by viewModels()
    private lateinit var profileImage: ImageView
    private lateinit var saveBar: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    private fun getFlagEmoji(countryCode: String?): String {
        return try {
            val flag1 = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val flag2 = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            String(Character.toChars(flag1)) + String(Character.toChars(flag2))
        } catch (e: Exception) {
            "🏳"
        }
    }

    private fun getAllCountries(): LinkedHashMap<String, String> {
        val locales = Locale.getAvailableLocales()
        val countriesToTheirCodes = HashMap<String, String>()
        for (locale in locales) {
            val countryName = "${locale.displayCountry} ${getFlagEmoji(locale.country)}"
            val countryCode = locale.country
            if (countryName.isNotEmpty() && countryCode.isNotEmpty()) {
                countriesToTheirCodes[countryName] = countryCode
            }
        }
        return countriesToTheirCodes.toList()
            .sortedBy { (key, _) -> key }
            .toMap(LinkedHashMap())
    }

    private fun visibilityToPos(v: String?) = when (v) {
        "everyone" -> 0
        "friends" -> 1
        "nobody" -> 2
        else -> 0
    }

    private fun posToVisibility(pos: Int) = when (pos) {
        0 -> "everyone"
        1 -> "friends"
        else -> "nobody"
    }

    private fun updateLockIcon(icon: ImageView, pos: Int) {
        icon.setImageResource(if (pos == 2) R.drawable.round_lock_24 else R.drawable.round_lock_open_24)
    }

    private fun setupPrivacySpinner(spinnerId: Int, lockIconId: Int, current: String?, onPick: (String) -> Unit) {
        val spinner = findViewById<Spinner>(spinnerId)
        val lockIcon = findViewById<ImageView>(lockIconId)
        val adapter = ArrayAdapter(
            applicationContext,
            R.layout.widget_custom_spinner_item,
            resources.getStringArray(R.array.whoCanSeeFullname)
        )
        adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
        spinner.adapter = adapter
        val startPos = visibilityToPos(current)
        spinner.setSelection(startPos)
        updateLockIcon(lockIcon, startPos)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                updateLockIcon(lockIcon, pos)
                onPick(posToVisibility(pos))
                updateSaveBarVisibility()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun updateSaveBarVisibility() {
        saveBar.visibility = if (profileViewModel.isDirty()) View.VISIBLE else View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_profile)
        val countriesToTheirCodes = getAllCountries()

        val toolbar: Toolbar = findViewById(R.id.toolbar2)
        val backIcon = ContextCompat.getDrawable(applicationContext, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(
            ContextCompat.getColor(applicationContext, R.color.ok_green),
            PorterDuff.Mode.SRC_ATOP
        )
        toolbar.setNavigationIcon(backIcon)
        toolbar.setNavigationOnClickListener { attemptSaveAndExit() }

        saveBar = findViewById(R.id.btnSaveChanges)
        saveBar.setOnClickListener {
            profileViewModel.save { ok ->
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        if (ok) getString(R.string.changesSaved) else getString(R.string.failedToSaveChanges),
                        Toast.LENGTH_SHORT
                    ).show()
                    updateSaveBarVisibility()
                }
            }
        }

        profileImage = findViewById(R.id.profile_image_change_menu)
        val profileUsername = findViewById<TextInputEditText>(R.id.newUsername)
        val profileCountry = findViewById<AutoCompleteTextView>(R.id.newCountry_change_menu)
        val countryLayout = findViewById<TextInputLayout>(R.id.newCountryLayout)
        val profileAbout = findViewById<TextInputEditText>(R.id.newAboutMe)
        val profileFullname = findViewById<TextInputEditText>(R.id.newFullname)
        val profileEmail = findViewById<TextInputEditText>(R.id.newEmail)
        val profileEmailLayout = findViewById<TextInputLayout>(R.id.newEmailLayout)
        val usernameLayout = findViewById<TextInputLayout>(R.id.newUsernameLayout)
        val returnUsername = findViewById<ImageButton>(R.id.returnUsername)
        val returnAboutMe = findViewById<ImageButton>(R.id.returnAboutMe)

        findViewById<View>(R.id.avatarEditBadge).setOnClickListener { showImageSourceDialog() }
        profileImage.setOnClickListener { showImageSourceDialog() }

        // Стандартный ArrayAdapter фильтрует только по началу строки — подключаем свой Filter для поиска по подстроке.
        val countryAdapter = object : ArrayAdapter<String>(
            applicationContext,
            R.layout.widget_custom_spinner_dropdown_item,
            countriesToTheirCodes.keys.toList()
        ) {
            private val allNames = countriesToTheirCodes.keys.toList()
            override fun getFilter() = object : android.widget.Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.trim().orEmpty()
                    val matches = if (query.isEmpty()) allNames
                    else allNames.filter { it.contains(query, ignoreCase = true) }
                    return FilterResults().apply { values = matches; count = matches.size }
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    clear()
                    addAll(results?.values as? List<String> ?: emptyList())
                    notifyDataSetChanged()
                }
            }
        }
        profileCountry.threshold = 1
        profileCountry.setAdapter(countryAdapter)
        profileCountry.setOnItemClickListener { parent, _, pos, _ ->
            val name = parent.getItemAtPosition(pos) as String
            val code = countriesToTheirCodes[name]
            profileViewModel.draft.countryCode = code
            countryLayout.prefixText = getFlagEmoji(code)
            updateSaveBarVisibility()
        }

        var uiPopulated = false
        profileViewModel.profile.observe(this, Observer { user ->
            if (user == null || uiPopulated) return@Observer
            uiPopulated = true

            Glide.with(this)
                .load(user.avatarKey?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" })
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(profileImage)

            profileUsername.setText(user.username)
            val countryName = countriesToTheirCodes.entries.firstOrNull { it.value == user.countryCode }?.key
            profileCountry.setText(countryName, false)
            countryLayout.prefixText = getFlagEmoji(user.countryCode)
            profileAbout.setText(user.aboutMe)
            profileFullname.setText(user.fullname)
            profileEmailLayout.hint = user.email

            val chipGroup: ChipGroup = findViewById(R.id.chipGroupInterests)
            val tagsColors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
            val allUserTags: Array<Pair<String, String>> = Globals.getInstance().getUserFilters()
            for (tag in allUserTags.indices) {
                val chip = Chip(this)
                chip.text = allUserTags[tag].first
                chip.isCheckable = true
                chip.isCheckedIconVisible = false
                chip.chipStrokeWidth = 2f
                chip.setTextColor(Color.parseColor(tagsColors[tag].second))
                chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))
                chip.rippleColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].second))
                val isSelected = profileViewModel.draft.filters.contains(tag + 1)
                chip.isChecked = isSelected
                chip.chipBackgroundColor = ColorStateList.valueOf(
                    if (isSelected) Color.parseColor(tagsColors[tag].first)
                    else ContextCompat.getColor(applicationContext, R.color.transparent)
                )
                chip.setOnClickListener {
                    val filters = profileViewModel.draft.filters
                    if (!chip.isChecked) {
                        chip.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(applicationContext, R.color.transparent)
                        )
                        filters.remove(tag + 1)
                    } else {
                        chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))
                        filters.add(tag + 1)
                        filters.sort()
                    }
                    updateSaveBarVisibility()
                }
                chipGroup.addView(chip)
            }

            setupPrivacySpinner(R.id.canSeeName, R.id.lockName, user.nameVisibility) {
                profileViewModel.draft.nameVisibility = it
            }
            setupPrivacySpinner(R.id.canSeeCountry, R.id.lockCountry, user.countryVisibility) {
                profileViewModel.draft.countryVisibility = it
            }
            setupPrivacySpinner(R.id.canSeeFriends, R.id.lockFriends, user.friendsVisibility) {
                profileViewModel.draft.friendsVisibility = it
            }
            setupPrivacySpinner(R.id.canSeeGroups, R.id.lockGroups, user.groupsVisibility) {
                profileViewModel.draft.groupsVisibility = it
            }

            updateSaveBarVisibility()
        })

        profileUsername.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                returnUsername.visibility =
                    if (p0.toString() == profileViewModel.firstUsername) View.GONE else View.VISIBLE

                runnable?.let { handler.removeCallbacks(it) }
                runnable = Runnable {
                    if (profileUsername.isFocused) {
                        profileViewModel.checkUsername(p0?.toString()) { available ->
                            if (available || p0.toString() == profileViewModel.firstUsername) {
                                usernameLayout.boxStrokeColor =
                                    ContextCompat.getColor(applicationContext, R.color.ok_green)
                                usernameLayout.helperText =
                                    if (p0.toString() == profileViewModel.firstUsername) null
                                    else getString(R.string.usernameAvailable)
                                usernameLayout.setHelperTextColor(
                                    ContextCompat.getColorStateList(applicationContext, R.color.ok_green)
                                )
                                profileViewModel.draft.username = p0.toString()
                            } else {
                                usernameLayout.boxStrokeColor =
                                    ContextCompat.getColor(applicationContext, R.color.red_no)
                                usernameLayout.helperText = getString(R.string.usernameTaken)
                                usernameLayout.setHelperTextColor(
                                    ContextCompat.getColorStateList(applicationContext, R.color.red_no)
                                )
                            }
                            updateSaveBarVisibility()
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

        val maxLength = 256
        profileAbout.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                returnAboutMe.visibility =
                    if (p0.toString() == profileViewModel.firstAboutMe) View.GONE else View.VISIBLE
                if ((p0?.length ?: 0) <= maxLength) {
                    profileViewModel.draft.aboutMe = p0?.toString()
                }
                updateSaveBarVisibility()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        returnAboutMe.setOnClickListener {
            profileAbout.setText(profileViewModel.firstAboutMe)
        }

        profileFullname.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                profileViewModel.draft.fullname = p0?.toString()
                updateSaveBarVisibility()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        val applyEmail = findViewById<MaterialButton>(R.id.applyEmail)
        profileEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                applyEmail.visibility = if ((p0?.length ?: 0) > 3) View.VISIBLE else View.GONE
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        applyEmail.setOnClickListener {
            val newEmail = profileEmail.text.toString()
            val passwordInput = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                hint = "Текущий пароль"
            }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Смена почты")
                .setMessage("Подтвердите смену на $newEmail текущим паролем")
                .setView(passwordInput)
                .setPositiveButton("Подтвердить") { _, _ ->
                    ETAuth.getInstance().changeEmail(newEmail, passwordInput.text.toString()) { ok ->
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                if (ok) "Почта изменена!" else "Не удалось изменить почту",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (ok) {
                                profileEmailLayout.hint = "Email"
                                profileEmail.setText("")
                            }
                        }
                    }
                }
                .setNegativeButton("Отмена") { d, _ -> d.dismiss() }
                .show()
        }

        val currentPassword = findViewById<TextInputEditText>(R.id.currentPassword)
        val newPassword = findViewById<TextInputEditText>(R.id.newPassword)
        val applyPassword = findViewById<MaterialButton>(R.id.applyNewPassword)

        val passwordWatcher = object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                applyPassword.visibility =
                    if (currentPassword.text.toString().length > 3 && newPassword.text.toString().length > 3)
                        View.VISIBLE else View.GONE
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        }
        currentPassword.addTextChangedListener(passwordWatcher)
        newPassword.addTextChangedListener(passwordWatcher)
        applyPassword.setOnClickListener {
            ETAuth.getInstance()
                .changePassword(currentPassword.text.toString(), newPassword.text.toString()) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            if (it) "Пароль успешно изменен!" else "Не удалось изменить пароль!",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (it) newPassword.setText("")
                        currentPassword.setText("")
                        applyPassword.visibility = View.GONE
                    }
                }
        }

        profileViewModel.load()
    }

    private fun attemptSaveAndExit() {
        if (profileViewModel.isDirty()) {
            profileViewModel.save { onBackPressedDispatcher.onBackPressed() }
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showImageSourceDialog() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val galleryIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        val chooserIntent = Intent.createChooser(galleryIntent, "Добавьте изображение, используя")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        startActivityForResult(chooserIntent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            data?.let {
                val extras = it.extras
                if (extras != null && extras.containsKey("data")) {
                    addImageToLayout(extras.get("data") as Bitmap)
                } else {
                    val inputStream = it.data?.let { uri -> contentResolver.openInputStream(uri) }
                    addImageToLayout(BitmapFactory.decodeStream(inputStream))
                }
            }
        }
    }

    private fun addImageToLayout(imageBitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        profileViewModel.avatarPng = stream.toByteArray()
        profileImage.setImageBitmap(imageBitmap)
        updateSaveBarVisibility()
        Toast.makeText(applicationContext, "Аватар применится после сохранения", Toast.LENGTH_SHORT).show()
    }
}
