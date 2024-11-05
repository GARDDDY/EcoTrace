package com.gy.ecotrace.ui.more.groups

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.profile.ProfileActivity

class ShowAllGroupsViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShowAllGroupsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShowAllGroupsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ShowAllGroupsActivity : AppCompatActivity() {

    private lateinit var showAllViewModel: ShowAllGroupsViewModel
    private val currentUser = ETAuth.getInstance().getUID() // ?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_all_groups)

        val allGroups: LinearLayout = findViewById(R.id.allGroupsLayout)

        val toolbar = findViewById<Toolbar>(R.id.toolbar6)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }


        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val factory = ShowAllGroupsViewModelFactory(repository)
        showAllViewModel = ViewModelProvider(this, factory)[ShowAllGroupsViewModel::class.java]

        val tagsLayout: LinearLayout = findViewById(R.id.allTagsLayout)

        val tagsColors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
        val allGroupTags: Array<Pair<String, String>> = Globals.getInstance().getGroupsFilters()
        for (tag in allGroupTags.indices) {
            val tagButton = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
            tagButton.text = allGroupTags[tag].first
            tagButton.textSize = 18F
            tagButton.isClickable = true
            tagButton.setTextColor(Color.parseColor(tagsColors[tag].second))
            tagButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.transparent))
            tagButton.rippleColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].second))
            tagButton.strokeColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))

            tagButton.setOnClickListener {
                Log.d("btn", "clicked")
                tagButton.isActivated = !tagButton.isActivated

                findViewById<ShimmerFrameLayout>(R.id.allGroupsLoadingLayout).visibility = View.VISIBLE
                allGroups.visibility = View.GONE
                findViewById<TextView>(R.id.noGroupsWarning).visibility = View.GONE
                allGroups.removeAllViews()

                showAllViewModel.reapplyFilter(tag)
                showAllViewModel.getGroups()
                if(!tagButton.isActivated) tagButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.transparent))
                else tagButton.setBackgroundColor(Color.parseColor(tagsColors[tag].first))
            }

            tagsLayout.addView(tagButton)
        }

        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener {
            startActivity(
                Intent(this@ShowAllGroupsActivity, CreateGroupActivity::class.java)
            )
        }
        fabAdd.imageTintList = getColorStateList(R.color.dirt_white)
        fabAdd.setImageResource(R.drawable.baseline_add_24)


        showAllViewModel.getGroups()
        showAllViewModel.groupsFound.observe(this, Observer {
            findViewById<ShimmerFrameLayout>(R.id.allGroupsLoadingLayout).visibility = View.GONE
            allGroups.visibility = View.VISIBLE
            for (group in it) {
                val groupLayout = layoutInflater.inflate(R.layout.layout_group_in_all_groups, null)
                groupLayout.findViewById<TextView>(R.id.groupName).text = group.groupName
                groupLayout.findViewById<TextView>(R.id.groupDescription).text = group.groupAbout ?: "Нет описания"
                groupLayout.findViewById<TextView>(R.id.groupMembers).text = group.groupCountMembers.toString()
                groupLayout.findViewById<TextView>(R.id.groupCreator).text = group.groupCreatorName

                groupLayout.findViewById<LinearLayout>(R.id.creatorLayout).setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", group.groupCreatorId)
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                    )
                }

                if (group.filters != "") {
                    val groupTagsLayout = groupLayout.findViewById<LinearLayout>(R.id.groupTags)
                    for (tag in group.filters.split(',').sorted()) {
                        val tagInt = tag.toInt() - 1
                        val tagButton = layoutInflater.inflate(
                            R.layout.widget_tag_filter_button,
                            null
                        ) as MaterialButton
                        tagButton.text = allGroupTags[tagInt].first
                        tagButton.textSize = 18F
                        tagButton.setTextColor(Color.parseColor(tagsColors[tagInt].second))
                        tagButton.setBackgroundColor(Color.parseColor(tagsColors[tagInt].first))
                        tagButton.isClickable = false
                        groupTagsLayout.addView(tagButton)
                    }
                }


                Glide.with(this)
                    .load(Globals().getImgUrl("groups", group.groupId))
                    .into(groupLayout.findViewById(R.id.groupImage))

                Glide.with(this)
                    .load(Globals().getImgUrl("users", group.groupCreatorId))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(groupLayout.findViewById(R.id.groupCreatorImage))


                groupLayout.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatchingGroup", group.groupId)
                    startActivity(
                        Intent(
                            this@ShowAllGroupsActivity,
                            ShowGroupActivity::class.java
                        )
                    )
                }

                allGroups.addView(groupLayout)
            }
            if (it.isEmpty()) {
                findViewById<TextView>(R.id.noGroupsWarning).visibility = View.VISIBLE
            }
        })

        showAllViewModel.getUserGroups(currentUser)
        showAllViewModel.userGroups.observe(this, Observer {
            it?.let{
                if (it.size != 0) {
                    findViewById<LinearLayout>(R.id.userJoinedGroups).visibility = View.VISIBLE
                }
                val joinedGroups = findViewById<LinearLayout>(R.id.userJoinedGroupsLayout)
                for (group in it) {
                    val groupLayout = layoutInflater.inflate(R.layout.layout_joined_group_short, null)
                    groupLayout.findViewById<TextView>(R.id.groupName).text = group.groupInfo.groupName

                    Glide.with(this)
                        .load(Globals().getImgUrl("groups", group.groupInfo.groupId))
                        .into(groupLayout.findViewById(R.id.groupImage))


                    groupLayout.setOnClickListener {
                        Globals.getInstance().setString("CurrentlyWatchingGroup", group.groupInfo.groupId)
                        startActivity(Intent(this@ShowAllGroupsActivity, ShowGroupActivity::class.java))
                    }

                    joinedGroups.addView(groupLayout)
                    val separator = View(applicationContext)
                    separator.layoutParams = ViewGroup.LayoutParams(resources.getDimensionPixelSize(R.dimen.default_PaddingMargin),
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                    joinedGroups.addView(separator)
                }
            }
        })

        val groupsScrollView: ScrollView = findViewById(R.id.allGroupsScrollView)
        groupsScrollView.setOnScrollChangeListener { view, _,_,_,_ ->
            if (groupsScrollView.getChildAt(groupsScrollView.childCount-1).bottom == groupsScrollView.height+view.scrollY
                && !showAllViewModel.foundAll && !showAllViewModel.isUpdating) {
                Log.d("updating", "true")
                showAllViewModel.isUpdating = true
                findViewById<ShimmerFrameLayout>(R.id.allGroupsLoadingLayout).visibility = View.VISIBLE
                showAllViewModel.getGroups(false)
            }
        }
    }


}