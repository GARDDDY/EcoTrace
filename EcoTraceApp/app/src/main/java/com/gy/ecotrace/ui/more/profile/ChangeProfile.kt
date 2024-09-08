package com.gy.ecotrace.ui.more.profile

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ChangeProfile : AppCompatActivity() {

    private var countriesToTheirCodes = HashMap<String, String>()
    private fun getAllCountries() {
        val locales = Locale.getAvailableLocales()
        for (locale in locales) {
            val countryName = locale.displayCountry
            val countryCode = locale.country
            countriesToTheirCodes[countryName] = countryCode
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_profile)
        getAllCountries()
        val toolbar: Toolbar = findViewById(R.id.toolbar2)
        val backIcon = ContextCompat.getDrawable(applicationContext, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(applicationContext, R.color.ok_green), PorterDuff.Mode.SRC_ATOP)
        toolbar.setNavigationIcon(backIcon)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val currentUser = Globals.getInstance().getString("CurrentlyLogged")
        val profileImage : ImageButton = findViewById(R.id.profile_image_change_menu)
        Glide.with(this)
            .load(Globals().getImgUrl("users", currentUser))
            .placeholder(R.drawable.baseline_person_24)
            .into(profileImage)
        val usernameChange: EditText = findViewById(R.id.newUsername_change_menu)
        val realnameChnage: EditText = findViewById(R.id.newFullname_change_menu)
        val countryChange: Spinner = findViewById(R.id.newCountry_change_menu)
        val aboutmeChange: EditText = findViewById(R.id.aboutme_change_menu)
        lifecycleScope.launch {
            val userData = withContext(Dispatchers.IO) { DatabaseMethods.UserDatabaseMethods().getUserInfo(currentUser) }
            usernameChange.setText(userData?.username ?: "none")
            realnameChnage.setText(userData?.fullname ?: "none")
            aboutmeChange.setText(userData?.aboutMe ?: "")

            countryChange.setSelection(countriesToTheirCodes.keys.toList().sorted().indexOf(userData?.country!!.name))
            val adapter = ArrayAdapter(applicationContext, R.layout.widget_custom_spinner_item, countriesToTheirCodes.keys.toList().sorted())
            adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
            countryChange.adapter = adapter
        }





        val savePublic : Button = findViewById(R.id.save_public_info)
//        val f = DatabaseMethods()
//        lifecycleScope.launch {
//            f.appCont(applicationContext)
//            val img = withContext(Dispatchers.IO){ f.getUserProfileImage(currentUser) }
//            if (img != null) {
//                profileImage.setImageBitmap(img)
//            }
//            val myData: DatabaseMethods.UserInfo = withContext(Dispatchers.IO) { f.getUserInfo(currentUser)!! }
//            usernameChange.setHint(myData.username)
//            realnameChnage.setHint(myData.fullname)
//            Log.e("About me", myData.aboutMe)
//            aboutmeChange.setText(myData.aboutMe)
//
//
//            countryChange.setSelection(countriesToTheirCodes.keys.toList().sorted().indexOf(myData.country.name))
//            val adapter = ArrayAdapter(applicationContext, R.layout.widget_custom_spinner_item, countriesToTheirCodes.keys.toList().sorted())
//            adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
//            countryChange.adapter = adapter
//
//
//            findViewById<ProgressBar>(R.id.progressbar_loading_change_menu).visibility = View.GONE
//            findViewById<ScrollView>(R.id.scrollviewmain_change_menu).visibility = View.VISIBLE
//
//        }


//        val userData = getUserLocalData(currentUser)
//        if (userData != null){
//            val currentImg : Bitmap? = getUserLocalImage(currentUser)
//            if (currentImg != null){
//                profileImage.setImageBitmap(currentImg)
//            }
//        }
//        else {
//
//        }

//        savePublic.setOnClickListener {
//
//            val newUsername : EditText = findViewById(R.id.newUsername_change_menu)
//            val newFullname : EditText = findViewById(R.id.newFullname_change_menu)
//            val newEmail : EditText = findViewById(R.id.newEmail_change_menu)
//            checkForUser(newUsername.text.toString(), newEmail.text.toString()){
//                if (it != 0){
//                    // 0 все занято  1 свободен только никнейм  2 свободна только почта    3 свободно все
//                    userData!!.username = if (newUsername.text.toString().length >= 5) newUsername.text.toString() else userData.username
//                    userData.fullname = if (newFullname.text.toString().length >= 2) newFullname.text.toString() else userData.fullname
//                    userData.email = if (newEmail.text.toString().length >= 8 && newEmail.text.toString().contains("@")) newEmail.text.toString() else userData.email
//
//                    saveUserLocalData(currentUser, userData)
//                    saveDataOnServer(currentUser, userData)
//                }
//            }
//
//        }

    }

}