package com.gy.ecotrace.ui.more.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.activities.profile.ProfileActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class UsersSearchFriends : AppCompatActivity() {
    private suspend fun getUsersStartingWith(expression: String): List<Pair<String, String>> {
        return try {
            withContext(Dispatchers.IO) {
                val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
                val usersStartingWith = mutableListOf<Pair<String, String>>()

                val snapshot = database.get().await()
                for (userSnap in snapshot.children) {
                    val username = userSnap.child("username").getValue(String::class.java)
                    val userid = userSnap.child("id").getValue(String::class.java)
                    if (!username.isNullOrEmpty() && userid != null) {
                        if (expression.length == 1 && username.startsWith(expression, ignoreCase = true)) {
                            usersStartingWith.add(Pair(userid, username))
                        } else if (username.startsWith(expression, ignoreCase = true)) {
                            usersStartingWith.add(Pair(userid, username))
                        }
                    }
                }
                return@withContext usersStartingWith
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun caesarEnc(text: String, shift: Int = 12): String {
        val result = StringBuilder()

        for (char in text) {
            if (char.isLetter()) {
                val start = if (char.isUpperCase()) 'A' else 'a'
                val shifted = (start.toInt() + (char - start + shift) % 26).toChar()
                result.append(shifted)
            } else {
                result.append(char)
            }
        }

        return result.toString()
    }
    fun caesarDec(text: String, shift: Int = 12): String {
        return caesarEnc(text, 26 - shift)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_users_search_friends)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val localSettings = getSharedPreferences("localValues", MODE_PRIVATE)
        val currentUser = localSettings.getString("loggedId", "0")
        val globals = Globals.getInstance()
        val searcher : EditText = findViewById(R.id.search_user_by_username)
        val foundSection : LinearLayout = findViewById(R.id.found_friends)

        val data: Uri? = intent.data
        if (data != null && data.isHierarchical && data.queryParameterNames.isNotEmpty()) {
            val userId: String? = data.getQueryParameter("id")
            if (userId != null) {
                val userid = caesarDec(userId)
                if (userid != currentUser) globals.setString("CurrentWatching", userid)
                else if (currentUser == "0") return
                val gotoProfile = Intent(this@UsersSearchFriends, ProfileActivity::class.java)
                this@UsersSearchFriends.startActivity(gotoProfile)
            }
        }

        if (currentUser != "0") {
            val personalInvLink : TextView = findViewById(R.id.personal_invite_link)
            val link = "https://ecotrace/addFriend?id=${caesarEnc(currentUser!!)}"
            personalInvLink.text = link
            personalInvLink.isClickable = true
            personalInvLink.setOnClickListener {
                val clipboard : ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Ссылка скопирована", link)
                clipboard.setPrimaryClip(clip)
            }
        }
        var searchRequest : Job? = null
        searcher.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                foundSection.removeAllViews()
                val text = s.toString()
                if (text != "") {
                    searchRequest?.cancel() // Отменяем предыдущий запрос

                    searchRequest = CoroutineScope(Dispatchers.Main).launch {
                        delay(300) // Даем небольшую задержку перед выполнением запроса
                        val text = s.toString()
                        if (text.isNotEmpty()) {
                            val users = getUsersStartingWith(text)
                            for (user in users) {
                                val userLL = layoutInflater.inflate(R.layout.friend_linear_layout, null)
                                userLL.findViewById<TextView>(R.id.username_friend_layout).text = user.second
                                val userImgBtn : ImageButton = userLL.findViewById(R.id.user_img_friend_layout)

                                userImgBtn.setOnClickListener {
                                    if (user.first == currentUser || currentUser == "0"){

                                    }
                                    else {
                                        globals.setString("CurrentWatching", user.first)
                                        val gotoProfile = Intent(this@UsersSearchFriends, ProfileActivity::class.java)
                                        this@UsersSearchFriends.startActivity(gotoProfile)
                                    }
                                }
                                foundSection.addView(userLL)
                            }
                        }
                    }
                }
            }
        })
    }
}