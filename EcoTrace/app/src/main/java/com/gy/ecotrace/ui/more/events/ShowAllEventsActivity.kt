package com.gy.ecotrace.ui.more.events

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
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
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.TokenStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShowAllEventsActivity : AppCompatActivity() {

    private val showAllViewModel: ShowAllEventsViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private val renderedEventIds = HashSet<Long>()

    private fun timingLabel(timing: Int): String = when (timing) {
        0 -> "Предстоит"; 1 -> "Идёт"; else -> "Прошло"
    }

    private fun roleLabel(role: String): String = when (role) {
        "participant" -> "Участник"
        "assistant"  -> "Помощник"
        "creator"    -> "Организатор"
        else         -> role
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_all_events)
        Globals.getInstance().setString("CurrentlyWatchingEvent", "0")

        val toolbar: Toolbar = findViewById(R.id.toolbar6)
        val allEvents: LinearLayout = findViewById(R.id.allEventsLayout)
        val swipeRefresh: SwipeRefreshLayout = findViewById(R.id.allEventsSwipeRefresh)
        val eventsScrollView: ScrollView = findViewById(R.id.allEventsScrollView)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val tagsColors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
        val allEventTags: Array<Pair<String, String>> = Globals.getInstance().getEventsFilters()

        fun restartSearch() {
            findViewById<ShimmerFrameLayout>(R.id.allEventsLoadingLayout).visibility = View.VISIBLE
            allEvents.visibility = View.GONE
            findViewById<TextView>(R.id.noEventsWarning).visibility = View.GONE
            allEvents.removeAllViews()
            renderedEventIds.clear()
            showAllViewModel.search(reset = true)
        }

        supportFragmentManager.setFragmentResultListener(EventsSortBottomSheet.RESULT_KEY, this) { _, bundle ->
            showAllViewModel.sortBy = bundle.getString(EventsSortBottomSheet.ARG_SORT)
            restartSearch()
        }
        supportFragmentManager.setFragmentResultListener(EventsFiltersFragment.RESULT_KEY, this) { _, bundle ->
            showAllViewModel.filter = bundle.getString(EventsFiltersFragment.ARG_FILTER)
            showAllViewModel.from   = bundle.getString(EventsFiltersFragment.ARG_FROM)
            showAllViewModel.to     = bundle.getString(EventsFiltersFragment.ARG_TO)
            showAllViewModel.minMembers = bundle.getInt(EventsFiltersFragment.ARG_MIN_MEMBERS, -1).takeIf { it >= 0 }
            showAllViewModel.maxMembers = bundle.getInt(EventsFiltersFragment.ARG_MAX_MEMBERS, -1).takeIf { it >= 0 }
            showAllViewModel.hasMap = bundle.getBoolean(EventsFiltersFragment.ARG_HAS_MAP, false).takeIf { it }
            showAllViewModel.lat      = bundle.getDouble(EventsFiltersFragment.ARG_LAT, Double.NaN).takeIf { !it.isNaN() }
            showAllViewModel.lng      = bundle.getDouble(EventsFiltersFragment.ARG_LNG, Double.NaN).takeIf { !it.isNaN() }
            showAllViewModel.radiusKm = bundle.getDouble(EventsFiltersFragment.ARG_RADIUS_KM, Double.NaN).takeIf { !it.isNaN() }
            restartSearch()
        }

        swipeRefresh.setColorSchemeResources(R.color.ok_green)
        swipeRefresh.setOnRefreshListener {
            restartSearch()
            swipeRefresh.isRefreshing = false
        }

        val nameEntry = findViewById<EditText>(R.id.eventNameEntry)
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

        findViewById<ImageButton>(R.id.showFiltersButton).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, EventsFiltersFragment.newInstance(
                    showAllViewModel.filter,
                    showAllViewModel.from,
                    showAllViewModel.to
                ))
                .addToBackStack(EventsFiltersFragment.TAG)
                .commit()
        }
        findViewById<ImageButton>(R.id.sortByButton).setOnClickListener {
            EventsSortBottomSheet.newInstance(showAllViewModel.sortBy)
                .show(supportFragmentManager, EventsSortBottomSheet.TAG)
        }

        val tagButtons = mutableListOf<MaterialButton>()
        if (!allEventTags.isEmpty() && !tagsColors.isEmpty()) {
            for (tag in allEventTags.indices) {
                val tagButton = layoutInflater.inflate(
                    R.layout.widget_tag_filter_button,
                    null
                ) as MaterialButton
                tagButton.text = allEventTags[tag].first
                tagButton.textSize = 18F
                tagButton.isClickable = true
                tagButton.setTextColor(Color.parseColor(tagsColors[tag].second))
                tagButton.setBackgroundColor(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.transparent
                    )
                )
                tagButton.rippleColor =
                    ColorStateList.valueOf(Color.parseColor(tagsColors[tag].second))
                tagButton.strokeColor =
                    ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))

                tagButton.setOnClickListener {
                    val wasActive = tagButton.isActivated
                    tagButtons.forEach {
                        it.isActivated = false
                        it.setBackgroundColor(
                            ContextCompat.getColor(
                                applicationContext,
                                R.color.transparent
                            )
                        )
                    }
                    if (!wasActive) {
                        tagButton.isActivated = true
                        tagButton.setBackgroundColor(Color.parseColor(tagsColors[tag].first))
                        showAllViewModel.filter = allEventTags[tag].first
                    } else showAllViewModel.filter = null
                    restartSearch()
                }
                tagButtons.add(tagButton)
            }
        }

        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener { startActivity(Intent(this, CreateEventActivity::class.java)) }
        fabAdd.imageTintList = getColorStateList(R.color.dirt_white)
        fabAdd.setImageResource(R.drawable.baseline_add_24)
        fabAdd.visibility = View.GONE

        showAllViewModel.loadCurrentUser()
        showAllViewModel.userExperience.observe(this) { exp ->
            fabAdd.visibility = if ((exp ?: 0L) >= 50L) View.VISIBLE else View.GONE
        }

        showAllViewModel.search(reset = true)
        showAllViewModel.events.observe(this, Observer { events ->
            findViewById<ShimmerFrameLayout>(R.id.allEventsLoadingLayout).visibility = View.GONE
            allEvents.visibility = View.VISIBLE
            for (event in events) {
                if (!renderedEventIds.add(event.id)) continue
                val row = layoutInflater.inflate(R.layout.layout_event__search_events, allEvents, false)
                row.findViewById<TextView>(R.id.eventName).text = event.name
                row.findViewById<TextView>(R.id.eventMembers).text = event.membersCount.toString()
                row.findViewById<TextView>(R.id.eventStatus).text = timingLabel(event.timing)
                row.findViewById<View>(R.id.eventTagsScrollView).visibility = View.GONE
                row.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatchingEvent", event.id.toString())
                    startActivity(Intent(this, ShowEventActivity::class.java))
                }
                allEvents.addView(row)
            }
            findViewById<TextView>(R.id.noEventsWarning).visibility =
                if (renderedEventIds.isEmpty()) View.VISIBLE else View.GONE
        })

        val showAllMyEventsBtn = findViewById<TextView>(R.id.showAllMyEvents)
        showAllMyEventsBtn.setOnClickListener {
            startActivity(Intent(this, ShowEventsJoinedActivity::class.java))
        }

        showAllViewModel.loadMyEvents(tokenStore.userId)
        showAllViewModel.myEvents.observe(this, Observer { events ->
            val joinedScroll = findViewById<View>(R.id.userJoinedEventsScroll)
            val noMyEvents = findViewById<TextView>(R.id.noMyEventsWarning)
            if (events.isEmpty()) {
                joinedScroll.visibility = View.GONE
                showAllMyEventsBtn.visibility = View.GONE
                noMyEvents.visibility = View.VISIBLE
                return@Observer
            }
            joinedScroll.visibility = View.VISIBLE
            showAllMyEventsBtn.visibility = View.VISIBLE
            noMyEvents.visibility = View.GONE
            val joined = findViewById<LinearLayout>(R.id.userJoinedEventsLayout)
            joined.removeAllViews()
            for (event in events) {
                val row = layoutInflater.inflate(R.layout.layout_event__my_events_no_fast_action_button, joined, false)
                row.findViewById<TextView>(R.id.eventName).text = event.name
                row.findViewById<TextView>(R.id.eventStatus).text = event.status ?: ""
                row.findViewById<TextView>(R.id.eventMembers).text = event.membersCount.toString()
                row.findViewById<TextView>(R.id.eventRole).text = roleLabel(event.role)
                row.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatchingEvent", event.id.toString())
                    startActivity(Intent(this, ShowEventActivity::class.java))
                }
                joined.addView(row)
                val separator = View(applicationContext)
                separator.layoutParams = ViewGroup.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.default_PaddingMargin),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                joined.addView(separator)
            }
        })

        eventsScrollView.setOnScrollChangeListener { view, _, _, _, _ ->
            val lastChild = eventsScrollView.getChildAt(eventsScrollView.childCount - 1)
            if (lastChild != null &&
                lastChild.bottom == eventsScrollView.height + view.scrollY &&
                !showAllViewModel.foundAll && !showAllViewModel.isUpdating
            ) {
                showAllViewModel.isUpdating = true
                findViewById<ShimmerFrameLayout>(R.id.allEventsLoadingLayout).visibility = View.VISIBLE
                showAllViewModel.search(reset = false)
            }
        }
    }
}
