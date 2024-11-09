package com.gy.ecotrace.ui.more.friends

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class UsersSearchFriends : AppCompatActivity() {
    private val mainHost = BuildConfig.SERVER_API_URI
    private val searcherViewModel: SearcherViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore
    private val loggedUser: Long get() = tokenStore.userId

    private val handler = Handler(Looper.getMainLooper())
    private val delay: Long = 300
    private var runnable: Runnable? = null

    private var sortingFriendsList = false

    private var pendingQrScanRequest = false
    private var startQrScannerAction: (() -> Unit)? = null

    private fun rankUrl(rank: String): String = "${BuildConfig.SERVER_API_URI}uploads/\$ranks/$rank"

    private val expToRanks = mutableMapOf(
        0 to "rank1", 300 to "rank2", 700 to "rank3",
        1200 to "rank4", 1800 to "rank5", 2500 to "rank6"
    )

    private fun userAvatarUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" }

    private fun isSubDomain(url: String): String {
        val mainDomain = Regex("https?://([^/]+)/").find(mainHost)?.groupValues?.get(1)
        return try {
            val host = "${URL(url).host}:${URL(url).port}"
            if (host == mainDomain || host.endsWith(".$mainDomain")) url else ""
        } catch (e: Exception) {
            ""
        }
    }

    override fun onPause() {
        super.onPause()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST && pendingQrScanRequest) {
            pendingQrScanRequest = false
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                startQrScannerAction?.invoke()
            } else {
                Toast.makeText(this, "Нужен доступ к камере для сканирования QR-кода", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private companion object {
        const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (loggedUser <= 0) {
            Toast.makeText(this, "Требуется вход в аккаунт", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_friends)
        val toolbar: Toolbar = findViewById(R.id.toolbar5)
        val previewView: PreviewView = findViewById(R.id.previewView)
        val swipeRefreshLayout: SwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        var isUsingCamera = false
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            if (isUsingCamera) {
                isUsingCamera = false
                ProcessCameraProvider.getInstance(this).get().unbindAll()
                previewView.visibility = View.GONE
                swipeRefreshLayout.visibility = View.VISIBLE
            } else onBackPressed()
        }

        val qrScanButton: ImageButton = findViewById(R.id.scanForQrCode)
        val shareOpen: ImageButton = findViewById(R.id.personalShareContent)
        shareOpen.setOnClickListener {
            startActivity(Intent(this, PersonalShareActivity::class.java))
        }

        val barcodeScanner = BarcodeScanning.getClient()
        fun processImageProxy(imageProxy: ImageProxy, callback: (String) -> Unit) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            callback(isSubDomain(barcode.rawValue ?: ""))
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
        }

        fun startQrScanner() {
            previewView.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
            isUsingCamera = true
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener(Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                            processImageProxy(imageProxy) { url ->
                                val userId = Regex("user/(\\d+)").find(url)?.groupValues?.get(1)
                                if (userId != null) {
                                    cameraProvider.unbindAll()
                                    previewView.visibility = View.GONE
                                    swipeRefreshLayout.visibility = View.VISIBLE
                                    isUsingCamera = false
                                    Globals.getInstance().setString("CurrentlyWatching", userId)
                                    startActivity(Intent(this, ProfileActivity::class.java))
                                }
                            }
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (exc: Exception) {
                }
            }, ContextCompat.getMainExecutor(this))
        }
        startQrScannerAction = ::startQrScanner

        qrScanButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startQrScanner()
            } else {
                pendingQrScanRequest = true
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            }
        }

        val data: Uri? = intent.data
        if (data != null && data.isHierarchical && data.queryParameterNames.isNotEmpty()) {
            data.getQueryParameter("id")?.let { userId ->
                Globals.getInstance().setString("CurrentlyWatching", userId)
                startActivity(Intent(this, ProfileActivity::class.java))
            }
        }

        supportFragmentManager.setFragmentResultListener(FriendsSortBottomSheet.RESULT_KEY, this) { _, bundle ->
            val sort = bundle.getString(FriendsSortBottomSheet.ARG_SORT)
            if (sortingFriendsList) {
                searcherViewModel.friendsSort = sort
                findViewById<ShimmerFrameLayout>(R.id.userFriendsInformationLayoutLoading).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.userFriendsInformationLayout).visibility = View.GONE
                searcherViewModel.loadFriends(loggedUser, true)
            } else {
                searcherViewModel.sortBy = sort
                restartSearch()
            }
        }

        supportFragmentManager.setFragmentResultListener(FriendsFiltersFragment.RESULT_KEY, this) { _, bundle ->
            searcherViewModel.filter = bundle.getString(FriendsFiltersFragment.ARG_FILTER)
            searcherViewModel.commonGroup = bundle.getBoolean(FriendsFiltersFragment.ARG_COMMON_GROUP).takeIf { it }
            searcherViewModel.commonEvent = bundle.getBoolean(FriendsFiltersFragment.ARG_COMMON_EVENT).takeIf { it }
            restartSearch()
        }

        searcherViewModel.loadFriends(loggedUser)
        searcherViewModel.friends.observe(this, Observer { friends ->
            val friendsLayout: LinearLayout = findViewById(R.id.friendsLayout)
            if (searcherViewModel.newFriends) {
                friendsLayout.removeAllViews()
                searcherViewModel.newFriends = false
            }
            findViewById<TextView>(R.id.noFriendsWarning).visibility =
                if (friends.isEmpty()) View.VISIBLE else View.GONE

            for (fr in friends) {
                val row = layoutInflater.inflate(R.layout.layout_friend__my_friend, null)
                row.findViewById<TextView>(R.id.userUsername).apply {
                    text = fr.username
                    setTextColor(ContextCompat.getColor(applicationContext, R.color.ok_green))
                }
                row.setOnClickListener {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("previousId", loggedUser.toString())
                    Globals.getInstance().setString("CurrentlyWatching", fr.id.toString())
                    startActivity(intent)
                }
                Glide.with(this)
                    .load(userAvatarUrl(fr.avatarKey))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(row.findViewById(R.id.userProfileImage))
                Glide.with(this)
                    .load(rankUrl(expToRanks[expToRanks.keys.filter { it <= fr.experience }.maxOrNull()] ?: "rank1"))
                    .into(row.findViewById<ImageView>(R.id.userRankImage))
                friendsLayout.addView(row)
            }

            findViewById<LinearLayout>(R.id.userFriendsInformationLayout).visibility = View.VISIBLE
            findViewById<ShimmerFrameLayout>(R.id.userFriendsInformationLayoutLoading).visibility = View.GONE
        })

        findViewById<HorizontalScrollView>(R.id.friendsLayoutScrollView)
            .setOnScrollChangeListener { v, scrollX, _, _, _ ->
                if (!searcherViewModel.updFriends && !searcherViewModel.foundAllFriends) {
                    if (scrollX >= (v as HorizontalScrollView).getChildAt(0).width - v.width) {
                        searcherViewModel.updFriends = true
                        searcherViewModel.loadFriends(loggedUser)
                    }
                }
            }

        findViewById<ImageButton>(R.id.friendsSortButton).setOnClickListener {
            sortingFriendsList = true
            FriendsSortBottomSheet.newInstance(searcherViewModel.friendsSort)
                .show(supportFragmentManager, FriendsSortBottomSheet.TAG)
        }

        findViewById<ScrollView>(R.id.scrollViewMain)
            .setOnScrollChangeListener { v, _, scrollY, _, _ ->
                if (!searcherViewModel.updUsers && !searcherViewModel.foundAllUsers) {
                    if (scrollY >= (v as ScrollView).getChildAt(0).height - v.height) {
                        searcherViewModel.updUsers = true
                        searcherViewModel.search()
                    }
                }
            }

        val loading = findViewById<ShimmerFrameLayout>(R.id.allUsersLoading)
        val layoutForFiltered: LinearLayout = findViewById(R.id.foundUsersLayout)

        findViewById<EditText>(R.id.usernameSearcher).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                searcherViewModel.query = p0?.toString()
                runnable?.let { handler.removeCallbacks(it) }
                runnable = Runnable { restartSearch() }
                handler.postDelayed(runnable!!, delay)
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        findViewById<ImageButton>(R.id.searchSortByButton).setOnClickListener {
            sortingFriendsList = false
            FriendsSortBottomSheet.newInstance(searcherViewModel.sortBy)
                .show(supportFragmentManager, FriendsSortBottomSheet.TAG)
        }
        findViewById<ImageButton>(R.id.showSearchFiltersButton).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, FriendsFiltersFragment.newInstance(
                    searcherViewModel.filter,
                    searcherViewModel.commonGroup,
                    searcherViewModel.commonEvent
                ))
                .addToBackStack(FriendsFiltersFragment.TAG)
                .commit()
        }

        searcherViewModel.search()
        searcherViewModel.results.observe(this, Observer { users ->
            loading.visibility = View.GONE
            layoutForFiltered.visibility = View.VISIBLE
            layoutForFiltered.removeAllViews()
            if (users.isEmpty()) {
                findViewById<TextView>(R.id.noUsersWarning).visibility = View.VISIBLE
                return@Observer
            }
            findViewById<TextView>(R.id.noUsersWarning).visibility = View.GONE
            for (user in users) {
                val row = layoutInflater.inflate(R.layout.layout_user_found_filters, null)
                row.findViewById<TextView>(R.id.username_found_layout).text = user.username
                Glide.with(this)
                    .load(userAvatarUrl(user.avatarKey))
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(row.findViewById(R.id.user_img_found_layout))

                val tagNames = user.filters.orEmpty().mapNotNull { id ->
                    Globals.getInstance().getUserFilters().getOrNull(id - 1)?.first
                }
                row.findViewById<ChipGroup>(R.id.tagsChipGroup).apply {
                    removeAllViews()
                    tagNames.take(3).forEach { name -> addView(Chip(this@UsersSearchFriends).apply { text = name }) }
                }
                val alsoTagsCount = row.findViewById<TextView>(R.id.alsoTagsCount)
                if (tagNames.isEmpty()) {
                    row.findViewById<HorizontalScrollView>(R.id.tagsInfoLayout).visibility = View.GONE
                } else {
                    row.findViewById<HorizontalScrollView>(R.id.tagsInfoLayout).visibility = View.VISIBLE
                    if (tagNames.size > 3) {
                        alsoTagsCount.visibility = View.VISIBLE
                        alsoTagsCount.text = "+${tagNames.size - 3}"
                    } else {
                        alsoTagsCount.visibility = View.GONE
                    }
                }

                val commonGroupsCount = user.commonGroupsCount ?: 0
                val commonEventsCount = user.commonEventsCount ?: 0
                row.findViewById<ImageView>(R.id.commonGroupsVectorImage).visibility =
                    if (commonGroupsCount > 0) View.VISIBLE else View.GONE
                row.findViewById<TextView>(R.id.commonGroups).apply {
                    visibility = if (commonGroupsCount > 0) View.VISIBLE else View.GONE
                    if (commonGroupsCount > 0) text = commonGroupsLabel(commonGroupsCount)
                }
                row.findViewById<ImageView>(R.id.commonEventsVectorImage).visibility =
                    if (commonEventsCount > 0) View.VISIBLE else View.GONE
                row.findViewById<TextView>(R.id.commonEvents).apply {
                    visibility = if (commonEventsCount > 0) View.VISIBLE else View.GONE
                    if (commonEventsCount > 0) text = commonEventsLabel(commonEventsCount)
                }
                row.findViewById<TextView>(R.id.delimiter1).visibility =
                    if (commonGroupsCount > 0 && commonEventsCount > 0) View.VISIBLE else View.GONE
                row.findViewById<RelativeLayout>(R.id.commonGroupsLayout).visibility =
                    if (commonGroupsCount > 0 || commonEventsCount > 0) View.VISIBLE else View.GONE

                row.findViewById<LinearLayout>(R.id.goto_profile).setOnClickListener {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("previousId", loggedUser.toString())
                    Globals.getInstance().setString("CurrentlyWatching", user.id.toString())
                    startActivity(intent)
                }
                layoutForFiltered.addView(row)
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            searcherViewModel.search(true)
            searcherViewModel.loadFriends(loggedUser, true)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun commonGroupsLabel(count: Int): String {
        val noun = when {
            count % 10 == 1 && count % 100 != 11 -> "группа"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "группы"
            else -> "групп"
        }
        val adj = if (count % 10 == 1 && count % 100 != 11) "общая" else "общих"
        return "$count $adj $noun"
    }

    private fun commonEventsLabel(count: Int): String {
        val noun = when {
            count % 10 == 1 && count % 100 != 11 -> "мероприятие"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "мероприятия"
            else -> "мероприятий"
        }
        val adj = if (count % 10 == 1 && count % 100 != 11) "общее" else "общих"
        return "$count $adj $noun"
    }

    private fun restartSearch() {
        findViewById<ShimmerFrameLayout>(R.id.allUsersLoading).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.foundUsersLayout).visibility = View.GONE
        searcherViewModel.search(true)
    }
}
