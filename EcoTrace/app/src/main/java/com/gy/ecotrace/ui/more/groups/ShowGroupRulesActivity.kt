package com.gy.ecotrace.ui.more.groups

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.viewModels.ShowGroupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text

class ShowGroupRulesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_group_rules)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        getRules(Globals.getInstance().getString("CurrentlyWatchingGroup")) {
            findViewById<TextView>(R.id.groupRulesText).text = it[0] ?: getString(R.string.noRulesInThisGroupYet)

            Glide.with(this)
                .load(it[1] ?: "")
                .into(findViewById(R.id.groupRulesImage))
        }
    }

    private fun getRules(groupId: String, callback: (MutableList<String?>) -> Unit) {
        GlobalScope.launch {
            val rules = withContext(Dispatchers.IO) {
                Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods()).getGroupRules(groupId)
            }
            withContext(Dispatchers.Main) {
                callback(rules)
            }
        }
    }
}