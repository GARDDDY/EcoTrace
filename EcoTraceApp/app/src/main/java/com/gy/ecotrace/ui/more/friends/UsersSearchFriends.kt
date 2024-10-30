package com.gy.ecotrace.ui.more.friends

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import java.net.URL


class SearcherViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearcherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearcherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class UsersSearchFriends : AppCompatActivity() {
    private var mainHost = BuildConfig.SERVER_API_URI
    private lateinit var searcherViewModel: SearcherViewModel
    private var loggedUser = ETAuth.getInstance().guid()


    private val handler = Handler(Looper.getMainLooper())
    private val delay: Long = 300
    private var runnable: Runnable? = null

    private fun isSubDomain(url: String): String {
        val mainDomain = Regex("https?://([^/]+)/").find(mainHost)?.groupValues?.get(1)

        return try {
            val host = "${URL(url).host}:${URL(url).port}"

            Log.d("URL", "$mainDomain   $host")
            if (host == mainDomain || host.endsWith(".$mainDomain")) url else ""
        } catch (e: Exception) {
            ""
        }

    }

    override fun onPause() {
        super.onPause()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (loggedUser == "0") {
            Toast.makeText(this@UsersSearchFriends, "Требуется вход в аккаунт", Toast.LENGTH_SHORT).show()
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
            startActivity(Intent(this@UsersSearchFriends, PersonalShareActivity::class.java))
        }

        val barcodeScanner = BarcodeScanning.getClient()

        fun processImageProxy(imageProxy: ImageProxy, callback: (String) -> Unit) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            Log.d("QRLink", rawValue.toString())
                            callback(isSubDomain(rawValue?: ""))
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }

        qrScanButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            }


            previewView.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
            isUsingCamera = true
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener(Runnable {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                            processImageProxy(imageProxy) { url ->
                                if (url.isNotEmpty()) {
                                    try {
                                        cameraProvider.unbindAll()
                                    } finally {
                                        previewView.visibility = View.GONE
                                        swipeRefreshLayout.visibility = View.VISIBLE
                                        isUsingCamera = false

                                        val regex = Regex("user/(\\w+)")
                                        val matchResult = regex.find(url)
                                        val userId = matchResult?.groups?.get(1)?.value ?: ""
                                        Globals.getInstance().setString("CurrentlyWatching", userId)
                                        startActivity(Intent(this@UsersSearchFriends, ProfileActivity::class.java))
                                    }
                                }
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraX", "Error binding camera use cases", exc)
                }
            }, ContextCompat.getMainExecutor(this))
        }

        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())

        val factory = SearcherViewModelFactory(repository)
        searcherViewModel = ViewModelProvider(this, factory)[SearcherViewModel::class.java]

        val data: Uri? = intent.data
        if (data != null && data.isHierarchical && data.queryParameterNames.isNotEmpty()) {
            val userId: String? = data.getQueryParameter("id")
            if (userId != null) {
                Globals.getInstance().setString("CurrentlyWatching", userId)
                val gotoProfile = Intent(this@UsersSearchFriends, ProfileActivity::class.java)
                startActivity(gotoProfile)
            }
        }
//


        searcherViewModel.getUserFriends(loggedUser)
        searcherViewModel.userFriends.observe(this, Observer {
            val friendsLayout: LinearLayout =
                findViewById(R.id.friendsLayout)
            friendsLayout.removeAllViews()

            it?.let{
                if (it.isNotEmpty()) {

                    for (fr in it) {
                        val friendOneLayout =
                            layoutInflater.inflate(R.layout.friend_linear_layout, null)
                        val usernameText = friendOneLayout.findViewById<TextView>(R.id.username_friend_layout)
                        usernameText.text = fr.username
                        usernameText.setTextColor(ContextCompat.getColor(applicationContext, if (fr.isFriend == 1) R.color.ok_green else R.color.red_no))


                        val friendId = if (fr.senderId == loggedUser) fr.userId else fr.senderId

                        friendOneLayout.setOnClickListener {
                            val myIntent = Intent(this, ProfileActivity::class.java)
                            myIntent.putExtra("previousId", loggedUser)
                            Globals.getInstance().setString("CurrentlyWatching", friendId)
                            this.startActivity(myIntent)
                        }

                        Glide.with(this)
                            .load(Globals().getImgUrl("users", "${friendId}"))
                            .circleCrop()
                            .into(friendOneLayout.findViewById(R.id.user_img_friend_layout))

                        friendsLayout.addView(friendOneLayout)
                    }

                    findViewById<TextView>(R.id.noFriendsWarning).visibility = View.GONE
                }


            }
            findViewById<LinearLayout>(R.id.userFriendsInformationLayout).visibility = View.VISIBLE
            findViewById<ShimmerFrameLayout>(R.id.userFriendsInformationLayoutLoading).visibility = View.GONE
        })

        val userFriendScrollView: HorizontalScrollView = findViewById(R.id.friendsLayoutScrollView)
        userFriendScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (!searcherViewModel.updFriends && !searcherViewModel.foundAll) {
                if (scrollX >= (v as HorizontalScrollView).getChildAt(0).width - v.width) {
                    searcherViewModel.updFriends = true
                    searcherViewModel.getUserFriends(loggedUser)
                }
            }
        }

        val friendScrollView: ScrollView = findViewById(R.id.scrollViewMain)
        friendScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (!searcherViewModel.updUsers && !searcherViewModel.foundAllUsers) {
                if (scrollX >= (v as HorizontalScrollView).getChildAt(0).width - v.width) {
                    searcherViewModel.updUsers = true
                    searcherViewModel.findAllUsers()
                }
            }
        }

        val loading = findViewById<ShimmerFrameLayout>(R.id.allUsersLoading)
        val layoutForFiltered: LinearLayout = findViewById(R.id.foundUsersLayout)
        val filtersLayout: LinearLayout = findViewById(R.id.userFiltersLayout)
        val tagsColors:Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
        val allEventTags:Array<Pair<String, String>> = Globals.getInstance().getUserFilters()
        for (tag in allEventTags.indices) {
            val tagButton = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
            tagButton.text = allEventTags[tag].first
            tagButton.textSize = 18F
            tagButton.setTextColor(Color.parseColor(tagsColors[tag].second))
            tagButton.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.transparent))
            tagButton.rippleColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].second))
            tagButton.strokeColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))

            tagButton.setOnClickListener {
                tagButton.isActivated = !tagButton.isActivated
                if(!tagButton.isActivated) tagButton
                    .setBackgroundColor(ContextCompat
                        .getColor(applicationContext, R.color.transparent))
                else tagButton
                    .setBackgroundColor(Color.parseColor(tagsColors[tag].first))
                searcherViewModel.reapplyFilter(tag)
                loading.visibility = View.VISIBLE
                layoutForFiltered.visibility = View.GONE
                searcherViewModel.findAllUsers(true)
            }

            filtersLayout.addView(tagButton)
        }

        findViewById<EditText>(R.id.usernameSearcher).addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                searcherViewModel.filterName = p0?.toString()
                loading.visibility = View.VISIBLE
                layoutForFiltered.visibility = View.GONE
                searcherViewModel.filterName = p0?.toString()
                runnable?.let { handler.removeCallbacks(it) }
                runnable = Runnable {
                    searcherViewModel.findAllUsers(true)
                }
                handler.postDelayed(runnable!!, delay)
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        // найденные
        searcherViewModel.findAllUsers()
        searcherViewModel.usersFoundFilter.observe(this, Observer {
            loading.visibility = View.GONE
            layoutForFiltered.visibility = View.VISIBLE
            it?.let{
//                findViewById<TextView>(R.id.bad_tags_no_found).visibility = View.GONE
//                findViewById<TextView>(R.id.bad_query_not_found).visibility = View.GONE
                layoutForFiltered.removeAllViews()
                if (it.isNotEmpty()) {
                    findViewById<TextView>(R.id.noUsersWarning).visibility = View.GONE
                    for (user in it) {
                        Log.d("unparse", user.toString())
                        user.let {
                            val layoutInf =
                                layoutInflater.inflate(R.layout.layout_user_found_filters, null)
                            layoutInf.findViewById<TextView>(R.id.username_found_layout).text =
                                user.username
                            val filters = user.filters.split(",").map { it.toInt() - 1 }
                            val filtersLayout =
                                layoutInf.findViewById<LinearLayout>(R.id.user_filters_layout)
                            for (f in filters) {
                                val tagFilter = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
                                tagFilter.text = allEventTags[f].first
                                tagFilter.textSize = 16F
                                tagFilter.tooltipText = allEventTags[f].second
                                tagFilter.setTextColor(Color.parseColor(tagsColors[f].second))
                                tagFilter.setBackgroundColor(Color.parseColor(tagsColors[f].first))
                                filtersLayout.addView(tagFilter)
                            }

                            Glide.with(this)
                                .load(Globals().getImgUrl("users", user.userId))
                                .circleCrop()
                                .placeholder(R.drawable.baseline_person_24)
                                .into(layoutInf.findViewById(R.id.user_img_found_layout))

                            layoutInf.findViewById<LinearLayout>(R.id.goto_profile)
                                .setOnClickListener {
                                    val myIntent = Intent(this, ProfileActivity::class.java)
                                    myIntent.putExtra("previousId", loggedUser)
                                    Globals.getInstance()
                                        .setString("CurrentlyWatching", user.userId)
                                    this.startActivity(myIntent)
                                }

                            layoutForFiltered.addView(layoutInf)
                        }
                    }
                }
                else {
                    findViewById<TextView>(R.id.noUsersWarning).visibility = View.VISIBLE
                }
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            searcherViewModel.findAllUsers(true)
            searcherViewModel.getUserFriends(loggedUser, true)
        }
    }
}