package com.gy.ecotrace.ui.more.profile.tabs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.UserGroupItem
import com.gy.ecotrace.ui.more.groups.ShowGroupActivity
import com.gy.ecotrace.ui.more.profile.ProfileViewModel

private const val GROUPS_PER_ROW = 3

fun showGroupsSortMenu(context: Context, anchor: View, viewModel: ProfileViewModel) {
    val popup = PopupMenu(context, anchor)
    popup.menuInflater.inflate(R.menu.popup_menu_profile_events_sort, popup.menu)
    popup.setOnMenuItemClickListener { item ->
        val position = if (item.itemId == R.id.sortByMembers) 1 else 0
        if (viewModel.lastGroupSort != position) {
            viewModel.lastGroupSort = position
            viewModel.loadGroups(viewModel.currentUserId, if (position == 1) "members" else null, true)
        }
        true
    }
    popup.show()
}

class ProfileTabGroups : Fragment() {

    private val viewModel: ProfileViewModel by activityViewModels()

    private fun groupImageUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/groups/$it" }

    private fun membersLabel(count: Int): String {
        val ending = when {
            count % 100 in 11..14 -> "ов"
            count % 10 == 1 -> ""
            count % 10 in 2..4 -> "а"
            else -> "ов"
        }
        return "$count участник$ending"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_layout_profile__groups, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupsLayout = view.findViewById<LinearLayout>(R.id.groupsLayout)

        if (viewModel.groups.value.isNullOrEmpty() && !viewModel.foundAllGroups) {
            viewModel.loadGroups(viewModel.currentUserId, groupSortParam(viewModel.lastGroupSort))
        }
        viewModel.groups.observe(viewLifecycleOwner, Observer { groups ->
            view.findViewById<ShimmerFrameLayout>(R.id.userGroupsInformationLayoutLoading).visibility = View.GONE
            if (viewModel.newGroups) {
                groupsLayout.removeAllViews()
                viewModel.newGroups = false
            }
            val hasAny = groups.isNotEmpty() || groupsLayout.childCount > 0
            groupsLayout.visibility = if (hasAny) View.VISIBLE else View.GONE
            view.findViewById<TextView>(R.id.noGroupsWarning).visibility = if (hasAny) View.GONE else View.VISIBLE

            for (group in groups) addGroupCard(groupsLayout, group)
        })
    }

    private fun groupSortParam(position: Int): String? = if (position == 1) "members" else null

    private fun addGroupCard(container: LinearLayout, group: UserGroupItem) {
        val card = layoutInflater.inflate(R.layout.layout_group__my_groups_no_fast_action_button, container, false)
        card.findViewById<TextView>(R.id.groupName).text = group.name
        card.findViewById<TextView>(R.id.groupMembers).text = membersLabel(group.membersCount)
        Glide.with(this).load(groupImageUrl(group.rulesImageKey))
            .placeholder(R.drawable.outline_rectangle_24)
            .into(card.findViewById(R.id.groupImage))
        card.setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatchingGroup", group.id.toString())
            startActivity(Intent(requireActivity(), ShowGroupActivity::class.java))
        }
        (card.layoutParams as? LinearLayout.LayoutParams)?.let {
            val margin = resources.getDimensionPixelSize(R.dimen.default_PaddingMargin) / 2
            it.marginStart = margin
            it.marginEnd = margin
            card.layoutParams = it
        }
        currentRow(container).addView(card)
    }

    private fun currentRow(container: LinearLayout): LinearLayout {
        val lastRow = container.getChildAt(container.childCount - 1) as? LinearLayout
        if (lastRow != null && lastRow.childCount < GROUPS_PER_ROW) return lastRow

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.default_PaddingMargin) }
        }
        container.addView(row)
        return row
    }
}
