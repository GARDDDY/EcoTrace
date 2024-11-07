package com.gy.ecotrace.ui.more.groups

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupMembersViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity


class ShowGroupMembersActivity : AppCompatActivity() {
    private var userRole: Int = 3
    private fun applyRoles(user: DatabaseMethods.DataClasses.UserInGroup, userLayout: View, showGroupMembersViewModel: ShowGroupMembersViewModel) {
        if (userRole < user.role) {
            val menu = userLayout.findViewById<ImageButton>(R.id.userManageMenu)
            menu.visibility = View.VISIBLE
            menu.setOnClickListener {
                val popupView = layoutInflater.inflate(R.layout.layout_menu_user_manage_in_group, null)
                val popupWindow = PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                )
                popupView.findViewById<TextView>(R.id.username).text = user.username
                val makeCeo = popupView.findViewById<TextView>(R.id.toceo)
                val makeHelper = popupView.findViewById<TextView>(R.id.tohelper)
                val makeMember = popupView.findViewById<TextView>(R.id.tomember)
                when (user.role) {
                    1 -> makeCeo.visibility = View.GONE
                    2 -> makeHelper.visibility = View.GONE
                    3 -> makeMember.visibility = View.GONE
                }
                if (userRole != 0) {
                    makeCeo.visibility = View.GONE
                } else {
                    makeCeo.setOnClickListener { _ ->
                        showGroupMembersViewModel.setUserRole(
                            user.userId,
                            1
                        ) {
                            if (it) {

                            } else Toast.makeText(this@ShowGroupMembersActivity, "Произошла ошибка!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    makeHelper.setOnClickListener {
                        showGroupMembersViewModel.setUserRole(
                            user.userId,
                            2
                        ) {
                            if (it) {

                            } else Toast.makeText(this@ShowGroupMembersActivity, "Произошла ошибка!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    makeMember.setOnClickListener {
                        showGroupMembersViewModel.setUserRole(
                            user.userId,
                            3
                        ) {
                            if (it) {

                            } else Toast.makeText(this@ShowGroupMembersActivity, "Произошла ошибка!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    val kick = popupView.findViewById<TextView>(R.id.kick)
                    if (userRole <= 2) {
                        popupView.findViewById<TextView>(R.id.kick).setOnClickListener {
                            showGroupMembersViewModel.kickUser(user.userId) {
                                if (it) {

                                } else Toast.makeText(this@ShowGroupMembersActivity, "Произошла ошибка!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        kick.visibility = View.GONE
                    }

                    val location = IntArray(2)
                    menu.getLocationOnScreen(location)
                    popupWindow.showAtLocation(menu, Gravity.NO_GRAVITY,
                        location[0], location[1] + menu.height)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_group_members)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val showGroupMembersViewModel = ShowGroupMembersViewModel(repository)

        val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")
        showGroupMembersViewModel.groupId = currentGroup
        val currentUser = ETAuth.getInstance().getUID()

        showGroupMembersViewModel.getRole {
            userRole = it

            showGroupMembersViewModel.getCreator()
            showGroupMembersViewModel.getCEOs()
            showGroupMembersViewModel.getHelpers()
            showGroupMembersViewModel.getMembers()
        }




        var layoutMembers1: LinearLayout? = null
        var layoutMembers2: LinearLayout? = null
        var layoutMembers3: LinearLayout? = null

        showGroupMembersViewModel.creator.observe(this, Observer {
            it?.let {
                findViewById<TextView>(R.id.creatorName).text = it.username
                findViewById<LinearLayout>(R.id.openProfile).setOnClickListener { _ ->
                    Globals.getInstance().setString("CurrentlyWatching", it.userId)
                    startActivity(Intent(this@ShowGroupMembersActivity, ProfileActivity::class.java))
                }
                Glide.with(this@ShowGroupMembersActivity)
                    .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", it.userId))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(findViewById(R.id.creatorImage))
            }
        })

        showGroupMembersViewModel.ceo.observe(this) { users ->
            findViewById<TextView>(R.id.role1count).text = users.size.toString()
            for (user in users) {

                if (layoutMembers1 == null) {
                    layoutMembers1 = LinearLayout(applicationContext)
                    layoutMembers1!!.orientation = LinearLayout.HORIZONTAL
                    findViewById<LinearLayout>(R.id.role1).addView(layoutMembers1)
                }

                val userLayout = layoutInflater.inflate(R.layout.layout_user_in_group, null)
                userLayout.findViewById<TextView>(R.id.username).text = user.username
                Glide.with(this@ShowGroupMembersActivity)
                    .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", user.userId))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(userLayout.findViewById(R.id.userImage))
                userLayout.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", user.userId)
                    startActivity(Intent(this@ShowGroupMembersActivity, ProfileActivity::class.java))
                }

                layoutMembers1!!.addView(userLayout)

                if (layoutMembers1!!.childCount == 6) {
                    layoutMembers1 = null
                }

                applyRoles(user, userLayout, showGroupMembersViewModel)
            }
        }

        showGroupMembersViewModel.helpers.observe(this) { users ->
            findViewById<TextView>(R.id.role2count).text = users.size.toString()
            for (user in users) {

                if (layoutMembers2 == null) {
                    layoutMembers2 = LinearLayout(applicationContext)
                    layoutMembers2!!.orientation = LinearLayout.HORIZONTAL
                    findViewById<LinearLayout>(R.id.role2).addView(layoutMembers2)
                }

                val userLayout = layoutInflater.inflate(R.layout.layout_user_in_group, null)
                userLayout.findViewById<TextView>(R.id.username).text = user.username
                Glide.with(this@ShowGroupMembersActivity)
                    .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", user.userId))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(userLayout.findViewById(R.id.userImage))
                userLayout.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", user.userId)
                    startActivity(Intent(this@ShowGroupMembersActivity, ProfileActivity::class.java))
                }

                layoutMembers2!!.addView(userLayout)

                if (layoutMembers2!!.childCount == 6) {
                    layoutMembers2 = null
                }

                applyRoles(user, userLayout, showGroupMembersViewModel)
            }
        }

        showGroupMembersViewModel.users.observe(this) { users ->
            for (user in users) {

                if (layoutMembers3 == null) {
                    layoutMembers3 = LinearLayout(applicationContext)
                    layoutMembers3!!.orientation = LinearLayout.HORIZONTAL
                    findViewById<LinearLayout>(R.id.role3).addView(layoutMembers3)
                }

                val userLayout = layoutInflater.inflate(R.layout.layout_user_in_group, null)
                userLayout.findViewById<TextView>(R.id.username).text = user.username
                Glide.with(this@ShowGroupMembersActivity)
                    .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", user.userId))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(userLayout.findViewById(R.id.userImage))
                userLayout.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", user.userId)
                    startActivity(Intent(this@ShowGroupMembersActivity, ProfileActivity::class.java))
                }

                layoutMembers3!!.addView(userLayout)

                if (layoutMembers3!!.childCount == 6) {
                    layoutMembers3 = null
                }

                applyRoles(user, userLayout, showGroupMembersViewModel)
            }
        }

        val scrollView: ScrollView = findViewById(R.id.scrollViewMembers)
        scrollView.setOnScrollChangeListener { view, _, _, _, _ ->
            if (scrollView.getChildAt(scrollView.childCount - 1).bottom == scrollView.height + view.scrollY) {
                showGroupMembersViewModel.getMembers()
            }
        }
    }
}