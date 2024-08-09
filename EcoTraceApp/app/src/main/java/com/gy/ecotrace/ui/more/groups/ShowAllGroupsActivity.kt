package com.gy.ecotrace.ui.more.groups

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.events.CreateEventActivity

class ShowAllGroupsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_all_groups)


        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener {
            startActivity(
                Intent(this@ShowAllGroupsActivity, CreateGroupActivity::class.java)
            )
        }
        fabAdd.imageTintList = getColorStateList(R.color.dirt_white)
        fabAdd.setImageResource(R.drawable.baseline_add_24)
    }


}