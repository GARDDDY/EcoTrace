package com.gy.ecotrace.ui.more.events.showtabs

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.events.ShowEventViewModel
import org.w3c.dom.Text

class ShowEventStep1 : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_show_event_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentEvent = Globals.getInstance().getString("CurrentlyWatchingEvent")
        val currentUser = Globals.getInstance().getString("CurrentlyLogged")
        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val factory = ShowEventViewModelFactory(repository)
        val eventViewModel = ViewModelProvider(this, factory)[ShowEventViewModel::class.java]

        Glide.with(requireActivity())
            .load(Globals().getImgUrl("events", currentEvent))
            .placeholder(R.drawable.round_family_restroom_24)
            .into(view.findViewById(R.id.eventImage))
        var currentUserInTheEvent = false
        var currentUserIsCreator = false

        eventViewModel.getEvent(currentEvent)
        eventViewModel.event.observe(viewLifecycleOwner, Observer {
            view.findViewById<TextView>(R.id.eventName).text = it.eventName
            view.findViewById<TextView>(R.id.eventAbout).text = it.eventAbout
            view.findViewById<TextView>(R.id.eventCountMembers).text = it.eventCountMembers.toString()
            view.findViewById<TextView>(R.id.eventStartDate).text = when(it.eventStatus) {
                0 -> "Еще не началось"
                1 -> "Уже проходит"
                2 -> "Закончилось"
                else -> "unreal-event-status"
            }

            currentUserInTheEvent = it.eventUsersToTheirRoles!!.keys.contains(currentUser)
            currentUserIsCreator = it.eventCreatorId == currentUser
            Log.d("event join", "$currentUserInTheEvent   ${it.eventUsersToTheirRoles} $currentUser")

            eventViewModel.getEventMembers(it)


            if (currentUser != "0") {
                view.findViewById<LinearLayout>(R.id.optionsWhenLogged).visibility = View.VISIBLE
                val joinBtn = view.findViewById<Button>(R.id.joinEvent)

                if (currentUserInTheEvent) {
                    joinBtn.text = "Покинуть"
                    joinBtn.setOnClickListener {
                        if (!currentUserIsCreator) {
                            eventViewModel.leaveEvent(currentEvent, currentUser)
                        } else {
                            // unable to leave alert!!!
                        }
                    }
                } else {
                    joinBtn.setOnClickListener {
                        eventViewModel.joinEvent(currentEvent, currentUser)
                    }
                }

                view.findViewById<Button>(R.id.shareEvent).setOnClickListener {
                    // copy event link
                }
            }
        })
    }
}