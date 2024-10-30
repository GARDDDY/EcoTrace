package com.gy.ecotrace.ui.more.events

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.friends.PersonalShareActivity
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep1
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep2
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep3
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep4
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventViewModel
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventViewModelFactory
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import com.yandex.mapkit.MapKitFactory
import java.net.URL

class ShowEventActivity : AppCompatActivity() {
    private lateinit var eventViewModel: ShowEventViewModel


    private var mainHost = BuildConfig.SERVER_API_URI
    private fun isValidation(url: String): String {
        val data = Regex("eventparticipanrvalidation\\?(.+)").find(url)?.groupValues?.get(1)
        return data ?: ""

    }

    override fun onPause() {
        super.onPause()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var isUsingCamera = false

        if (!Globals.getInstance().getBool("ShowEvent_initMap")) {
//            val key = "f3d745ad-1974-4793-978d-52b3a165865c"
//            if (!Globals.getInstance().getBool("mapkit")) {
//                MapKitFactory.setApiKey(key)
//                Globals.getInstance().setBool("mapkit", true)
//            }
            MapKitFactory.initialize(this)
            Globals.getInstance().setBool("ShowEvent_initMap", true)
        }

        setContentView(R.layout.activity_show_event)

        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val factory = ShowEventViewModelFactory(repository)
        eventViewModel = ViewModelProvider(this, factory)[ShowEventViewModel::class.java]

        val currentEvent = Globals.getInstance().getString("CurrentlyWatchingEvent")
        eventViewModel.currentEvent = currentEvent

        val toolbar: Toolbar = findViewById(R.id.toolbar3)
        Globals().initToolbarIconBack(toolbar, applicationContext)


        val tabTitles = arrayOf("Основное", "План", "Карта", "Участники")
        val tabView: TabLayout = findViewById(R.id.tabLayout)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val previewView: PreviewView = findViewById(R.id.previewView)

        toolbar.setNavigationOnClickListener {
            if (isUsingCamera) {
                isUsingCamera = false
                ProcessCameraProvider.getInstance(this).get().unbindAll()
                previewView.visibility = View.GONE
                tabView.visibility = View.VISIBLE
                viewPager.visibility = View.VISIBLE
            } else onBackPressed()
        }

        viewPager.adapter = ShowAdapter(this, 4)
        viewPager.isUserInputEnabled = false
        TabLayoutMediator(tabView, viewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        val createQr: ImageButton = findViewById(R.id.eventCreateQr)

        val scanQr: ImageButton = findViewById(R.id.eventScanQr)

        eventViewModel.getEvent()
        eventViewModel.event.observe(this, Observer {

            createQr.setOnClickListener { _ ->
                val currentUser = ETAuth.getInstance().guid()
                val qrActivity = Intent(this@ShowEventActivity, PersonalShareActivity::class.java)
                qrActivity.putExtra("valid", true)
                qrActivity.putExtra("link", "eventparticipanrvalidation?uid=$currentUser&eid=$currentEvent")

                qrActivity.putExtra("image", currentEvent)
                qrActivity.putExtra("imageF", "events")
                qrActivity.putExtra("name", it.eventInfo.eventName)

                startActivity(qrActivity)
            }

            if (it.eventInfo.eventStatus == 1) {
                eventViewModel.isUserValidated { valid ->
                    if (!valid) createQr.visibility = View.VISIBLE
                }
            }


            eventViewModel.isUserModer {
                if (!it) {
                    findViewById<ImageButton>(R.id.eventEdit).visibility = View.GONE
                    return@isUserModer
                }
                createQr.visibility = View.GONE
                scanQr.visibility = View.VISIBLE

                toolbar.inflateMenu(R.menu.popup_menu_event)
                toolbar.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_settings -> {
                            Globals.getInstance().setString("CurrentlyEditingEvent", currentEvent)
                            val myIntent = Intent(this, CreateEventActivity::class.java)
                            startActivity(myIntent)
                            true
                        }
                        R.id.action_logout -> {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Удаление мероприятия")

                            builder.setMessage("Вы действительно хотите безвозвратно удалить это мероприятие?")
                            builder.setPositiveButton("Подтвердить") { dialog, which ->
                                // delete
                                finish()
                            }
                            builder.setNegativeButton("Отмена") { dialog, which ->
                                dialog.dismiss()
                            }
                            val dialog = builder.create()
                            dialog.show()
                            true
                        }
                        else -> {
                            super.onOptionsItemSelected(it)
                        }
                    }
                }

            }
        })

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
                            callback(isValidation(rawValue?: ""))
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }



        scanQr.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            }


            previewView.visibility = View.VISIBLE
            tabView.visibility = View.GONE
            viewPager.visibility = View.GONE
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
                                        tabView.visibility = View.VISIBLE
                                        viewPager.visibility = View.VISIBLE
                                        isUsingCamera = false

                                        val regex = Regex("uid=(.+)&eid=(.+)$")
                                        val matchResult = regex.find(url)
                                        val userId = matchResult?.groups?.get(1)?.value ?: ""
                                        val eventId = matchResult?.groups?.get(2)?.value ?: ""

                                        if (eventViewModel.currentEvent == eventId) {
                                            eventViewModel.validateUser(userId) {

                                                Toast.makeText(this@ShowEventActivity,
                                                    if (it) "Успешно!"
                                                    else "Не удалось подтвердить пользователя!",
                                                    Toast.LENGTH_LONG
                                                )
                                                    .show()

                                            }
                                        }

                                        Log.d("link qr", userId)
//                                        Globals.getInstance().setString("CurrentlyWatching", userId)
//                                        startActivity(Intent(this@UsersSearchFriends, ProfileActivity::class.java))
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


    }

    class ShowAdapter(fragmentActivity: FragmentActivity, private val totalTabs: Int) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ShowEventStep1()
                1 -> ShowEventStep2()
                2 -> ShowEventStep3()
                else -> ShowEventStep4()
            }
        }

        override fun getItemCount(): Int {
            return totalTabs
        }
        override fun getItemViewType(position: Int): Int {
            return position
        }
    }
}