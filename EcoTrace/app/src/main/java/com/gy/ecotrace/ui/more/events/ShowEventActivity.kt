package com.gy.ecotrace.ui.more.events

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep1
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep2
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventViewModel
import com.gy.ecotrace.ui.more.friends.PersonalShareActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShowEventActivity : AppCompatActivity() {

    private val eventViewModel: ShowEventViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private var currentEvent: Long = 0

    private fun isValidation(url: String): String =
        Regex("eventparticipanrvalidation\\?(.+)").find(url)?.groupValues?.get(1) ?: ""

    override fun onPause() {
        super.onPause()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_event_show)

        var isUsingCamera = false

        currentEvent = Globals.getInstance().getString("CurrentlyWatchingEvent").toLongOrNull() ?: 0
        eventViewModel.currentEvent = currentEvent

        val toolbar: Toolbar = findViewById(R.id.toolbar3)
        Globals().initToolbarIconBack(toolbar, applicationContext)

        val tabTitles = arrayOf("Основное", "Карта")
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

        viewPager.adapter = ShowAdapter(this, 2)
        viewPager.isUserInputEnabled = false
        TabLayoutMediator(tabView, viewPager) { tab, pos -> tab.text = tabTitles[pos] }.attach()

        val createQr: ImageButton = findViewById(R.id.eventCreateQr)
        val scanQr: ImageButton = findViewById(R.id.eventScanQr)
        val editBtn: ImageButton = findViewById(R.id.eventEdit)

        eventViewModel.getEvent()
        eventViewModel.event.observe(this, Observer { event ->
            event ?: return@Observer

            createQr.setOnClickListener {
                val qrActivity = Intent(this, PersonalShareActivity::class.java)
                qrActivity.putExtra("valid", true)
                qrActivity.putExtra("link", "eventparticipanrvalidation?uid=${tokenStore.userId}&eid=$currentEvent")
                qrActivity.putExtra("image", event.imageKey)
                qrActivity.putExtra("imageF", "events")
                qrActivity.putExtra("name", event.name)
                startActivity(qrActivity)
            }
            createQr.visibility =
                if (event.timing == 1 && event.myRole != null && !event.myValidated) View.VISIBLE else View.GONE

            if (eventViewModel.isModer) {
                createQr.visibility = View.GONE
                scanQr.visibility = View.VISIBLE
            }
            if (eventViewModel.isCreator) {
                editBtn.visibility = View.VISIBLE
                editBtn.setOnClickListener {
                    val popup = android.widget.PopupMenu(this, editBtn)
                    popup.menuInflater.inflate(R.menu.popup_menu_event, popup.menu)
                    popup.menu.findItem(R.id.action_edit).isVisible = event.timing == 0
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_logout -> {
                                AlertDialog.Builder(this)
                                    .setTitle("Удаление мероприятия")
                                    .setMessage("Вы действительно хотите безвозвратно удалить это мероприятие?")
                                    .setPositiveButton("Подтвердить") { _, _ ->
                                        eventViewModel.deleteEvent { if (it) finish() }
                                    }
                                    .setNegativeButton("Отмена") { d, _ -> d.dismiss() }
                                    .show()
                                true
                            }
                            R.id.action_edit -> {
                                startActivity(Intent(this, CreateEventActivity::class.java).apply {
                                    putExtra("EditEventId", currentEvent)
                                })
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
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
                        for (barcode in barcodes) callback(isValidation(barcode.rawValue ?: ""))
                    }
                    .addOnCompleteListener { imageProxy.close() }
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
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                            processImageProxy(imageProxy) { data ->
                                val match = Regex("uid=(.+)&eid=(.+)$").find(data)
                                val userId = match?.groups?.get(1)?.value?.toLongOrNull()
                                val eventId = match?.groups?.get(2)?.value?.toLongOrNull()
                                if (userId != null && eventId == currentEvent) {
                                    cameraProvider.unbindAll()
                                    previewView.visibility = View.GONE
                                    tabView.visibility = View.VISIBLE
                                    viewPager.visibility = View.VISIBLE
                                    isUsingCamera = false
                                    eventViewModel.validateUser(userId) { ok ->
                                        Toast.makeText(
                                            this,
                                            if (ok) "Успешно!" else "Не удалось подтвердить пользователя!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
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
    }

    class ShowAdapter(fragmentActivity: FragmentActivity, private val totalTabs: Int) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ShowEventStep1()
            else -> ShowEventStep2()
        }
        override fun getItemCount(): Int = totalTabs
        override fun getItemViewType(position: Int): Int = position
    }
}
