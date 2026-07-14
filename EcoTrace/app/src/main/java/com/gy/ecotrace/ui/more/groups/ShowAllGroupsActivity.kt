package com.gy.ecotrace.ui.more.groups

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShowAllGroupsActivity : AppCompatActivity() {

    private val showAllViewModel: ShowAllGroupsViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private val renderedGroupIds = HashSet<Long>()

    private fun groupImageUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/groups/$it" }

    private fun userImageUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" }

    private fun statusIcon(type: String): Int =
        if (type == "closed") R.drawable.round_lock_24 else R.drawable.round_lock_open_24

    private fun statusColor(type: String): Int =
        if (type == "closed") R.color.red_no else R.color.ok_green

    private fun statusLabel(type: String): String =
        if (type == "closed") "Закрытая" else "Открытая"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_all_groups)

        val allGroups: LinearLayout = findViewById(R.id.allGroupsLayout)

        val toolbar = findViewById<Toolbar>(R.id.toolbar6)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val swipeRefresh: SwipeRefreshLayout = findViewById(R.id.allGroupsSwipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.ok_green)

        fun restartSearch() {
            findViewById<ShimmerFrameLayout>(R.id.allGroupsLoadingLayout).visibility = View.VISIBLE
            allGroups.visibility = View.GONE
            findViewById<TextView>(R.id.noGroupsWarning).visibility = View.GONE
            allGroups.removeAllViews()
            renderedGroupIds.clear()
            showAllViewModel.search(reset = true)
            showAllViewModel.loadUserGroups(tokenStore.userId)
        }

        swipeRefresh.setOnRefreshListener {
            restartSearch()
            swipeRefresh.isRefreshing = false
        }

        supportFragmentManager.setFragmentResultListener(GroupsSortBottomSheet.RESULT_KEY, this) { _, bundle ->
            showAllViewModel.sortBy = bundle.getString(GroupsSortBottomSheet.ARG_SORT)
            restartSearch()
        }

        val nameEntry = findViewById<EditText>(R.id.groupNameEntry)
        val searchHandler = Handler(Looper.getMainLooper())
        val searchRunnable = Runnable {
            showAllViewModel.name = nameEntry.text.toString().takeIf { it.isNotBlank() }
            restartSearch()
        }
        nameEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchHandler.removeCallbacks(searchRunnable)
                searchHandler.postDelayed(searchRunnable, 500)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<ImageButton>(R.id.sortByButton).setOnClickListener {
            GroupsSortBottomSheet.newInstance(showAllViewModel.sortBy)
                .show(supportFragmentManager, GroupsSortBottomSheet.TAG)
        }

        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener { startActivity(Intent(this, CreateGroupActivity::class.java)) }
        fabAdd.imageTintList = getColorStateList(R.color.dirt_white)
        fabAdd.setImageResource(R.drawable.baseline_add_24)
        fabAdd.visibility = View.GONE

        showAllViewModel.loadCurrentUser()
        showAllViewModel.userExperience.observe(this) { exp ->
            fabAdd.visibility = if ((exp ?: 0L) >= 100L) View.VISIBLE else View.GONE
        }

        showAllViewModel.search(reset = true)
        showAllViewModel.groups.observe(this, Observer { groups ->
            findViewById<ShimmerFrameLayout>(R.id.allGroupsLoadingLayout).visibility = View.GONE
            allGroups.visibility = View.VISIBLE
            for (group in groups) {
                if (!renderedGroupIds.add(group.id)) continue
                val groupLayout = layoutInflater.inflate(R.layout.layout_group__search_groups, null)
                groupLayout.findViewById<TextView>(R.id.groupName).text = group.name
                groupLayout.findViewById<TextView>(R.id.groupDescription).text =
                    group.about ?: "Нет описания"
                groupLayout.findViewById<TextView>(R.id.groupMembers).text = group.membersCount.toString()
                groupLayout.findViewById<TextView>(R.id.groupCreator).text = group.creatorUsername

                groupLayout.findViewById<ImageView>(R.id.groupStatusVectorImage).apply {
                    setImageResource(statusIcon(group.type))
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, statusColor(group.type)))
                }
                groupLayout.findViewById<TextView>(R.id.groupStatus).text = statusLabel(group.type)

                groupLayout.findViewById<LinearLayout>(R.id.creatorLayout).setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", group.creatorId.toString())
                    startActivity(Intent(this, ProfileActivity::class.java))
                }

                Glide.with(this).load(groupImageUrl(group.rulesImageKey))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(groupLayout.findViewById(R.id.groupImage))
                Glide.with(this).load(userImageUrl(group.creatorAvatarKey))
                    .placeholder(R.drawable.baseline_person_24)
                    .circleCrop()
                    .into(groupLayout.findViewById(R.id.groupCreatorImage))

                groupLayout.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatchingGroup", group.id.toString())
                    startActivity(Intent(this, ShowGroupActivity::class.java))
                }
                allGroups.addView(groupLayout)
            }
            findViewById<TextView>(R.id.noGroupsWarning).visibility =
                if (renderedGroupIds.isEmpty()) View.VISIBLE else View.GONE
        })

        findViewById<TextView>(R.id.showAllMyGroups).setOnClickListener {
            startActivity(Intent(this, ShowGroupsJoinedActivity::class.java))
        }

        showAllViewModel.loadUserGroups(tokenStore.userId)
        showAllViewModel.userGroups.observe(this, Observer { groups ->
            if (groups.isEmpty()) return@Observer
            findViewById<LinearLayout>(R.id.userJoinedGroups).visibility = View.VISIBLE
            val joinedGroups = findViewById<LinearLayout>(R.id.userJoinedGroupsLayout)
            joinedGroups.removeAllViews()
            for (group in groups) {
                val groupLayout = layoutInflater.inflate(R.layout.layout_group__my_groups_no_fast_action_button, null)
                groupLayout.findViewById<TextView>(R.id.groupName).text = group.name
                Glide.with(this).load(groupImageUrl(group.rulesImageKey))
                    .placeholder(R.drawable.baseline_person_24)
                    .into(groupLayout.findViewById(R.id.groupImage))
                groupLayout.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatchingGroup", group.id.toString())
                    startActivity(Intent(this, ShowGroupActivity::class.java))
                }
                joinedGroups.addView(groupLayout)
                val separator = View(applicationContext)
                separator.layoutParams = ViewGroup.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.default_PaddingMargin),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                joinedGroups.addView(separator)
            }
        })

        val groupsScrollView: ScrollView = findViewById(R.id.allGroupsScrollView)
        groupsScrollView.setOnScrollChangeListener { view, _, _, _, _ ->
            if (groupsScrollView.getChildAt(groupsScrollView.childCount - 1).bottom ==
                groupsScrollView.height + view.scrollY &&
                !showAllViewModel.foundAll && !showAllViewModel.isUpdating
            ) {
                showAllViewModel.isUpdating = true
                findViewById<ShimmerFrameLayout>(R.id.allGroupsLoadingLayout).visibility = View.VISIBLE
                showAllViewModel.search(reset = false)
            }
        }
    }
}
