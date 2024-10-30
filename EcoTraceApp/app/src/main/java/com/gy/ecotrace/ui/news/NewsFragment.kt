package com.gy.ecotrace.ui.news

import android.app.ActionBar.LayoutParams
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.yandex.mapkit.search.Line
import org.w3c.dom.Text
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

        val lastExit = requireActivity().getSharedPreferences("getData", Context.MODE_PRIVATE)
            .getLong("exit", 0)
        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val vModel = NewsViewModel(repository)
        vModel.getUpdates(lastExit) {
            view.findViewById<TextView>(R.id.userNameText).text = it
        }
        val loggedLayout: LinearLayout = view.findViewById(R.id.loggedUserLayouts)
        vModel.data.observe(viewLifecycleOwner, Observer {
            loggedLayout.visibility = View.VISIBLE
            var endSum = 0

            val starts = view.findViewById<TextView>(R.id.amountAndTextEventStarts)
            starts.text = it[0].toString()
            endSum += it[0]
            starts.visibility = View.VISIBLE
            view.findViewById<ShimmerFrameLayout>(R.id.loadingEventStart).visibility = View.GONE
            val ends = view.findViewById<TextView>(R.id.amountAndTextEventEnds)
            ends.text = it[1].toString()
            endSum += it[1]
            ends.visibility = View.VISIBLE
            view.findViewById<ShimmerFrameLayout>(R.id.loadingEventEnd).visibility = View.GONE

            if (endSum == 0) {
                view.findViewById<LinearLayout>(R.id.anything).visibility = View.GONE
                view.findViewById<TextView>(R.id.nothing).visibility = View.VISIBLE
            }
        })

        val allNews: LinearLayout = view.findViewById(R.id.allNewsLayout)
        val undesirable = requireActivity().getSharedPreferences("settings", MODE_PRIVATE).getString("undesirableSources", "") ?: ""
        val savedTranslations = hashMapOf<String, String>()
        vModel.getNews(undesirable)
        vModel.sites.observe(viewLifecycleOwner, Observer {
            Log.d("got", it.toString())
            for (news in it) {
                val newsLayout = layoutInflater.inflate(R.layout.layout_news, null)
                val titleName = newsLayout.findViewById<TextView>(R.id.newsName)
                titleName.text = news["postTitle"]
                Glide.with(this)
                    .load(news["postImage"])
                    .into(newsLayout.findViewById(R.id.newsImage))

                newsLayout.setOnClickListener {
                    val browser = Intent(requireActivity(), NewsActivity::class.java)
                    browser.putExtra("url", news["postLink"].toString())
                    browser.putExtra("res", news["source"].toString())
                    startActivity(browser)
                }

                val translate = newsLayout.findViewById<ImageButton>(R.id.translate)
                translate.setOnClickListener {
                    translate.isActivated = !translate.isActivated
                    translate.isClickable = false
                    when (translate.isActivated) {
                        true -> {
                            val actualText = news["postTitle"]?.replace(" ", "%20") ?: ""

                            savedTranslations[actualText]?.let {
                                titleName.text = it
                                translate.isClickable = true
                            } ?: run {
                                vModel.getText("${BuildConfig.TRANSLATE_EXPRESSION}/translate?dl=ru&text=$actualText") { translatedText ->
                                    savedTranslations[actualText] = translatedText
                                    activity?.runOnUiThread {
                                        titleName.text = translatedText
                                        translate.isClickable = true
                                    }
                                }
                            }

                            translate.imageTintList = ContextCompat.getColorStateList(requireActivity(), R.color.ok_green)
                        }

                        false -> {
                            titleName.text = news["postTitle"]
                            translate.imageTintList = ContextCompat.getColorStateList(requireActivity(), R.color.silver)
                            translate.isClickable = true
                        }

                    }


                }


                allNews.addView((newsLayout))
            }

            view.findViewById<ShimmerFrameLayout>(R.id.newsLoading).visibility = View.GONE
            allNews.visibility = View.VISIBLE
        })

        vModel.getEducations()
        vModel.edu.observe(viewLifecycleOwner, Observer{
            loggedLayout.visibility = View.VISIBLE
            view.findViewById<ShimmerFrameLayout>(R.id.userFriendsInformationLayoutLoading).visibility = View.GONE
            val a = requireActivity().assets.list("education")
            var nonDone = 0
            for (b in a ?: emptyArray()){
                val eId = Regex("\\d+").find(b.toString())!!.value.toInt()
                if (!it.contains(eId)) {
                    nonDone++
                }
            }
            if (nonDone == 0) {
                view.findViewById<TextView>(R.id.allEduDone).visibility = View.VISIBLE
            } else {
                view.findViewById<TextView>(R.id.notallEduDone).visibility = View.VISIBLE
            }
        })
    }
}