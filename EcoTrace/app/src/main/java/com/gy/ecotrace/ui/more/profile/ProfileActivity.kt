package com.gy.ecotrace.ui.more.profile

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.tabs.TabLayout
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.NetworkMonitor
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.ui.more.profile.change.ChangeProfile
import com.gy.ecotrace.ui.more.profile.tabs.ProfileTabActivity
import com.gy.ecotrace.ui.more.profile.tabs.ProfileTabCalculator
import com.gy.ecotrace.ui.more.profile.tabs.ProfileTabFriends
import com.gy.ecotrace.ui.more.profile.tabs.ProfileTabGroups
import com.gy.ecotrace.ui.more.profile.tabs.showEventsSortMenu
import com.gy.ecotrace.ui.more.profile.tabs.showGroupsSortMenu
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ACTIVITY_TAB_INDEX = 1
private const val FRIENDS_TAB_INDEX = 2
private const val GROUPS_TAB_INDEX = 3

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    private val userViewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private var currentUser: Long = -1L
    private val loggedUser: Long get() = tokenStore.userId
    private val isSelf: Boolean get() = currentUser == loggedUser

    private lateinit var toolbar: Toolbar
    private lateinit var scrollView: NestedScrollView
    private lateinit var tabView: TabLayout
    private lateinit var tabViewSticky: TabLayout

    private var isHeaderCollapsed = false
    private var currentTabIndex = 0
    private var currentUsername: String = ""
    private var friendButtonWanted = false

    private val tabTitles = arrayOf("Калькулятор", "Активность", "Друзья", "Группы")

    private fun updateTabTitle(position: Int, count: Int) {
        val title = "${tabTitles[position]} ($count)"
        tabView.getTabAt(position)?.text = title
        tabViewSticky.getTabAt(position)?.text = title
    }

    private fun userAvatarUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" }

    private fun rankUrl(rank: String): String =
        "${BuildConfig.SERVER_API_URI}uploads/\$ranks/$rank"

    private fun setupFriendButton() {
        val setFriend = findViewById<ImageButton>(R.id.setFriend)
        userViewModel.loadFriendship(currentUser)
        userViewModel.friendship.observe(this, Observer { code ->
            when (code) {
                // друзья
                3 -> {
                    setFriend.setImageResource(R.drawable.baseline_person_remove_24)
                    setFriend.imageTintList =
                        ContextCompat.getColorStateList(applicationContext, R.color.red_no)
                    setFriend.setOnClickListener {
                        confirm("Удаление из друзей",
                            "Вы действительно хотите удалить этого пользователя из списка своих друзей?") {
                            userViewModel.removeFriend(currentUser); regetProfile()
                        }
                    }
                }

                // нет связи
                0 -> {
                    setFriend.setImageResource(R.drawable.baseline_person_add_24)
                    setFriend.imageTintList =
                        ContextCompat.getColorStateList(applicationContext, R.color.ok_green)
                    setFriend.setOnClickListener {
                        confirm("Добавление в друзья",
                            "Отправить этому пользователю запрос в друзья?") {
                            userViewModel.addFriend(currentUser); regetProfile()
                        }
                    }
                }

                // заявка отправлена
                1 -> {
                    setFriend.setImageResource(R.drawable.baseline_person_remove_24)
                    setFriend.imageTintList =
                        ContextCompat.getColorStateList(applicationContext, R.color.red_no)
                    setFriend.setOnClickListener {
                        confirm("Отмена запроса",
                            "Отменить свой запрос в друзья?") {
                            userViewModel.removeFriend(currentUser); regetProfile()
                        }
                    }
                }

                // 2: входящая заявка
                else -> {
                    setFriend.setImageResource(R.drawable.baseline_person_24)
                    setFriend.imageTintList =
                        ContextCompat.getColorStateList(applicationContext, R.color.silver)
                    setFriend.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Запрос на добавление в друзья")
                            .setMessage("Вы хотите принять или отклонить запрос в друзья?")
                            .setPositiveButton("Принять") { _, _ ->
                                userViewModel.addFriend(currentUser); regetProfile()
                            }
                            .setNegativeButton("Отклонить") { _, _ ->
                                userViewModel.removeFriend(currentUser); regetProfile()
                            }
                            .setNeutralButton("Отмена") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                }
            }
            friendButtonWanted = true
            applyToolbarCollapseState()
        })
    }

    // Toolbar.inflateMenu() добавляет пункты поверх старых, а не заменяет их — чистим меню перед инфлейтом.
    private fun showDefaultToolbarMenu() {
        toolbar.menu.clear()
        if (isSelf) {
            toolbar.inflateMenu(R.menu.popup_menu_profile)
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_settings -> {
                        startActivity(Intent(this, ChangeProfile::class.java))
                        true
                    }

                    R.id.action_logout -> {
                        AlertDialog.Builder(this)
                            .setTitle("Выход из аккаунта")
                            .setMessage("Вы действительно хотите выйти из своего аккаунта?")
                            .setPositiveButton("Подтвердить") { _, _ ->
                                ETAuth.getInstance().logout()
                                finish()
                            }
                            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
                            .show()
                        true
                    }

                    else -> super.onOptionsItemSelected(it)
                }
            }
        }
    }

    private fun showSortToolbarMenu() {
        toolbar.menu.clear()
    }

    private fun showTab(position: Int) {
        val fragment: Fragment = when (position) {
            0 -> ProfileTabCalculator()
            1 -> ProfileTabActivity()
            2 -> ProfileTabFriends()
            else -> ProfileTabGroups()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.tabContentContainer, fragment)
            .commit()
    }

    private fun applyHeaderState() {
        applyToolbarCollapseState()
        userViewModel.setContentId(if (isHeaderCollapsed) currentTabIndex else 0)
        tabViewSticky.visibility = if (isHeaderCollapsed) View.VISIBLE else View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        currentUser = Globals.getInstance().getString("CurrentlyWatching").toLongOrNull() ?: loggedUser
        userViewModel.currentUserId = currentUser
        userViewModel.isSelf = isSelf

        scrollView = findViewById(R.id.fullProfileScrollView)

        toolbar = findViewById(R.id.toolbarProfile)
        val backIcon =
            ContextCompat.getDrawable(applicationContext, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(
            ContextCompat.getColor(applicationContext, R.color.ok_green),
            PorterDuff.Mode.SRC_ATOP
        )
        toolbar.navigationIcon = backIcon
        toolbar.setNavigationOnClickListener { onBackPressed() }

        showDefaultToolbarMenu()
        if (!isSelf) setupFriendButton()

        userViewModel.loadProfile(currentUser, isSelf)

        userViewModel.profile.observe(this, Observer { user ->
            findViewById<ShimmerFrameLayout>(R.id.userMainInformationLayoutLoading).visibility =
                View.GONE
            if (user == null) {
                findViewById<RelativeLayout>(R.id.userMainInformationLayout).visibility = View.GONE
                android.widget.Toast.makeText(
                    this, "Не удалось загрузить профиль", android.widget.Toast.LENGTH_SHORT
                ).show()
                return@Observer
            }

            findViewById<RelativeLayout>(R.id.userMainInformationLayout).visibility = View.VISIBLE

            currentUsername = user.username
            findViewById<TextView>(R.id.userUsernameProfile).text = user.username
            findViewById<TextView>(R.id.toolbarUsername).text = user.username

            Glide.with(this)
                .load(userAvatarUrl(user.avatarKey))
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(findViewById(R.id.userImageProfile))

            val about = findViewById<TextView>(R.id.userAboutProfile)
            about.text = user.aboutMe
            about.setOnClickListener {
                about.isActivated = !about.isActivated
                about.maxLines = if (about.isActivated) Int.MAX_VALUE else 3
            }
            findViewById<TextView>(R.id.userExperienceProfile).text = user.experience.toString()

            val expToRanks = mutableMapOf(
                0 to "rank1", 300 to "rank2", 700 to "rank3",
                1200 to "rank4", 1800 to "rank5", 2500 to "rank6"
            )
            val rankName = expToRanks[expToRanks.keys.filter { it <= user.experience }.maxOrNull()] ?: "rank1"
            Glide.with(this)
                .load(rankUrl(rankName))
                .into(findViewById(R.id.userExperienceImageProfile))

            val flagEmoji = getCountryEmoji(user.countryCode)
            val flagTextView = findViewById<TextView>(R.id.userCountryFlag)
            if (flagEmoji.isNullOrEmpty()) flagTextView.visibility = View.GONE
            else flagTextView.text = flagEmoji
        })

        tabView = findViewById(R.id.tabLayout)
        tabViewSticky = findViewById(R.id.tabLayoutSticky)

        userViewModel.loadEvents(currentUser, if (userViewModel.lastEventSort == 1) "members" else null)
        userViewModel.loadFriends(currentUser)
        userViewModel.loadGroups(currentUser)
        userViewModel.events.observe(this, Observer { updateTabTitle(ACTIVITY_TAB_INDEX, it.size) })
        userViewModel.friends.observe(this, Observer { updateTabTitle(FRIENDS_TAB_INDEX, it.size) })
        userViewModel.groups.observe(this, Observer { updateTabTitle(GROUPS_TAB_INDEX, it.size) })

        tabView.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTabIndex = tab.position
                scrollView.scrollTo(0, 0)
                showTab(tab.position)
                applyHeaderState()
                if (tabViewSticky.selectedTabPosition != tab.position) {
                    tabViewSticky.getTabAt(tab.position)?.select()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        tabViewSticky.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tabView.selectedTabPosition != tab.position) {
                    tabView.getTabAt(tab.position)?.select()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        currentTabIndex = tabView.selectedTabPosition.coerceAtLeast(0)
        showTab(currentTabIndex)

        val headerWrapper = findViewById<View>(R.id.profileHeaderWrapper)
        scrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                val collapsed = scrollY >= headerWrapper.height
                if (collapsed != isHeaderCollapsed) {
                    isHeaderCollapsed = collapsed
                    applyHeaderState()
                }
            }
        )

        userViewModel.contentId.observe(this, Observer { content ->
            if (content == 0) {
                showDefaultToolbarMenu()
            } else {
                showSortToolbarMenu()
            }
        })

        findViewById<ImageButton>(R.id.toolbarFilterButton).setOnClickListener {
            when (currentTabIndex) {
                ACTIVITY_TAB_INDEX -> showEventsSortMenu(this, it, userViewModel)
                GROUPS_TAB_INDEX -> showGroupsSortMenu(this, it, userViewModel)
            }
        }

        val refresh = findViewById<SwipeRefreshLayout>(R.id.refreshProfile)
        refresh.setOnRefreshListener {
            regetProfile()
            refresh.isRefreshing = false
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var wasOnline = NetworkMonitor.isOnline.value
                NetworkMonitor.isOnline.collect { online ->
                    if (online && !wasOnline) refetchOnReconnect()
                    wasOnline = online
                }
            }
        }
    }

    private fun applyToolbarCollapseState() {
        findViewById<TextView>(R.id.toolbarUsername).apply {
            text = currentUsername
            animate().alpha(if (isHeaderCollapsed) 1f else 0f).duration = 200
        }

        val showFilter = isHeaderCollapsed &&
            (currentTabIndex == ACTIVITY_TAB_INDEX || currentTabIndex == GROUPS_TAB_INDEX)
        findViewById<ImageButton>(R.id.toolbarFilterButton).visibility = if (showFilter) View.VISIBLE else View.GONE

        if (isSelf) {
            toolbar.menu?.let { menu ->
                menu.findItem(R.id.action_settings)?.isVisible = !isHeaderCollapsed
                menu.findItem(R.id.action_logout)?.isVisible = !isHeaderCollapsed
            }
        } else {
            findViewById<ImageButton>(R.id.setFriend).visibility =
                if (!isHeaderCollapsed && friendButtonWanted) View.VISIBLE else View.GONE
        }
    }

    private fun confirm(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Подтвердить") { _, _ -> onConfirm() }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun regetProfile() {
        findViewById<ShimmerFrameLayout>(R.id.userMainInformationLayoutLoading).visibility =
            View.VISIBLE
        findViewById<RelativeLayout>(R.id.userMainInformationLayout).visibility = View.GONE

        userViewModel.refreshProfile(currentUser, isSelf)
        if (!isSelf) userViewModel.loadFriendship(currentUser)
        userViewModel.graphReq = true
        userViewModel.loadGraph(currentUser, userViewModel.lastGraphTime, userViewModel.lastGraphTypes)
        userViewModel.loadEvents(currentUser, if (userViewModel.lastEventSort == 1) "members" else null, true)
        userViewModel.loadFriends(currentUser, true)
        userViewModel.loadGroups(currentUser, if (userViewModel.lastGroupSort == 1) "members" else null, true)
    }

    private fun refetchOnReconnect() {
        userViewModel.refreshProfile(currentUser, isSelf)
        if (!isSelf) userViewModel.loadFriendship(currentUser)
        userViewModel.graphReq = true
        userViewModel.loadGraph(currentUser, userViewModel.lastGraphTime, userViewModel.lastGraphTypes)
        userViewModel.loadEvents(currentUser, if (userViewModel.lastEventSort == 1) "members" else null, true)
        userViewModel.loadFriends(currentUser, true)
        userViewModel.loadGroups(currentUser, if (userViewModel.lastGroupSort == 1) "members" else null, true)
    }

    private fun getCountryEmoji(country: String?): String? {
        if (country == null) null
        return try {
            val flag1 = Character.codePointAt(country, 0) - 0x41 + 0x1F1E6
            val flag2 = Character.codePointAt(country, 1) - 0x41 + 0x1F1E6
            String(Character.toChars(flag1)) + String(Character.toChars(flag2))
        } catch (e: Exception) {
            null
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val previousId = intent.getStringExtra("previousId")
        if (previousId != null) {
            Globals.getInstance().setString("CurrentlyWatching", previousId)
        }
    }
}
