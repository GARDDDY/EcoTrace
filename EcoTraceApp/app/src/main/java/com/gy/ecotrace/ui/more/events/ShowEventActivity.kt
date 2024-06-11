package com.gy.ecotrace.ui.more.events

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.profile.ProfileActivity

class ShowEventViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShowEventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShowEventViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ShowEventActivity : AppCompatActivity() {
    private lateinit var eventViewModel: ShowEventViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_event)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val currentEvent = Globals.getInstance().getString("CurrentlyWatchingEvent")

        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val factory = ShowEventViewModelFactory(repository)
        eventViewModel = ViewModelProvider(this, factory)[ShowEventViewModel::class.java]

        val toolbar: Toolbar = findViewById(R.id.toolbar3)
        val backIcon = ContextCompat.getDrawable(applicationContext, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(applicationContext, R.color.ok_green), PorterDuff.Mode.SRC_ATOP)
        toolbar.navigationIcon = backIcon
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        eventViewModel.getEvent(currentEvent)
        eventViewModel.event.observe(this, Observer {
                findViewById<TextView>(R.id.eventName).text = it.eventName
                findViewById<TextView>(R.id.eventAbout).text = it.eventAbout
                findViewById<TextView>(R.id.eventCountMembers).text = it.eventCountMembers.toString()

            eventViewModel.getEventMembers(currentEvent, it)
        })


        eventViewModel.members.observe(this, Observer { member ->
//            for (member in members) {
                val userOneLayoutInEvent = layoutInflater.inflate(R.layout.layout_user_in_event, null)
                userOneLayoutInEvent.findViewById<TextView>(R.id.username_user_in_event_layout).text = member.username
                userOneLayoutInEvent.findViewById<TextView>(R.id.user_role_in_event_user_in_event_layout).text = DatabaseMethods.DataClasses.EventRoles[member.userRole]
                userOneLayoutInEvent.findViewById<TextView>(R.id.user_group_user_in_event_layout).text = member.userBestGroupName
                userOneLayoutInEvent.findViewById<TextView>(R.id.user_rank_user_in_event_layout).text = member.userRank
                userOneLayoutInEvent.findViewById<TextView>(R.id.user_experience_user_in_event_layout).text = member.userExperience.toString()

                Glide.with(this@ShowEventActivity)
                    .load(Globals().getImgUrl("users", member.userId))
                    .placeholder(R.drawable.baseline_person_24)
                    .into(userOneLayoutInEvent.findViewById(R.id.user_img_user_in_event_layout))


                userOneLayoutInEvent.findViewById<LinearLayout>(R.id.profile_open).setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", member.userId)
                    this@ShowEventActivity.startActivity(Intent(this@ShowEventActivity, ProfileActivity::class.java))
                }

                findViewById<LinearLayout>(R.id.users_in_event_layout).addView(userOneLayoutInEvent)
//            }
        })

//        findViewById<TextView>(R.id.eventName).text = eventData.eventInfo.eventName
//        findViewById<TextView>(R.id.eventAbout).text = eventData.eventInfo.eventAbout
//        findViewById<TextView>(R.id.eventCountMembers).text = eventData.eventInfo.eventCountMembers.toString()
//
//        eventData.eventInfo.eventUsersToTheirNames!!.forEach {(key, value) ->
//            val userOneLayoutInEvent = layoutInflater.inflate(R.layout.layout_user_in_event, null)
//            userOneLayoutInEvent.findViewById<TextView>(R.id.username_user_in_event_layout).text = value.username
//            userOneLayoutInEvent.findViewById<TextView>(R.id.user_role_in_event_user_in_event_layout).text = DatabaseMethods.DataClasses.EventRoles[value.role]
//
//            Glide.with(this@ShowEventActivity)
//                .load(Globals().getImgUrl("users", key))
//                .placeholder(R.drawable.baseline_person_24)
//                .into(userOneLayoutInEvent.findViewById(R.id.user_img_user_in_event_layout))
//
//
//            userOneLayoutInEvent.findViewById<LinearLayout>(R.id.profile_open).setOnClickListener {
//                Globals.getInstance().setString("CurrentlyWatching", key)
//                this@ShowEventActivity.startActivity(Intent(this@ShowEventActivity, ProfileActivity::class.java))
//            }
//
//            findViewById<LinearLayout>(R.id.users_in_event_layout).addView(userOneLayoutInEvent)
//        }

        Glide.with(this@ShowEventActivity)
            .load(Globals().getImgUrl("events", currentEvent))
            .placeholder(R.drawable.round_family_restroom_24)
            .into(findViewById(R.id.eventImage))



    }
}