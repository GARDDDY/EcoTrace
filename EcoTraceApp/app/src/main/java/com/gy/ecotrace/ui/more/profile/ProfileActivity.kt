package com.gy.ecotrace.ui.more.profile

import com.gy.ecotrace.customs.SpinnerMultiChoice
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.events.ShowEventActivity
import com.gy.ecotrace.ui.more.groups.ShowGroupActivity


class ProfileActivity : AppCompatActivity() {
    private lateinit var userViewModel: ProfileViewModel
    private var currentUser = Globals.getInstance().getString("CurrentlyWatching")
    private var loggedUser = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var toolbar: Toolbar
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_profile)

            val repository = Repository(
                DatabaseMethods.UserDatabaseMethods(),
                DatabaseMethods.ApplicationDatabaseMethods()
            )

            userViewModel = ProfileViewModel(repository)


            toolbar = findViewById(R.id.toolbarProfile)
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
//
            if (currentUser == loggedUser) {
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
                val setFriend = findViewById<ImageButton>(R.id.setFriend)
                userViewModel.areFriends(currentUser) {
                    if (it) {
                        setFriend.setImageResource(R.drawable.baseline_person_remove_24)
                        setFriend.imageTintList =
                            ContextCompat.getColorStateList(applicationContext, R.color.red_no)

                        setFriend.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Удаление из друзей")

                            builder.setMessage("Вы действительно хотите удалить этого пользователя из списка своих друзей?")
                            builder.setPositiveButton("Подтвердить") { _, _ ->
                                userViewModel.removeFriend(currentUser)
                                recreate()
                            }
                            builder.setNegativeButton("Отмена") { dialog, _ ->
                                dialog.dismiss()
                            }
                            val dialog = builder.create()
                            dialog.show()
                        }

                    } else {
                        setFriend.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Добавление в друзья")

                            builder.setMessage("Вы действительно хотите добавить этого пользователя в список своих друзей?")
                            builder.setPositiveButton("Подтвердить") { _, _ ->
                                userViewModel.addFriend(loggedUser)
                                recreate()
                            }
                            builder.setNegativeButton("Отмена") { dialog, _ ->
                                dialog.dismiss()
                            }
                            val dialog = builder.create()
                            dialog.show()
                        }
                    }

                    setFriend.visibility = View.VISIBLE
                }
            }

            userViewModel.getUser(currentUser)
            userViewModel.getEvents(currentUser)
            userViewModel.getFriends(currentUser)
            userViewModel.getGroups(currentUser)

            userViewModel.user.observe(this, Observer { user ->
                user?.let {

                    findViewById<ShimmerFrameLayout>(R.id.userMainInformationLayoutLoading).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.userMainInformationLayout).visibility = View.VISIBLE

                    val username = findViewById<TextView>(R.id.userUsernameProfile)
                    username.text = user.username
                    username.setOnClickListener {

                    }

                    Glide.with(this)
                        .load(DatabaseMethods
                            .ApplicationDatabaseMethods()
                            .getImageLink("users", user.userId))
                        .circleCrop()
                        .placeholder(R.drawable.baseline_person_24)
                        .into(findViewById(R.id.userImageProfile))

                    toolbar.title = user.username
                    toolbar.setTitleTextColor(Color.Transparent.toArgb())


                    val about = findViewById<TextView>(R.id.userAboutProfile)
                    about.text = user.aboutMe
                    about.setOnClickListener {
                        about.isActivated = !about.isActivated
                        about.maxLines = if (about.isActivated) Int.MAX_VALUE else 3
                    }
                    findViewById<TextView>(R.id.userExperienceProfile).text = user.experience.toString()

                    val expToRanks = mutableMapOf(
                        100 to "rank1",
                        300 to "rank2",
                        700 to "rank3",
                        1200 to "rank4",
                        1800 to "rank5",
                        2500 to "rank6"
                    )

                    Glide.with(this)
                        .load(DatabaseMethods
                            .ApplicationDatabaseMethods()
                            .getImageLink("\$ranks", expToRanks[expToRanks.keys.filter { it <= user.experience }.maxOrNull()]!!))
                        .into(findViewById(R.id.userExperienceImageProfile))
                    findViewById<TextView>(R.id.userCountryName).text = user.country.name
                    findViewById<TextView>(R.id.userCountryFlag).text = getCountryEmoji(user.country.code)


                }
            })


            var lastTypes: MutableList<Int>? = null
            var lastTime = 0
            val spinnerTimes = findViewById<Spinner>(R.id.ecoDataTimesIntervalChoose)
            val spinnerTypes = findViewById<Spinner>(R.id.ecoDataDataSpecifyChoose)

            val items = listOf("Пища", "Вода", "Отходы", "Энергия", "Транспорт")
            if (spinnerTypes.adapter == null) {
                val adapter = SpinnerMultiChoice(applicationContext, items)
                adapter.setDropDownViewResource(R.layout.widget_spinner_multichoice_item)
                spinnerTypes.dropDownWidth = 500
                spinnerTypes.dropDownHorizontalOffset = spinnerTimes.right
                spinnerTypes.dropDownVerticalOffset = 50
                spinnerTypes.adapter = adapter

                lastTypes = adapter.getUnSelectedItems().mapNotNull { item ->
                    items.indexOf(item).takeIf { it != -1 }
                }.toMutableList()

                userViewModel.getUserGraphs(currentUser, lastTime, lastTypes)
            }


            userViewModel.graph.observe(this, Observer { graph ->
                findViewById<ShimmerFrameLayout>(R.id.userEcoDataInformationLayoutLoading).visibility = View.GONE
                findViewById<LinearLayout>(R.id.userEcoDataInformationLayout).visibility = View.VISIBLE
                if (graph != null) {
                    findViewById<TextView>(R.id.noEcoDataWarning).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.ecoDataLayout).visibility = View.VISIBLE

                    spinnerTypes.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
                        if (hasFocus) {
                            lastTypes = (spinnerTypes.adapter as SpinnerMultiChoice)
                                    .getUnSelectedItems().mapNotNull { item ->
                                        items.indexOf(item).takeIf { it != -1 }
                                    }.toMutableList()

                            updateGraph()
                            Log.d("sending", "from types")
                            userViewModel.getUserGraphs(currentUser, lastTime, lastTypes)
                        }
                    }


                    spinnerTimes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            lastTime = position
                            updateGraph()
                            Log.d("sending", "from times")
                            userViewModel.getUserGraphs(currentUser, lastTime, lastTypes)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }

                    findViewById<ImageView>(R.id.userEcoDataGraph).setImageBitmap(graph)
                }
            })


            var lastSort = 0
            val eventRoles: Array<String> = Globals.getInstance().getEventRoles()

            val eventSort: Spinner = findViewById(R.id.eventsFiltersChoose)
            eventSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    if (lastSort != p2) {
                        lastSort = p2
                        userViewModel.getEvents(currentUser, lastSort, true)
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            userViewModel.events.observe(this, Observer { events ->
                Log.d("eventsGot", "$events")
                findViewById<ShimmerFrameLayout>(R.id.userEventsInformationLayoutLoading).visibility = View.GONE
                findViewById<LinearLayout>(R.id.userEventsInformationLayout).visibility = View.VISIBLE
                findViewById<TextView>(R.id.noEventsWarning).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.allEventsLayout).visibility = View.VISIBLE
                val eventsLayout: LinearLayout = findViewById(R.id.eventsLayout)
                if (userViewModel.newEvents) {
                    eventsLayout.removeAllViews()
                    userViewModel.newEvents = false
                }
                userViewModel.updEvents = false
                for (event in events) {
                    val eventName = event.eventInfo.eventName
                    val eventId = event.eventInfo.eventId
                    val activityOneLayout =
                        layoutInflater.inflate(R.layout.layout_user_event, null)
                    activityOneLayout.findViewById<TextView>(R.id.eventName).text =
                        eventName
                    activityOneLayout.findViewById<TextView>(R.id.eventCountMembers).text = event.eventInfo.eventCountMembers.toString()
                    activityOneLayout.findViewById<TextView>(R.id.eventUserRole).text = eventRoles[event.eventRole]
                    val eventStatusString: String = when (event.eventInfo.eventStatus) {
                            0 -> "Еще не началось!"
                            1 -> "Уже проходит!"
                            2 -> "Закончилось"
                            else -> "error-unreal-status"
                        }

                    activityOneLayout.findViewById<TextView>(R.id.eventStatus).text =
                        eventStatusString

                    val ending = when {
                        event.eventInfo.eventCountMembers % 100 in 11..14 -> "ов"
                        event.eventInfo.eventCountMembers % 10 == 1 -> ""
                        event.eventInfo.eventCountMembers % 10 in 2..4 -> "а"
                        else -> "ов"
                    }
                    activityOneLayout.findViewById<TextView>(R.id.membersWord).text = "участник$ending"


                    Glide.with(this)
                        .load(Globals().getImgUrl("events", eventId))
                        .into(activityOneLayout.findViewById(R.id.eventImage))

                    activityOneLayout.setOnClickListener {
                        Globals.getInstance()
                            .setString("CurrentlyWatchingEvent", eventId)
                        startActivity(Intent(this, ShowEventActivity::class.java))

                    }

                    eventsLayout.addView(activityOneLayout)
                    if (events.last() != event) {
                        val space = View(applicationContext)
                        val dpDefault = resources.getDimensionPixelSize(R.dimen.default_PaddingMargin)
                        val layoutParams = ViewGroup.LayoutParams(dpDefault, ViewGroup.LayoutParams.WRAP_CONTENT)
                        space.layoutParams = layoutParams
                        eventsLayout.addView(space)
                    }
                }
            })

            val eventScrollView: HorizontalScrollView = findViewById(R.id.allEventsScrollView)
            eventScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                if (!userViewModel.updEvents) {
                    if (scrollX >= (v as HorizontalScrollView).getChildAt(0).width - v.width) {
                        userViewModel.updEvents = true
                        userViewModel.getEvents(currentUser, lastSort)
                    }
                }
            }

            userViewModel.friends.observe(this, Observer { friends ->
                findViewById<ShimmerFrameLayout>(R.id.userFriendsInformationLayoutLoading).visibility = View.GONE
                findViewById<LinearLayout>(R.id.userFriendsInformationLayout).visibility = View.VISIBLE
                friends?.let {
                    findViewById<TextView>(R.id.noFriendsWarning).visibility = View.GONE
                    val friendsLayout: LinearLayout = findViewById(R.id.friendsLayout)
                    if (userViewModel.newFriends) {
                        friendsLayout.removeAllViews()
                        userViewModel.newFriends = false
                    }
                    userViewModel.updFriends = false
                    for (fr in friends) {
                        if (fr.userId != currentUser ) {
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
                                .circleCrop()
                                .placeholder(R.drawable.baseline_person_24)
                                .into(friendOneLayout.findViewById(R.id.user_img_friend_layout))

                            friendsLayout.addView(friendOneLayout)
                        }
                    }
                }
            })

            val friendScrollView: HorizontalScrollView = findViewById(R.id.allFriendsScrollView)
            friendScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                if (!userViewModel.updFriends) {
                    if (scrollX >= (v as HorizontalScrollView).getChildAt(0).width - v.width) {
                        userViewModel.updFriends = true
                        userViewModel.getFriends(currentUser)
                    }
                }
            }

            userViewModel.groups.observe(this, Observer { groups ->
                findViewById<ShimmerFrameLayout>(R.id.userGroupsInformationLayoutLoading).visibility = View.GONE
                findViewById<LinearLayout>(R.id.userGroupsInformationLayout).visibility = View.VISIBLE
                groups?.let {
                    findViewById<TextView>(R.id.noGroupsWarning).visibility = View.GONE
                    val groupsLayout: LinearLayout = findViewById(R.id.groupsLayout)
                    groupsLayout.removeAllViews()
                    for (group in groups) {
                        val groupName = group.groupInfo.groupName
                        val groupId = group.groupInfo.groupId
                        val groupOneLayout =
                            layoutInflater.inflate(R.layout.layout_user_group, null)
                        groupOneLayout.findViewById<TextView>(R.id.groupName).text =
                            groupName
                        groupOneLayout.findViewById<TextView>(R.id.groupAbout).text = group.groupInfo.groupAbout
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
                        if (groups.last() != group) {
                            val space = View(applicationContext)
                            val dpDefault = resources.getDimensionPixelSize(R.dimen.default_PaddingMargin)
                            val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpDefault)
                            space.layoutParams = layoutParams
                            groupsLayout.addView(space)
                        }
                    }
                }
            })
            val refresh = findViewById<SwipeRefreshLayout>(R.id.refreshProfile)
            refresh.setOnRefreshListener {
                findViewById<ShimmerFrameLayout>(R.id.userMainInformationLayoutLoading).visibility = View.VISIBLE
                findViewById<RelativeLayout>(R.id.userMainInformationLayout).visibility = View.GONE

                updateGraph()

                updateEvents()

                findViewById<ShimmerFrameLayout>(R.id.userFriendsInformationLayoutLoading).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.userFriendsInformationLayout).visibility = View.GONE

                findViewById<ShimmerFrameLayout>(R.id.userGroupsInformationLayoutLoading).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.userGroupsInformationLayout).visibility = View.GONE


                userViewModel.getUser(currentUser)
                userViewModel.getUserGraphs(currentUser, lastTime, lastTypes)
                userViewModel.getEvents(currentUser, lastSort, true)
                userViewModel.getFriends(currentUser, true)
                userViewModel.getGroups(currentUser, true)

                refresh.isRefreshing = false
            }

            val scrollView = findViewById<ScrollView>(R.id.scrollView)
            var hidden = false
            scrollView.viewTreeObserver.addOnScrollChangedListener {
                val scrollBounds = Rect()
                scrollView.getHitRect(scrollBounds)
                if (findViewById<TextView>(R.id.userUsernameProfile).getLocalVisibleRect(scrollBounds)) {
                    if (hidden) {
                        animationColorChange(
                            ContextCompat.getColor(applicationContext, R.color.ok_green),
                            ContextCompat.getColor(applicationContext, R.color.transparent)
                        )
                        hidden = false
                    }
                } else {
                    if (!hidden) {
                        animationColorChange(
                            ContextCompat.getColor(applicationContext, R.color.transparent),
                            ContextCompat.getColor(applicationContext, R.color.ok_green)
                        )
                        hidden = true
                    }
                }
            }
        }

    private fun updateGraph() {
        findViewById<ShimmerFrameLayout>(R.id.userEcoDataInformationLayoutLoading).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.userEcoDataInformationLayout).visibility = View.GONE
    }

    private fun updateEvents() {
        findViewById<ShimmerFrameLayout>(R.id.userEventsInformationLayoutLoading).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.userEventsInformationLayout).visibility = View.GONE
    }

    private fun animationColorChange(colorFrom: Int, colorTo: Int) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 300
        colorAnimation.addUpdateListener { animator ->
            toolbar.setTitleTextColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }

    private fun getCountryEmoji(country: String?): String {
        if (country == null) {
            return "🏳️"
        }

        val flag1 = Character.codePointAt(country, 0) - 0x41 + 0x1F1E6
        val flag2 = Character.codePointAt(country, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(flag1)) + String(Character.toChars(flag2))

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