package com.gy.ecotrace.ui.more.profile.tabs

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import com.gy.ecotrace.ui.more.profile.ProfileViewModel

private const val FRIENDS_PER_ROW = 3

class ProfileTabFriends : Fragment() {

    private val viewModel: ProfileViewModel by activityViewModels()

    private fun avatarUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" }

    private fun rankUrl(rank: String): String =
        "${BuildConfig.SERVER_API_URI}uploads/\$ranks/$rank"

    private val expToRanks = mutableMapOf(
        0 to "rank1", 300 to "rank2", 700 to "rank3",
        1200 to "rank4", 1800 to "rank5", 2500 to "rank6"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_layout_profile__friends, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val friendsLayout = view.findViewById<LinearLayout>(R.id.friendsLayout)

        if (viewModel.friends.value.isNullOrEmpty() && !viewModel.foundAllFriends) {
            viewModel.loadFriends(viewModel.currentUserId)
        }
        viewModel.friends.observe(viewLifecycleOwner, Observer { friends ->
            view.findViewById<ShimmerFrameLayout>(R.id.userFriendsInformationLayoutLoading).visibility = View.GONE
            if (viewModel.newFriends) {
                friendsLayout.removeAllViews()
                viewModel.newFriends = false
            }
            val hasAny = friends.isNotEmpty() || friendsLayout.childCount > 0
            friendsLayout.visibility = if (hasAny) View.VISIBLE else View.GONE
            view.findViewById<TextView>(R.id.noFriendsWarning).visibility = if (hasAny) View.GONE else View.VISIBLE

            for (fr in friends) {
                val friendView = layoutInflater.inflate(R.layout.layout_friend__my_friend, friendsLayout, false)
                friendView.findViewById<TextView>(R.id.userUsername).apply {
                    text = fr.username
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_green))
                }
                friendView.setOnClickListener {
                    val intent = Intent(requireActivity(), ProfileActivity::class.java)
                    intent.putExtra("previousId", viewModel.currentUserId.toString())
                    Globals.getInstance().setString("CurrentlyWatching", fr.id.toString())
                    startActivity(intent)
                }
                Glide.with(this)
                    .load(avatarUrl(fr.avatarKey))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(friendView.findViewById(R.id.userProfileImage))
                currentRow(friendsLayout).addView(friendView)

                Glide.with(this)
                    .load(rankUrl(expToRanks[expToRanks.keys.filter { it <= fr.experience }.maxOrNull()] ?: "rank1"))
                    .into(friendView.findViewById(R.id.userRankImage))
            }
        })
    }

    private fun currentRow(container: LinearLayout): LinearLayout {
        val lastRow = container.getChildAt(container.childCount - 1) as? LinearLayout
        if (lastRow != null && lastRow.childCount < FRIENDS_PER_ROW) return lastRow

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(row)
        return row
    }
}
