package com.gy.ecotrace.ui.more.events

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
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.EventMember
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowEventMembersActivity : AppCompatActivity() {

    private val viewModel: ShowEventViewModel by viewModels()

    private fun avatarUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" }

    private fun eventRoleLabel(role: String): String = when (role) {
        "creator" -> "Создатель"; "assistant" -> "Помощник"; else -> "Участник"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_event_members)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        viewModel.currentEvent = Globals.getInstance().getString("CurrentlyWatchingEvent").toLongOrNull() ?: 0
        viewModel.getEvent()

        val container = findViewById<LinearLayout>(R.id.membersLayout)

        viewModel.getEventMembers(new = true)
        viewModel.members.observe(this, Observer { members ->
            if (viewModel.new) {
                container.removeAllViews()
                viewModel.new = false
            }
            for (user in members) addMember(container, user)
        })

        val scrollView: ScrollView = findViewById(R.id.scrollViewMembers)
        scrollView.setOnScrollChangeListener { view, _, _, _, _ ->
            val lastChild = scrollView.getChildAt(scrollView.childCount - 1)
            if (lastChild != null &&
                lastChild.bottom == scrollView.height + view.scrollY &&
                !viewModel.foundAll && !viewModel.searching
            ) {
                viewModel.searching = true
                viewModel.getEventMembers()
            }
        }
    }

    private fun addMember(container: LinearLayout, user: EventMember) {
        val row = layoutInflater.inflate(R.layout.layout_event__user_in_event, container, false)
        row.findViewById<TextView>(R.id.username_user_in_event_layout).text = user.username
        row.findViewById<TextView>(R.id.user_role_in_event_user_in_event_layout).text = eventRoleLabel(user.role)
        row.findViewById<TextView>(R.id.user_rank_user_in_event_layout).text = ""
        row.findViewById<TextView>(R.id.user_experience_user_in_event_layout).text = user.experience.toString()

        val manage = row.findViewById<ImageButton>(R.id.manageUser)
        if (user.role == "creator" || !viewModel.isCreator) {
            manage.visibility = View.GONE
        } else {
            manage.setOnClickListener {
                val popupView = layoutInflater.inflate(R.layout.layout_event__menu_manage_user_in_event, null)
                val popupWindow = PopupWindow(
                    popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
                )
                popupView.findViewById<TextView>(R.id.username).text = user.username
                val toMember = popupView.findViewById<TextView>(R.id.tomember)
                val toHelper = popupView.findViewById<TextView>(R.id.tohelper)
                if (user.role == "participant") toMember.visibility = View.GONE
                if (user.role == "assistant") toHelper.visibility = View.GONE

                toMember.setOnClickListener {
                    viewModel.setUserRole(user.userId, "participant") { ok ->
                        Toast.makeText(this, if (ok) "${user.username} теперь участник!" else "Ошибка!", Toast.LENGTH_SHORT).show()
                        viewModel.getEventMembers(new = true)
                    }
                    popupWindow.dismiss()
                }
                toHelper.setOnClickListener {
                    viewModel.setUserRole(user.userId, "assistant") { ok ->
                        Toast.makeText(this, if (ok) "${user.username} теперь помощник!" else "Ошибка!", Toast.LENGTH_SHORT).show()
                        viewModel.getEventMembers(new = true)
                    }
                    popupWindow.dismiss()
                }

                val location = IntArray(2)
                manage.getLocationOnScreen(location)
                popupWindow.showAtLocation(manage, Gravity.NO_GRAVITY, location[0], location[1] + manage.height)
            }
        }

        Glide.with(this).load(avatarUrl(user.avatarKey)).circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(row.findViewById(R.id.user_img_user_in_event_layout))

        row.findViewById<LinearLayout>(R.id.profile_open).setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatching", user.userId.toString())
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        container.addView(row)
    }
}
