package com.gy.ecotrace.ui.more.events

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.friends.PersonalShareActivity
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep1
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep2
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep3
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventStep4
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventViewModel
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventViewModelFactory
import com.yandex.mapkit.MapKitFactory

class ShowEventActivity : AppCompatActivity() {
    private lateinit var eventViewModel: ShowEventViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val tabTitles = arrayOf("Основное", "План", "Карта", "Участники")
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        viewPager.adapter = ShowAdapter(this, 4)
        viewPager.isUserInputEnabled = false
        val tabView: TabLayout = findViewById(R.id.tabLayout)
        TabLayoutMediator(tabView, viewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        val createQr: ImageButton = findViewById(R.id.eventCreateQr)
        createQr.setOnClickListener {
            val intent = Intent(this@ShowEventActivity, PersonalShareActivity::class.java)
            intent.putExtra("linkDest", "event?uid=${FirebaseAuth.getInstance().currentUser?.uid}&eid=${eventViewModel.currentEvent}")
            intent.putExtra("imgFolder", "events")
            intent.putExtra("imgId", eventViewModel.currentEvent)
            intent.putExtra("text", "")
            startActivity(intent)
        }

        val scanQr: ImageButton = findViewById(R.id.eventScanQr)

        eventViewModel.getEvent()
        eventViewModel.event.observe(this, Observer {

            if (it.eventInfo.eventStatus == 2)


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