package com.gy.ecotrace.ui.more.groups

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import java.sql.Time
import java.util.Locale

class ShowGroupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_group)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar: Toolbar = findViewById(R.id.toolbar4)
        val backIcon = ContextCompat.getDrawable(applicationContext, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(applicationContext, R.color.ok_green), PorterDuff.Mode.SRC_ATOP)
        toolbar.navigationIcon = backIcon
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")
        val group = intent.getStringExtra("gInfo")
        val groupData = Gson().fromJson(group, DatabaseMethods.UserDatabaseMethods.UserGroups::class.java)

        findViewById<TextView>(R.id.groupName).text = groupData.groupInfo.groupName
        findViewById<TextView>(R.id.groupAbout).text = groupData.groupInfo.groupAbout
//        findViewById<TextView>(R.id.groupCountMembers).text = eventData.eventInfo.eventCountMembers.toString()





        Glide.with(this@ShowGroupActivity)
            .load(Globals().getImgUrl("groups", currentGroup))
            .placeholder(R.drawable.round_family_restroom_24)
            .into(findViewById(R.id.groupImage))

        val sections: MutableList<MutableList<Int>> = arrayListOf(
            arrayListOf(R.id.group_news_section, R.id.group_news_layout),
            arrayListOf(R.id.group_chat_section, R.id.group_chat_layout),
            arrayListOf(R.id.group_members_section, R.id.group_members_layout)
        )

        for (i in 0..2) {
            findViewById<Button>(sections[i][0]).setOnClickListener {
                findViewById<Button>(sections[i][0]).text = System.currentTimeMillis().toString()
                findViewById<Button>(sections[(i-1)%3 + if (i-1 == -1) 3 else 0][0]).setBackgroundResource(R.color.ok_green)
                findViewById<Button>(sections[(i+1)%3][0]).setBackgroundResource(R.color.ok_green)

                sections.forEach {

                }
            }
        }
    }
}