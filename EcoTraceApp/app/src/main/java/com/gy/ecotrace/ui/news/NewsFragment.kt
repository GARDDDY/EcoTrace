package com.gy.ecotrace.ui.news

import android.app.ActionBar.LayoutParams
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gy.ecotrace.R
import com.yandex.mapkit.search.Line
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NewsFragment : Fragment() {
    private fun getTodayUtc(dayOffset: Long = 0): String {
        val utcDate = OffsetDateTime.now(ZoneOffset.UTC)
        val newDay = utcDate.minusDays(dayOffset)
        return newDay.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lastExit = requireActivity().getSharedPreferences("getData", Context.MODE_PRIVATE).getLong("exit", 0)

        val eventsStarted = 1
        val eventsEnded = 0
        val friendsGot = 0
        val groupsInvited = 0

        if (eventsStarted + eventsEnded + friendsGot + groupsInvited == 0) {
            view.findViewById<LinearLayout>(R.id.anything).visibility = View.GONE
            view.findViewById<TextView>(R.id.nothing).visibility = View.VISIBLE
        }

//        val webView: WebView = view.findViewById(R.id.webView)
//
//        val allNews: LinearLayout = view.findViewById(R.id.allNewsLayout)
//        webView.webViewClient = object : WebViewClient() {
//            override fun onPageFinished(view: WebView, url: String) {
//                super.onPageFinished(view, url)
//
//
//                webView.evaluateJavascript(
//                    """
//
//                    var elements = document.querySelectorAll('[class*="col-bottom-gutter-2"]');
//                    var results = [];
//                    elements.forEach(function(element) {
//
//                        results.push({
//                            classes: element.className,
//                            text: element.textContent.trim()
//                        });
//                    });
//
//                    JSON.stringify(results);
//                    """
//                ) { result ->
//                    data class ElementData(val classes: String, val text: String)
//                    val listType = object : TypeToken<List<ElementData>>() {}.type
//
//                    val elements: List<ElementData> = Gson().fromJson(result, listType)
//
//                    elements.forEach {
//                        val text = TextView(context)
//                        text.text = it.text
//
//                        allNews.addView(text)
////                        Log.d("ParsedJson", "Classes: ${it.classes}")
////                        Log.d("ParsedJson", "Text: ${it.text}")
//                    }
//                }
//            }
//        }
//
//        webView.settings.javaScriptEnabled = true
//
//        webView.loadUrl("https://www.nationalgeographic.com/environment")

    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}