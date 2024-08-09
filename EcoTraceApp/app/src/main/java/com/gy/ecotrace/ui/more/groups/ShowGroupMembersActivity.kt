package com.gy.ecotrace.ui.more.groups

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.GroupRepository
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupMembersViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity


class ShowGroupMembersActivity : AppCompatActivity() {
    private fun imageLoadWithLoading(folder: String, imageId: String, element: ImageView, placeHolder: Int, circle: Boolean = true) {
        var img = Glide.with(this@ShowGroupMembersActivity)
            .load(Globals().getImgUrl(folder, imageId))
            .placeholder(placeHolder)
            .skipMemoryCache(true)

        if (circle) img = img.circleCrop()
        img.into(element)
        element.foreground =
            ColorDrawable(ContextCompat.getColor(applicationContext, R.color.transparent))
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

        val groupDataRepo = GroupRepository.DataStorage()
        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val showGroupMembersViewModel = ShowGroupMembersViewModel(repository)

        var userAbilities = showGroupMembersViewModel.roleToAbilities(3)
        val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")
        val currentUser = Globals.getInstance().getString("CurrentlyLogged")
        groupDataRepo.groupData.let { group ->
            showGroupMembersViewModel.getUsername(group.groupCreatorId) {
                findViewById<TextView>(R.id.creatorName).text = it
                if (currentUser == group.groupCreatorId) {
                    userAbilities = showGroupMembersViewModel.roleToAbilities(0)
//                    findViewById<TextView>(R.id.creatorName).text = "${it} (Это вы!)"
                }
                imageLoadWithLoading(
                    "users",
                    group.groupCreatorId,
                    findViewById(R.id.creatorImage),
                    R.drawable.baseline_person_24
                )
                findViewById<LinearLayout>(R.id.openProfile).setOnClickListener { _ ->
                    Globals.getInstance().setString("CurrentlyWatching", group.groupCreatorId)
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                    )
                }
            }
        }



        val showedUsers = mutableListOf<String>()
        var removeAlls = true

        fun updateUsers(){
            showGroupMembersViewModel.lastMemberId = null
            showedUsers.clear()
            removeAlls = true
            showGroupMembersViewModel.getAndObserveMembers(currentGroup)
        }

        val swipeRefresh: SwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        showGroupMembersViewModel.lastMemberId = null
        showGroupMembersViewModel.getAndObserveMembers(currentGroup)
        showGroupMembersViewModel.users.observe(this) { usersMap ->
            for ((role, value) in usersMap) {
                val currentRole = role.last().toString().toInt()
                val rolesLayout =
                    findViewById<LinearLayout>(resources.getIdentifier(role, "id", packageName))
                if (removeAlls) rolesLayout.removeAllViews()
                if (value.size == 0) {
                    Log.d("searching for warning", "${role}warningnoone")
//                    val text = findViewById<TextView>(resources.getIdentifier("${role}warningnoone", "id", packageName))
//                    text.visibility = View.VISIBLE
                }
                findViewById<TextView>(resources.getIdentifier("${role}count", "id", packageName))
                        .text = value.size.toString()

                var oneRow = LinearLayout(applicationContext)
                var count = 0
                for ((member, _) in value) {
                    Log.d("found user", member)
//                    if (member == currentUser) userAbilities = showGroupViewModel.currentUserRules(role.last().toString().toInt())
                    Log.d("found user1", member)

                    if (count % 6 == 0) {
                        oneRow = LinearLayout(applicationContext)
                        oneRow.orientation = LinearLayout.HORIZONTAL
                        rolesLayout.addView(oneRow)
                    }
                    Log.d("found user2", member)

                    showGroupMembersViewModel.getUsername(member){ itt ->
                        Log.d("found user3", member)
                        if (!showedUsers.contains(member)){
                            Log.d("found user4", member)
                            val userLayout = layoutInflater.inflate(R.layout.layout_user_in_group, null)
//                            Log.d("jkfl", "${userAbilities.manageUsers} ${role.last().toString().toInt()}")
                            if (currentRole >= -1){//userAbilities.manageUsers) {
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
                                    popupView.findViewById<TextView>(R.id.username).text = itt
                                    val makeCeo = popupView.findViewById<TextView>(R.id.toceo)
                                    val makeHelper = popupView.findViewById<TextView>(R.id.tohelper)
                                    val makeMember = popupView.findViewById<TextView>(R.id.tomember)
                                    when (currentRole) {
                                        1 -> makeCeo.visibility = View.GONE
                                        2 -> makeHelper.visibility = View.GONE
                                        3 -> makeMember.visibility = View.GONE
                                    }
                                    if (userAbilities.manageUsers != 0) {
                                        makeCeo.visibility = View.GONE
                                        makeHelper.visibility = View.GONE
                                    } else {
                                        makeCeo.setOnClickListener { _ ->
                                            if ((usersMap["role1"]?.size ?: 0) < 5) {
                                                showGroupMembersViewModel.setUserRole(
                                                    currentGroup,
                                                    member,
                                                    currentRole,
                                                    1
                                                )
                                                updateUsers()
                                            }
                                        }
                                        makeHelper.setOnClickListener {
                                            if ((usersMap["role2"]?.size ?: 0) < 15) {
                                                showGroupMembersViewModel.setUserRole(
                                                    currentGroup,
                                                    member,
                                                    currentRole,
                                                    2
                                                )
                                                updateUsers()
                                            }
                                        }
                                    }
                                    makeMember.setOnClickListener {
                                        showGroupMembersViewModel.setUserRole(currentGroup, member, currentRole, 3)
                                        updateUsers()
                                    }
                                    popupView.findViewById<TextView>(R.id.kick).setOnClickListener {
                                        showGroupMembersViewModel.kickUser(currentGroup, member, currentRole)
                                        updateUsers()
                                    }

                                    val location = IntArray(2)
                                    menu.getLocationOnScreen(location)
                                    popupWindow.showAtLocation(menu, Gravity.NO_GRAVITY,
                                        location[0], location[1] + menu.height)
                                }
                            }
                            userLayout.findViewById<TextView>(R.id.username).text = itt
                            imageLoadWithLoading("users", member, userLayout.findViewById(R.id.userImage), R.drawable.baseline_person_24)
                            userLayout.setOnClickListener {
                                Globals.getInstance().setString("CurrentlyWatching", member)
                                startActivity(
                                    Intent(this, ProfileActivity::class.java)
                                )
                            }
                            oneRow.addView(userLayout)
                            showedUsers.add(member)
                        }
                    }
                    count++
                }
            }
            removeAlls = false
        }
        val scrollView: ScrollView = findViewById(R.id.scrollViewMembers)
        scrollView.setOnScrollChangeListener { view, _,_,_,_ ->
            if (scrollView.getChildAt(scrollView.childCount-1).bottom == scrollView.height+view.scrollY) {
                showGroupMembersViewModel.getAndObserveMembers(currentGroup)
            }
        }

        swipeRefresh.setOnRefreshListener {
            updateUsers()
            swipeRefresh.isRefreshing = false
        }
    }
}