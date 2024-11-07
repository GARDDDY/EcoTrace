package com.gy.ecotrace.ui.more.friends

import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods

class PersonalShareActivity : AppCompatActivity() {
    private var mainHost = BuildConfig.SERVER_API_URI
    private val currentUser = ETAuth.getInstance().getUID()

    private fun applyQr(qrImage: ImageView, text: String) {
        val width = qrImage.width
        val height = qrImage.height

        Log.e("Qr", "got data $text")

        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
            Log.e("Qr", "creating...")

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y))
                        ContextCompat.getColor(applicationContext, R.color.ok_green)
                    else ContextCompat.getColor(applicationContext, R.color.dirt2_white))
                }
            }
            Log.e("Qr", "created")
            qrImage.setImageBitmap(bitmap)
            animationOpacity(qrImage)
        } catch (e: Exception) {
            Log.e("Qr", "failed to create")
            e.printStackTrace()
        }
    }

    private fun animationOpacity(imageView: ImageView) {
        val alphaAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)
        alphaAnimator.duration = 1000
        alphaAnimator.interpolator = AccelerateInterpolator()
        alphaAnimator.start()
        Log.e("Qr", "appearing")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_share)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }



        val personalInvLink : TextView = findViewById(R.id.userLink)
        val personalQr: ImageView = findViewById(R.id.userQrCode)
        Log.d("personal", "${personalInvLink.width} ${personalInvLink.height}")
        val link = intent.getStringExtra("link") ?: "${mainHost}user/$currentUser"
        if (!(intent.getBooleanExtra("valid", false))) {
            personalInvLink.text = link
            personalInvLink.isClickable = true
            personalInvLink.setOnClickListener {
                val clipboard: ClipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Ссылка скопирована", link)
                clipboard.setPrimaryClip(clip)
            }
        }
        Handler(Looper.getMainLooper()).post {
            personalQr.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    personalQr.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    applyQr(personalQr, link)
                }
            })
        }

        findViewById<TextView>(R.id.additionalText).text = intent.getStringExtra("name") ?: ""

        Glide.with(this)
            .load(DatabaseMethods.ApplicationDatabaseMethods()
                .getImageLink( intent.getStringExtra("imageF") ?: "users",
                    intent.getStringExtra("image") ?: currentUser))
            .circleCrop()
//            .placeholder(R.drawable.baseline_person_24)
            .into(findViewById(R.id.userImage))
    }
}