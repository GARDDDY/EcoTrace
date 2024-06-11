package com.gy.ecotrace.ui.more.friends

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Highlights
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import kotlinx.coroutines.delay


class SearcherViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearcherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearcherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class UsersSearchFriends : AppCompatActivity() {
    private lateinit var searcherViewModel: SearcherViewModel
    private var loggedUser = Globals.getInstance().getString("CurrentlyLogged")

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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_users_search_friends)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar: Toolbar = findViewById(R.id.toolbar5)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        if (loggedUser == "0") {
            val builder = AlertDialog.Builder(applicationContext)
            builder.setTitle("Требуется вход в аккаунт")

            builder.setMessage("Для использования этого раздела надо войти в свой аккаунт или создать новый")
            builder.setPositiveButton("Подтвердить") { dialog, which ->
                startActivity(Intent(this@UsersSearchFriends, ProfileActivity::class.java))
            }
            val dialog = builder.create()
            dialog.show()
            finish()
        }


        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())

        val factory = SearcherViewModelFactory(repository)
        searcherViewModel = ViewModelProvider(this, factory)[SearcherViewModel::class.java]

        // открытие профиля по ссылке
        val data: Uri? = intent.data
        if (data != null && data.isHierarchical && data.queryParameterNames.isNotEmpty()) {
            val userId: String? = data.getQueryParameter("id")
            if (userId != null) {
                val userid = caesarDec(userId)
                if (userid != loggedUser) Globals.getInstance().setString("CurrentWatching", userid)
                else if (loggedUser == "0") return
                val gotoProfile = Intent(this@UsersSearchFriends, ProfileActivity::class.java)
                this@UsersSearchFriends.startActivity(gotoProfile)
            }
        }

        val personalInvLink : TextView = findViewById(R.id.personal_invite_link)
        val link = "https://ecotrace/addFriend?id=${caesarEnc(loggedUser)}"
        personalInvLink.text = link
        personalInvLink.isClickable = true
        personalInvLink.setOnClickListener {
            val clipboard : ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Ссылка скопирована", link)
            clipboard.setPrimaryClip(clip)
        }
        searcherViewModel.getUserFriends(loggedUser)
        searcherViewModel.filteredUserFriends.observe(this, Observer {
            val friendsLayout: LinearLayout =
                findViewById(R.id.currentLoggedUserFriendsLayoutTo)
            friendsLayout.removeAllViews()
            it?.let{
                if (it.size > 0) {

                    for (fr in it) {
                        val friendOneLayout =
                            layoutInflater.inflate(R.layout.friend_linear_layout, null)
                        friendOneLayout.findViewById<TextView>(R.id.username_friend_layout).text =
                            fr.username

                        friendOneLayout.setOnClickListener {
                            val myIntent = Intent(this, ProfileActivity::class.java)
                            myIntent.putExtra("previousId", loggedUser)
                            Globals.getInstance().setString("CurrentlyWatching", fr.userId)
                            this.startActivity(myIntent)
                        }

                        Glide.with(this)
                            .load(Globals().getImgUrl("users", "${fr.userId}"))
                            .into(friendOneLayout.findViewById(R.id.user_img_friend_layout))

                        friendsLayout.addView(friendOneLayout)
                    }
                }


            }
            findViewById<LinearLayout>(R.id.friendsSectionMain).visibility = View.VISIBLE
            findViewById<ProgressBar>(R.id.progressBar2).visibility = View.GONE
        })
        findViewById<EditText>(R.id.search_friend_by_username)
            .addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    searcherViewModel.stringUserFriendsFilter.value = s.toString()
            }
        })
        // теги
        val tags = searcherViewModel.tags
        val filtersLayout: LinearLayout = findViewById(R.id.search_by_filters)
        lateinit var layout: LinearLayout
        for (i in 0..<tags.size) {
            if (i % 3 == 0) {
                layout = LinearLayout(applicationContext)
                layout.orientation = LinearLayout.HORIZONTAL
                layout.gravity = Gravity.CENTER
                filtersLayout.addView(layout)
            }
            val filter = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
            filter.text = tags[i].first
            filter.textSize = 18F
            filter.setTextColor(Color.parseColor(tags[i].third.second))
            filter.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
            filter.rippleColor = ColorStateList.valueOf(Color.parseColor(tags[i].third.second))
            filter.isClickable = true
            filter.tooltipText = tags[i].second
            layout.addView(filter)

            filter.setOnClickListener {
                filter.isActivated = !filter.isActivated
                searcherViewModel.reapplyFilter(i)
                if(!filter.isActivated) filter.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
                else filter.setBackgroundColor(Color.parseColor(tags[i].third.first))
            }

        }
        var searcherUsed = false // нажималась ли кнопка "Поиск"
        val searcher = findViewById<EditText>(R.id.search_user_by_username)
        searcher.isActivated = false
        searcher.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searcherViewModel.stringFriendsFilter.value = s.toString()
            }
        })

        val searchBtn: Button = findViewById(R.id.search_btn_by_filters)
        searchBtn.setOnClickListener {
            searcherViewModel.searchFor()
            searcherUsed = true
            searcher.visibility = View.VISIBLE
        }

        val nfl: LinearLayout = findViewById(R.id.no_find_new_layout)
        searcher.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                view.isActivated = !view.isActivated
                if (view.isActivated) {
                    nfl.visibility = View.GONE
                    view.requestFocus()
                    searcher.hint = "Имя пользователя (нажмите, чтобы свернуть)"
                } else if (!view.isActivated){
                    nfl.visibility = View.VISIBLE
                    view.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    searcher.hint = "Имя пользователя (нажмите, чтобы раскрыть)"
                }
            }
            false
        }

        // найденные
        searcherViewModel.filteredFriends.observe(this, Observer {
            val layoutForFiltered: LinearLayout = findViewById(R.id.foundFriendsLayoutTo)
            it?.let{
                findViewById<TextView>(R.id.bad_tags_no_found).visibility = View.GONE
                findViewById<TextView>(R.id.bad_query_not_found).visibility = View.GONE
               layoutForFiltered.removeAllViews()
                if (it.size > 0) {
                    for (user in it) {
                        val layoutInf = layoutInflater.inflate(R.layout.layout_user_found_filters, null)
                        layoutInf.findViewById<TextView>(R.id.username_found_layout).text = user.username
                        val filters = user.filters.split(",").map { it.toInt()-1 }
                        val filtersLayout = layoutInf.findViewById<LinearLayout>(R.id.user_filters_layout)
                        for (f in filters) {
                            val tagFilter = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
                            tagFilter.text = tags[f].first
                            tagFilter.textSize = 16F
                            tagFilter.tooltipText = tags[f].second
                            tagFilter.setTextColor(Color.parseColor(tags[f].third.second))
                            tagFilter.setBackgroundColor(Color.parseColor(tags[f].third.first))
                            filtersLayout.addView(tagFilter)
                        }

                        Glide.with(this)
                            .load(Globals().getImgUrl("users", user.userId))
                            .placeholder(R.drawable.baseline_person_24)
                            .into(layoutInf.findViewById(R.id.user_img_found_layout))

                        layoutInf.findViewById<LinearLayout>(R.id.goto_profile).setOnClickListener {
                            val myIntent = Intent(this, ProfileActivity::class.java)
                            myIntent.putExtra("previousId", loggedUser)
                            Globals.getInstance().setString("CurrentlyWatching", user.userId)
                            this.startActivity(myIntent)
                        }

                        layoutForFiltered.addView(layoutInf)
                    }
                } else {
                    if (searcherUsed) {
                        if (searcher.text.toString().isEmpty()) {
                            searcher.visibility = View.GONE

                            findViewById<TextView>(R.id.bad_tags_no_found).visibility = View.VISIBLE
                        } else {
                            val warningText = findViewById<TextView>(R.id.bad_query_not_found)
                            warningText.visibility = View.VISIBLE
                            warningText.text = "Среди найденных по тегам пользователей нет пользователя с никнеймом ${searcher.text}!"
                        }
                    }
                }
            }
        })
    }
}