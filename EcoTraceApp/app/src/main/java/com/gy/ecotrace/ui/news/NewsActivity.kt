package com.gy.ecotrace.ui.news

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R

class NewsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news)

        val mainLink = intent.getStringExtra("url")

        if (mainLink == null) {
            finish()
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val goBack: ImageButton = findViewById(R.id.goBack)
        val goForward: ImageButton = findViewById(R.id.goForward)
        val reloadPage: ImageButton = findViewById(R.id.reload)
        val translatePage: ImageButton = findViewById(R.id.translate)


        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return false//!url.contains("nationalgeographic.com") // todo
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                goForward.imageTintList = ContextCompat.getColorStateList(applicationContext, if (webView.canGoForward()) R.color.black else R.color.silver)
                goBack.imageTintList = ContextCompat.getColorStateList(applicationContext, if (webView.canGoBack()) R.color.black else R.color.silver)
            }
        }


        val sourceName: TextView = findViewById(R.id.sourceName)
        sourceName.text = intent.getStringExtra("res")

        goBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        goForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        reloadPage.setOnClickListener {
            webView.reload()
        }

        var isTranslated = false
        var originalUrl: String? = null

        translatePage.setOnClickListener {
            if (isTranslated) {
                if (originalUrl != null) {
                    webView.loadUrl(originalUrl!!)
                }
                isTranslated = false
            } else {
                originalUrl = webView.url
                if (originalUrl != null) {
                    val translateUrl = "/translate?sl=auto&tl=ru&u=$originalUrl" // todo
                    webView.loadUrl(translateUrl)
                }
                isTranslated = true
            }

            translatePage.imageTintList = ContextCompat.getColorStateList(applicationContext, if (isTranslated) R.color.ok_green else R.color.black)
        }

        webView.loadUrl(mainLink!!)
    }
}