package com.gy.ecotrace.ui.more.groups

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.GroupMember
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupMembersViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowGroupMembersActivity : AppCompatActivity() {

    private val viewModel: ShowGroupMembersViewModel by viewModels()
    private var participantsRow: LinearLayout? = null

    private var searchMode = false

    private fun groupRoleRank(role: String?): Int = when (role) {
        "creator" -> 3; "manager" -> 2; "assistant" -> 1; else -> 0
    }

    private fun avatarUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" }

    private fun toast(text: String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_group_members)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        viewModel.groupId = Globals.getInstance().getString("CurrentlyWatchingGroup").toLongOrNull() ?: 0
        viewModel.loadRole()
        viewModel.loadAll()

        viewModel.creator.observe(this, Observer { creator ->
            creator ?: return@Observer
            findViewById<TextView>(R.id.creatorName).text = creator.username
            findViewById<LinearLayout>(R.id.openProfile).setOnClickListener {
                Globals.getInstance().setString("CurrentlyWatching", creator.userId.toString())
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            Glide.with(this).load(avatarUrl(creator.avatarKey)).circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(findViewById(R.id.creatorImage))
        })

        viewModel.managers.observe(this) { users ->
            findViewById<TextView>(R.id.role1count).text = users.size.toString()
            renderInto(findViewById(R.id.role1), users)
        }
        viewModel.assistants.observe(this) { users ->
            findViewById<TextView>(R.id.role2count).text = users.size.toString()
            renderInto(findViewById(R.id.role2), users)
        }
        viewModel.participants.observe(this) { users ->
            if (searchMode) return@observe
            val parent = findViewById<LinearLayout>(R.id.role3)
            for (user in users) participantsRow = addMember(parent, user, participantsRow)
        }

        val searchEntry = findViewById<EditText>(R.id.participantsSearchEntry)
        val searchHandler = Handler(Looper.getMainLooper())
        val searchRunnable = Runnable {
            val query = searchEntry.text.toString()
            if (query.isBlank()) {
                searchMode = false
                findViewById<LinearLayout>(R.id.role3).removeAllViews()
                participantsRow = null
                findViewById<TextView>(R.id.role3warningnoone).visibility = View.GONE
                viewModel.loadParticipants(reset = true)
            } else {
                searchMode = true
                viewModel.searchParticipants(query)
            }
        }
        searchEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchHandler.removeCallbacks(searchRunnable)
                searchHandler.postDelayed(searchRunnable, 500)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        viewModel.searchResults.observe(this) { users ->
            if (!searchMode) return@observe
            val parent = findViewById<LinearLayout>(R.id.role3)
            parent.removeAllViews()
            participantsRow = null
            for (user in users) participantsRow = addMember(parent, user, participantsRow)
            findViewById<TextView>(R.id.role3warningnoone).apply {
                text = getString(R.string.noParticipantsFound)
                visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.topActiveParticipants.observe(this) { renderTopActiveParticipants(it.items, it.total) }
        viewModel.roleReady.observe(this) { role ->
            val canKick = groupRoleRank(role) >= 2
            searchEntry.visibility = if (canKick) View.VISIBLE else View.GONE
            findViewById<LinearLayout>(R.id.role3).visibility = if (canKick) View.VISIBLE else View.GONE
            findViewById<LinearLayout>(R.id.role3TopActive).visibility = if (canKick) View.GONE else View.VISIBLE
            if (!canKick) viewModel.loadTopActiveParticipants()
        }

        val scrollView: ScrollView = findViewById(R.id.scrollViewMembers)
        scrollView.setOnScrollChangeListener { view, _, _, _, _ ->
            if (scrollView.getChildAt(scrollView.childCount - 1).bottom == scrollView.height + view.scrollY &&
                !viewModel.foundAll && !searchMode
            ) {
                viewModel.loadParticipants()
            }
        }
    }

    private fun renderTopActiveParticipants(users: List<GroupMember>, total: Int) {
        val container = findViewById<LinearLayout>(R.id.role3TopActive)
        container.removeAllViews()
        var row: LinearLayout? = null
        for ((index, user) in users.take(7).withIndex()) {
            if (index % 4 == 0) {
                row = LinearLayout(applicationContext).apply { orientation = LinearLayout.HORIZONTAL }
                container.addView(row)
            }
            val userLayout = layoutInflater.inflate(R.layout.layout_user_in_group, row, false)
            userLayout.findViewById<TextView>(R.id.username).text = user.username
            Glide.with(this).load(avatarUrl(user.avatarKey)).circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(userLayout.findViewById(R.id.userImage))
            userLayout.setOnClickListener {
                Globals.getInstance().setString("CurrentlyWatching", user.userId.toString())
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            row?.addView(userLayout)
        }
        val remaining = total - users.take(7).size
        if (remaining > 0) {
            if (row == null || row.childCount == 4) {
                row = LinearLayout(applicationContext).apply { orientation = LinearLayout.HORIZONTAL }
                container.addView(row)
            }
            val badge = layoutInflater.inflate(R.layout.layout_user_in_group, row, false)
            badge.findViewById<ImageView>(R.id.userImage).apply {
                setImageDrawable(null)
                setBackgroundResource(R.drawable.bg_circle_ok_green)
            }
            badge.findViewById<TextView>(R.id.username).text = "+$remaining"
            row?.addView(badge)
        }
    }

    private fun renderInto(parent: LinearLayout, users: List<GroupMember>) {
        parent.removeAllViews()
        var row: LinearLayout? = null
        for (user in users) row = addMember(parent, user, row)
    }

    private fun addMember(parent: LinearLayout, user: GroupMember, currentRow: LinearLayout?): LinearLayout {
        var row = currentRow
        if (row == null || row.childCount == 6) {
            row = LinearLayout(applicationContext).apply { orientation = LinearLayout.HORIZONTAL }
            parent.addView(row)
        }
        val userLayout = layoutInflater.inflate(R.layout.layout_user_in_group, null)
        userLayout.findViewById<TextView>(R.id.username).text = user.username
        Glide.with(this).load(avatarUrl(user.avatarKey)).circleCrop()
            .placeholder(R.drawable.baseline_person_24)
            .into(userLayout.findViewById(R.id.userImage))
        userLayout.setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatching", user.userId.toString())
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        row.addView(userLayout)
        applyRoles(user, userLayout)
        return row
    }

    private fun applyRoles(user: GroupMember, userLayout: View) {
        val myRank = groupRoleRank(viewModel.myRole)
        val targetRank = groupRoleRank(user.role)
        if (myRank < 2 || myRank <= targetRank) return

        val menu = userLayout.findViewById<ImageButton>(R.id.userManageMenu)
        menu.visibility = View.VISIBLE
        menu.setOnClickListener {
            val popupMenu = PopupMenu(this, menu)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_group_member, popupMenu.menu)

            popupMenu.menu.findItem(R.id.toCeo).isVisible = myRank == 3 && user.role != "manager"
            popupMenu.menu.findItem(R.id.toHelper).isVisible = user.role != "assistant"
            popupMenu.menu.findItem(R.id.toMember).isVisible = user.role != "participant"

            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.toCeo -> changeRole(user.userId, "manager")
                    R.id.toHelper -> changeRole(user.userId, "assistant")
                    R.id.toMember -> changeRole(user.userId, "participant")
                    R.id.kickMember -> {
                        viewModel.kick(user.userId) { ok ->
                            if (ok) reloadMembers() else toast("Произошла ошибка!")
                        }
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun changeRole(userId: Long, role: String) {
        viewModel.setRole(userId, role) { ok ->
            if (ok) reloadMembers() else toast("Произошла ошибка!")
        }
    }

    private fun reloadMembers() {
        searchMode = false
        findViewById<LinearLayout>(R.id.role3).removeAllViews()
        participantsRow = null
        viewModel.loadAll()
    }
}
