package com.gy.ecotrace.ui.more.profile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.MoreFragment
import com.gy.ecotrace.ui.more.events.ShowEventActivity
import com.gy.ecotrace.ui.more.groups.ShowGroupActivity


class ProfileActivity : AppCompatActivity() {
    private lateinit var userViewModel: ProfileViewModel
    private var currentUser = Globals.getInstance().getString("CurrentlyWatching")
    private var loggedUser = Globals.getInstance().getString("CurrentlyLogged")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_profile)

            val repository = Repository(
                DatabaseMethods.UserDatabaseMethods(),
                DatabaseMethods.ApplicationDatabaseMethods()
            )

            userViewModel = ProfileViewModel(Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods()))


            val toolbar: Toolbar = findViewById(R.id.toolbar_profile)
            val backIcon =
                ContextCompat.getDrawable(applicationContext, R.drawable.baseline_arrow_back_24)
            backIcon?.setColorFilter(
                ContextCompat.getColor(applicationContext, R.color.ok_green),
                PorterDuff.Mode.SRC_ATOP
            )
            toolbar.navigationIcon = backIcon
            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }

            if (currentUser == loggedUser) {
                findViewById<LinearLayout>(R.id.add_user_as_friend).visibility = View.GONE
                findViewById<LinearLayout>(R.id.invite_user_to_group).visibility = View.GONE

                toolbar.inflateMenu(R.menu.popup_menu_profile)
                toolbar.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_settings -> {
                            val myIntent = Intent(this, ChangeProfile::class.java)
                            this.startActivity(myIntent)
                            true
                        }

                        R.id.action_logout -> {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Выход из аккаунта")

                            builder.setMessage("Вы действительно хотите выйти из своего аккаунта?")
                            builder.setPositiveButton("Подтвердить") { dialog, which ->
                                DatabaseMethods.Account().signOut()
                                finish()
                            }
                            builder.setNegativeButton("Отмена") { dialog, which ->
                                dialog.dismiss()
                            }
                            val dialog = builder.create()
                            dialog.show()
                            true
                        }

                        else -> {
                            super.onOptionsItemSelected(it)
                        }
                    }
                }
            } else {
                findViewById<LinearLayout>(R.id.add_user_as_friend).setOnClickListener {
                    userViewModel.friends.observe(this, Observer {
                        val anyCommunications =
                            it.indexOfFirst { fship -> fship.userId == loggedUser }

                        if (anyCommunications != -1) {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Удаление из друзей")

                            builder.setMessage("Вы действительно хотите удалить этого пользователя из списка своих друзей?")
                            builder.setPositiveButton("Подтвердить") { dialog, which ->
                                userViewModel.removeFriend(currentUser)
                                recreate()
                            }
                            builder.setNegativeButton("Отмена") { dialog, which ->
                                dialog.dismiss()
                            }
                            val dialog = builder.create()
                            dialog.show()
                        } else {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Добавление в друзья")

                            builder.setMessage("Вы действительно хотите добавить этого пользователя в список своих друзей?")
                            builder.setPositiveButton("Подтвердить") { dialog, which ->
                                userViewModel.addFriend(loggedUser)
                                recreate()
                            }
                            builder.setNegativeButton("Отмена") { dialog, which ->
                                dialog.dismiss()
                            }
                            val dialog = builder.create()
                            dialog.show()
                        }
                    })
                }
                findViewById<LinearLayout>(R.id.invite_user_to_group).visibility = View.GONE
            }

            Glide.with(this)
                .load(Globals().getImgUrl("users", currentUser))
                .placeholder(R.drawable.baseline_person_24)
                .circleCrop()
                .into(findViewById(R.id.profile_image_profile_menu))



            userViewModel.getUser(currentUser)
            userViewModel.getEvents(currentUser)
            userViewModel.getFriends(currentUser)
            userViewModel.getGroups(currentUser)

            userViewModel.user.observe(this, Observer { user ->
                if (user == null) {
                    Toast.makeText(this, "Запрещено!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                user?.let {
                    findViewById<TextView>(R.id.username_profile_menu_text).text =
                        user.username
                    findViewById<TextView>(R.id.user_about_me_text).text = user.aboutMe
                    findViewById<TextView>(R.id.user_country_text).text = user.country.name
                    findViewById<ImageView>(R.id.imageView)
                        .setImageBitmap(getCountryEmoji(user.country.code))


                }
            })
            val eventRoles = DatabaseMethods.DataClasses.EventRoles

            userViewModel.events.observe(this, Observer { events ->
                events?.let {
                    if (events.size > 0) {
                        findViewById<TextView>(R.id.text_warning_no_activity).visibility =
                            View.GONE
                        findViewById<HorizontalScrollView>(R.id.events_scroll_view_profile).visibility =
                            View.VISIBLE
                        val activitiesLayout: LinearLayout =
                            findViewById(R.id.activities_layout)
                        activitiesLayout.removeAllViews()
                        for (event in events) {
                            val eventName = event.eventInfo.eventName
                            val eventId = event.eventInfo.eventId
                            val activityOneLayout =
                                layoutInflater.inflate(R.layout.layout_user_event, null)
                            activityOneLayout.findViewById<TextView>(R.id.eventName).text =
                                eventName
//                            val eventStatusString: String = when (act.eventInfo.eventStatus) {
//                                0 -> "Еще не началось!"
//                                1 -> "Уже проходит!"
//                                2 -> "Закончилось"
//                                else -> "error-unreal-status"
//                            }
//                            activityOneLayout.findViewById<TextView>(R.id.event_status).text =
//                                eventStatusString
//                            activityOneLayout.findViewById<TextView>(R.id.event_count_members).text =
//                                "${act.eventInfo.eventCountMembers} участников"
//                            activityOneLayout.findViewById<TextView>(R.id.event_user_role).text =
//                                "Роль: "//${eventRoles[act.eventInfo.eventUsersToTheirRoles!!.get(currentUser)!!]}"

                            Glide.with(this)
                                .load(Globals().getImgUrl("events", eventId))
                                .placeholder(R.drawable.round_family_restroom_24)
                                .into(activityOneLayout.findViewById(R.id.eventImage))

                            activityOneLayout.setOnClickListener {
                                Globals.getInstance()
                                    .setString("CurrentlyWatchingEvent", eventId)
                                startActivity(Intent(this, ShowEventActivity::class.java))
                            }

                            activitiesLayout.addView(activityOneLayout)
                        }
                    }
                }

            })

            userViewModel.friends.observe(this, Observer { friends ->
                friends?.let {
                    if (friends.size > 0) {
                        val friendsLayout: LinearLayout = findViewById(R.id.friends_layout)
                        friendsLayout.removeAllViews()
                        for (fr in friends) {
                            if (fr.userId != currentUser) {
                                if (fr.sender && !fr.friend && currentUser == loggedUser || fr.friend) {
                                    val friendOneLayout =
                                        layoutInflater.inflate(R.layout.friend_linear_layout, null)
                                    friendOneLayout.findViewById<TextView>(R.id.username_friend_layout).text =
                                        fr.username

                                    friendOneLayout.setOnClickListener {
                                        val myIntent =
                                            Intent(this, ProfileActivity::class.java)
                                        myIntent.putExtra("previousId", currentUser)
                                        Log.d("goto", fr.userId)
                                        Globals.getInstance()
                                            .setString("CurrentlyWatching", fr.userId)
                                        startActivity(myIntent)
                                    }

                                    Glide.with(this)
                                        .load(Globals().getImgUrl("users", "${fr.userId}"))
                                        .placeholder(R.drawable.baseline_person_24)
                                        .into(friendOneLayout.findViewById(R.id.user_img_friend_layout))

                                    friendsLayout.addView(friendOneLayout)
                                }
                                if (fr.userId == loggedUser) {
                                    findViewById<TextView>(R.id.textView24).text =
                                        if (fr.friend) "Ваш друг" else "Отправлен"
                                    findViewById<ImageButton>(R.id.imageButton2)
                                        .setImageResource(R.drawable.baseline_person_remove_24)
                                }
                            }
                        }
                    }
                }
            })

            userViewModel.groups.observe(this, Observer { groups ->
                groups?.let {
                    if (groups.size > 0) {
                        val groupsLayout: LinearLayout = findViewById(R.id.groups_layout)
                        groupsLayout.removeAllViews()
                        for (group in groups) {
                            val groupName = group.groupInfo.groupName
                            val groupId = group.groupInfo.groupId
                            val groupOneLayout =
                                layoutInflater.inflate(R.layout.layout_user_group, null)
                            groupOneLayout.findViewById<TextView>(R.id.groupName).text =
                                groupName
                            Glide.with(this)
                                .load(Globals().getImgUrl("groups", groupId))
                                .centerInside()
                                .into(groupOneLayout.findViewById(R.id.groupImage))

                            groupOneLayout.setOnClickListener {
                                Globals.getInstance()
                                    .setString("CurrentlyWatchingGroup", groupId)
                                startActivity(Intent(this, ShowGroupActivity::class.java))
                            }

                            groupsLayout.addView(groupOneLayout)
                        }
                    }
                }
            })

//            userViewModel.additionalData.observe(this, Observer {
//                if (it) {
            val srl = findViewById<SwipeRefreshLayout>(R.id.refresh_profile)
            srl.visibility = View.VISIBLE

            srl.setOnRefreshListener {
                userViewModel.getUser(currentUser)
                userViewModel.getEvents(currentUser)
                userViewModel.getFriends(currentUser)
                userViewModel.getGroups(currentUser)

                srl.isRefreshing = false
            }
                    findViewById<ProgressBar>(R.id.progressBar_loading).visibility = View.GONE
//                }
//            })
        }

private fun getCountryEmoji(country: String?): Bitmap {
    if (country == null) {
        val image = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText("flag", 0f, 0f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
        return image//ContextCompat.getDrawable(applicationContext, R.drawable.baseline_flag_24).dra
    }

    val flag1 = Character.codePointAt(country, 0) - 0x41 + 0x1F1E6
    val flag2 = Character.codePointAt(country, 1) - 0x41 + 0x1F1E6
    val flag = String(Character.toChars(flag1)) + String(Character.toChars(flag2))

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.textSize = 100f
    paint.color = Color.Black.toArgb()
    paint.textAlign = android.graphics.Paint.Align.LEFT
    paint.typeface = Typeface.DEFAULT

    val baseline = -paint.ascent()
    val width = (paint.measureText(flag) + 0.5f).toInt()
    val height = (baseline + paint.descent() + 0.5f).toInt()

    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    canvas.drawText(flag, 0f, baseline, paint)
    return image
}

    override fun onBackPressed() {
        super.onBackPressed()
        val previousId = intent.getStringExtra("previousId")
        if (previousId != null) {
            Globals.getInstance().setString("CurrentlyWatching", previousId)
        } else {
//            supportFragmentManager.findFragmentByTag(MoreFragment::class.java)
        }
    }
}