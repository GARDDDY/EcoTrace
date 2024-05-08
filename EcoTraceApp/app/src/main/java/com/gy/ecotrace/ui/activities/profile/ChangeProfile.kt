package com.gy.ecotrace.ui.activities.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException

class ChangeProfile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_profile)
        val currentUser = Globals.getInstance().getString("CurrentlyLogged")

        val profileImage : ImageButton = findViewById(R.id.profile_image_change_menu)
        val savePublic : Button = findViewById(R.id.save_public_info)

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