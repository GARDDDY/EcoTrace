package com.gy.ecotrace.ui.more.events

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.ShapeAppearanceModel
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.friends.SearcherViewModel
import com.gy.ecotrace.ui.more.friends.SearcherViewModelFactory
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import com.yandex.mapkit.search.Line
import org.w3c.dom.Text

class ShowAllEventsViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShowAllEventsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShowAllEventsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ShowAllEventsActivity : AppCompatActivity() {
    private lateinit var showAllViewModel: ShowAllEventsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_all_events)
        Globals.getInstance().setString("CurrentlyWatchingEvent", "0")
        val toolbar: Toolbar = findViewById(R.id.toolbar6)
        val allEvents: LinearLayout = findViewById(R.id.allEventsLayout)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val factory = ShowAllEventsViewModelFactory(repository)
        showAllViewModel = ViewModelProvider(this, factory)[ShowAllEventsViewModel::class.java]

        val tagsLayout: LinearLayout = findViewById(R.id.allTagsLayout)
        val tagsColors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
        val allEventTags: Array<Pair<String, String>> = Globals.getInstance().getEventsFilters()
        for (tag in allEventTags.indices) {
            val tagButton = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
            tagButton.text = allEventTags[tag].first
            tagButton.textSize = 18F
            tagButton.isClickable = true
            Log.w("testcolors", tagsColors[tag].toString())
            Log.w("testcolors", tagsColors[tag].second.toString())
            tagButton.setTextColor(Color.parseColor(tagsColors[tag].second))
            tagButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.transparent))
            tagButton.rippleColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].second))
            tagButton.strokeColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))

            tagButton.setOnClickListener {
                Log.d("btn", "clicked")
                tagButton.isActivated = !tagButton.isActivated

                findViewById<ShimmerFrameLayout>(R.id.allEventsLoadingLayout).visibility = View.VISIBLE
                allEvents.visibility = View.GONE
                findViewById<TextView>(R.id.noEventsWarning).visibility = View.GONE
                allEvents.removeAllViews()

                showAllViewModel.reapplyFilter(tag)
                showAllViewModel.getEvents()
                if(!tagButton.isActivated) tagButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.transparent))
                else tagButton.setBackgroundColor(Color.parseColor(tagsColors[tag].first))
            }

            tagsLayout.addView(tagButton)
        }

        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener {
            startActivity(
                Intent(this@ShowAllEventsActivity, CreateEventActivity::class.java)
            )
        }
        fabAdd.imageTintList = getColorStateList(R.color.dirt_white)
        fabAdd.setImageResource(R.drawable.baseline_add_24)


        showAllViewModel.getEvents()
        showAllViewModel.eventsFound.observe(this, Observer {
            findViewById<ShimmerFrameLayout>(R.id.allEventsLoadingLayout).visibility = View.GONE
            allEvents.visibility = View.VISIBLE
            for (event in it) {
                val eventLayout = layoutInflater.inflate(R.layout.layout_event_in_all_events, null)
                eventLayout.findViewById<TextView>(R.id.eventName).text = event.eventName
                eventLayout.findViewById<TextView>(R.id.eventDescription).text = event.eventAbout ?: "Нет описания"
                eventLayout.findViewById<TextView>(R.id.eventMembers).text = event.eventCountMembers.toString()
                eventLayout.findViewById<TextView>(R.id.eventCreator).text = event.eventCreatorName

                eventLayout.findViewById<TextView>(R.id.eventStatus).text = when(event.eventStatus) {
                    0 -> "Еще не началось"
                    1 -> "Уже проходит"
                    2 -> "Закончилось"
                    else -> "unreal-event-status"
                }

                eventLayout.findViewById<LinearLayout>(R.id.creatorLayout).setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", event.eventCreatorId)
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                    )
                }

                if (event.filters != "") {
                    val eventTagsLayout = eventLayout.findViewById<LinearLayout>(R.id.eventTags)
                    for (tag in event.filters.split(',').sorted()) {
                        val tagInt = tag.toInt() - 1
                        val tagButton = layoutInflater.inflate(
                            R.layout.widget_tag_filter_button,
                            null
                        ) as MaterialButton
                        tagButton.text = allEventTags[tagInt].first
                        tagButton.textSize = 18F
                        tagButton.setTextColor(Color.parseColor(tagsColors[tagInt].second))
                        tagButton.setBackgroundColor(Color.parseColor(tagsColors[tagInt].first))
                        tagButton.isClickable = false
                        eventTagsLayout.addView(tagButton)
                    }
                }


                Glide.with(this)
                    .load(Globals().getImgUrl("events", event.eventId))
                    .into(eventLayout.findViewById(R.id.eventImage))

                Glide.with(this)
                    .load(Globals().getImgUrl("users", event.eventCreatorId))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(eventLayout.findViewById(R.id.eventCreatorImage))


                eventLayout.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatchingEvent", event.eventId)
                    startActivity(
                        Intent(
                            this@ShowAllEventsActivity,
                            ShowEventActivity::class.java
                        )
                    )
                }

                allEvents.addView(eventLayout)
            }
            if (it.isEmpty()) {
                findViewById<TextView>(R.id.noEventsWarning).visibility = View.VISIBLE
            }
        })

        showAllViewModel.getUserEvents()
        showAllViewModel.events.observe(this, Observer {
            it?.let{
                findViewById<LinearLayout>(R.id.userJoinedEvents).visibility = View.VISIBLE
                val joinedEvents = findViewById<LinearLayout>(R.id.userJoinedEventsLayout)
                for ((eventId, eventName) in it) {
                    val eventLayout = layoutInflater.inflate(R.layout.layout_joined_event_short, null)
                    eventLayout.findViewById<TextView>(R.id.eventName).text = eventName

                    Glide.with(this)
                    .load(Globals().getImgUrl("events", eventId))
                    .into(eventLayout.findViewById(R.id.eventImage))


                    eventLayout.setOnClickListener {
                        Globals.getInstance().setString("CurrentlyWatchingEvent", eventId)
                        startActivity(Intent(this@ShowAllEventsActivity, ShowEventActivity::class.java))
                    }

                    joinedEvents.addView(eventLayout)
                    val separator = View(applicationContext)
                    separator.layoutParams = ViewGroup.LayoutParams(resources.getDimensionPixelSize(R.dimen.default_PaddingMargin),
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                    joinedEvents.addView(separator)
                }
            }
        })

        val eventsScrollView: ScrollView = findViewById(R.id.allEventsScrollView)
        eventsScrollView.setOnScrollChangeListener { view, _,_,_,_ ->
            if (eventsScrollView.getChildAt(eventsScrollView.childCount-1).bottom == eventsScrollView.height+view.scrollY
                    && !showAllViewModel.foundAll && !showAllViewModel.isUpdating) {
                Log.d("updating", "true")
                showAllViewModel.isUpdating = true
                findViewById<ShimmerFrameLayout>(R.id.allEventsLoadingLayout).visibility = View.VISIBLE
                showAllViewModel.getEvents(false)
            }
        }
    }
}