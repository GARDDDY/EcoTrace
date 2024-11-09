package com.gy.ecotrace.ui.more.groups

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.dto.UserGroupItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShowGroupsJoinedActivity : AppCompatActivity() {

    private val viewModel: ShowAllGroupsViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private var selectedRole: String? = null

    private lateinit var chipAll: Chip
    private lateinit var chipCreator: Chip
    private lateinit var chipManager: Chip
    private lateinit var chipAssistant: Chip
    private lateinit var chipParticipant: Chip
    private lateinit var notFoundGroups: TextView

    private val pendingLeaveIds = mutableSetOf<Long>()

    private fun groupImageUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/groups/$it" }

    private fun roleLabel(role: String): String = when (role) {
        "participant" -> "Участник"
        "assistant"   -> "Помощник"
        "manager"     -> "Руководитель"
        "creator"     -> "Организатор"
        else          -> role
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_groups_joined)

        val toolbar = findViewById<Toolbar>(R.id.toolbar11)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        chipAll         = findViewById(R.id.allRoles)
        chipCreator     = findViewById(R.id.creatorRole)
        chipManager     = findViewById(R.id.managerRole)
        chipAssistant   = findViewById(R.id.assistantRole)
        chipParticipant = findViewById(R.id.participantRole)
        notFoundGroups  = findViewById(R.id.textNotFoundGroups)
        val container = findViewById<LinearLayout>(R.id.relevantGroups)

        val allChips = listOf(chipAll, chipCreator, chipManager, chipAssistant, chipParticipant)
        selectChip(chipAll)
        allChips.forEach { chip ->
            chip.setOnClickListener { selectChip(chip); rebind(container, viewModel.userGroups.value ?: emptyList()) }
        }

        viewModel.userGroups.observe(this) { groups -> rebind(container, groups) }
        viewModel.loadUserGroups(tokenStore.userId)
    }

    private fun selectChip(chip: Chip) {
        listOf(chipAll, chipCreator, chipManager, chipAssistant, chipParticipant).forEach {
            it.backgroundTintList = getColorStateList(R.color.dirt_white)
        }
        chip.backgroundTintList = getColorStateList(R.color.dirt2_white)
        selectedRole = when (chip.id) {
            R.id.creatorRole     -> "creator"
            R.id.managerRole     -> "manager"
            R.id.assistantRole   -> "assistant"
            R.id.participantRole -> "participant"
            else                 -> null
        }
    }

    private fun rebind(container: LinearLayout, groups: List<UserGroupItem>) {
        container.removeAllViews()
        val filtered = groups.filter { selectedRole == null || it.role == selectedRole }
        notFoundGroups.isVisible = filtered.isEmpty()
        for (group in filtered) {
            val row = layoutInflater.inflate(R.layout.layout_group__my_groups_with_fast_action_button, container, false)
            row.findViewById<TextView>(R.id.groupName).text = group.name
            row.findViewById<TextView>(R.id.groupDescription).text = group.about ?: "Нет описания"
            row.findViewById<TextView>(R.id.groupMembers).text = group.membersCount.toString()
            row.findViewById<TextView>(R.id.groupRole).text = roleLabel(group.role)
            Glide.with(this).load(groupImageUrl(group.rulesImageKey))
                .placeholder(R.drawable.baseline_person_24)
                .into(row.findViewById(R.id.groupImage))

            row.setOnClickListener {
                Globals.getInstance().setString("CurrentlyWatchingGroup", group.id.toString())
                startActivity(Intent(this, ShowGroupActivity::class.java))
            }

            val leaveBtn = row.findViewById<MaterialButton>(R.id.leaveGroup)
            if (group.role != "creator") {
                leaveBtn.visibility = View.VISIBLE
                applyLeaveButtonState(leaveBtn, group.id)
                leaveBtn.setOnClickListener {
                    if (!pendingLeaveIds.add(group.id)) pendingLeaveIds.remove(group.id)
                    applyLeaveButtonState(leaveBtn, group.id)
                }
            } else {
                leaveBtn.visibility = View.GONE
            }

            container.addView(row)
        }
    }

    private fun applyLeaveButtonState(btn: MaterialButton, groupId: Long) {
        if (pendingLeaveIds.contains(groupId)) {
            btn.text = "Присоединиться"
            btn.backgroundTintList = getColorStateList(R.color.ok_green)
            btn.setTextColor(getColor(R.color.dirt_white))
        } else {
            btn.text = "Покинуть"
            btn.backgroundTintList = getColorStateList(R.color.dirt_white)
            btn.setTextColor(getColor(R.color.ok_green))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (groupId in pendingLeaveIds) viewModel.leaveGroup(groupId) {}
    }
}
