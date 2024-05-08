package com.gy.ecotrace

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine



class FirebaseMethods {

    class UserInfo(var username: String = "error-username", var fullname: String = "error-fullname", var gender: Int = 0, var country: String = "error-country", var aboutMe: String = "")
    class UserPrivate(var email: String = "error-email", var password: String = "error-password")
    class UserRules(var nameSeen: Int = 0 /* Все */, var countrySeen: Int = 0 /* Все */, var friendFrom: Int = 0 /* Все */)

    private lateinit var applicationContext: Context

    fun appCont(appContext: Context) {
        applicationContext = appContext
    }

    fun hash256(text: String) : String{
        val bytes = text.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    suspend fun getUserProfileImage(userId : String): Bitmap? {
        val image: StorageReference = FirebaseStorage.getInstance().reference.child("profile_images/$userId.png")
        val localFile = withContext(Dispatchers.IO) {
            File.createTempFile("${userId}-ProfileImage", "png")
        }
        return try {
            val task = image.getFile(localFile)
            Tasks.await(task)
            BitmapFactory.decodeFile(localFile.absolutePath)
        } catch (e: Exception) {
            Log.e("Image GET", "Unable to get, ID: $userId! $e")
            null
        }
    }

    suspend fun setUserProfileImage(userId: String, userImg: Bitmap): Boolean {
        val image: StorageReference = FirebaseStorage.getInstance().reference.child("profile_images/$userId.png")
        return try {
            val byteImg = ByteArrayOutputStream()
            userImg.compress(Bitmap.CompressFormat.PNG, 100, byteImg)
            val img = byteImg.toByteArray()
            val task = image.putBytes(img)
            Tasks.await(task)
            true
        } catch (e: Exception) {
            Log.e("Image SET", "Unable to upload! $e")
            false
        }
    }

    private fun getLocalInfo(userId: String): UserInfo? {
        val file = File(applicationContext.filesDir, "${userId}-@{info}.json")
        if (file.exists()) {
            val reader = BufferedReader(FileReader(file))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            reader.close()
            Log.e("Data", "Found")
            return Gson().fromJson(stringBuilder.toString(), UserInfo::class.java)
        }
        Log.e("Data", "Not found")
        return null
    }

    suspend fun getUserInfo(userId: String): UserInfo? {
        val localInfo = getLocalInfo(userId)
        if (localInfo != null) {
            Log.d("UserInfoClassGET", "Local!")
            return localInfo
        }
        return suspendCoroutine {
            val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId)
            database.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val data = snapshot.getValue(UserInfo::class.java)!!
                        Log.wtf("UserInfoClassGET", "Network!")
                        it.resume(data)
                        saveCache(data, "info", userId)
                    } else {
                        Log.wtf("Account info", "Error $userId")
                        it.resume(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    suspend fun getUserRules(userId: String){

    }

    suspend fun getUserPrivateInfo(userId: String, userPassword: String){

    }

    suspend fun isUser(username: String, email: String): Boolean{
        return suspendCoroutine {
            val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("indexes")
            database.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChild(username) || snapshot.hasChild(email.replace(".", "!"))) it.resume(true)
                    else it.resume(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    it.resume(true)
                }
            })
        }
    }

    private fun saveCache(data: Any, dataType: String, userId: String) {
        try {
            val directory = applicationContext.filesDir
            val file = File(directory, "${userId}-@{$dataType}.json")
            val jsonString = Gson().toJson(data)
            FileOutputStream(file).use { out ->
                out.write(jsonString.toByteArray())
            }
            Log.d("JSON", "JSON saved successfully")
        } catch (e: IOException) {
            Log.e("JSON", "Error saving JSON")
            e.printStackTrace()
        }
    }

    fun clearAllCache() {
        val dataTypes = arrayOf("info", "picture")
        val directory = applicationContext.filesDir
        for (dType in dataTypes) {
            val files = directory.listFiles { file ->
                file.name.contains("@{$dType}")
            }

            files?.forEach { file ->
                file.delete()
            }
        }
    }
}