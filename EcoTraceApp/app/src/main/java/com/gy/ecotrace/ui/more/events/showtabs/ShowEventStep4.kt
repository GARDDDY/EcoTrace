package com.gy.ecotrace.ui.more.events.showtabs

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.gy.ecotrace.ui.more.groups.ShowGroupActivity
import com.gy.ecotrace.ui.more.profile.ProfileActivity

class ShowEventStep4 : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_show_event_step4, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentEvent = Globals.getInstance().getString("CurrentlyWatchingEvent")
        val currentUser = Globals.getInstance().getString("CurrentlyWatching")
        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val factory = ShowEventViewModelFactory(repository)
        val eventViewModel = ViewModelProvider(this, factory)[ShowEventViewModel::class.java]

        eventViewModel.getEvent(currentEvent)
        eventViewModel.event.observe(viewLifecycleOwner, Observer{
            eventViewModel.getEventMembers(it)
        })
        eventViewModel.members.observe(viewLifecycleOwner, Observer { member ->
            val userOneLayoutInEvent = layoutInflater.inflate(R.layout.layout_user_in_event, null)
            userOneLayoutInEvent.findViewById<TextView>(R.id.username_user_in_event_layout).text = member.username
            userOneLayoutInEvent.findViewById<TextView>(R.id.user_role_in_event_user_in_event_layout).text = DatabaseMethods.DataClasses.EventRoles[member.userRole]
            val openBestGroup = userOneLayoutInEvent.findViewById<TextView>(R.id.user_group_user_in_event_layout)
            openBestGroup.text = member.userBestGroupName
            userOneLayoutInEvent.findViewById<TextView>(R.id.user_rank_user_in_event_layout).text = member.userRank
            userOneLayoutInEvent.findViewById<TextView>(R.id.user_experience_user_in_event_layout).text = member.userExperience.toString()

            Glide.with(requireActivity())
                .load(Globals().getImgUrl("users", member.userId))
                .placeholder(R.drawable.baseline_person_24)
                .into(userOneLayoutInEvent.findViewById(R.id.user_img_user_in_event_layout))


            userOneLayoutInEvent.findViewById<LinearLayout>(R.id.profile_open).setOnClickListener {
                Globals.getInstance().setString("CurrentlyWatching", member.userId)
                this.startActivity(Intent(requireActivity(), ProfileActivity::class.java))
            }
            openBestGroup.setOnClickListener {
                if (member.userBestGroup != "0") {
                    Globals.getInstance()
                        .setString("CurrentlyWatchingGroup", member.userBestGroup)
                    requireActivity().startActivity(
                        Intent(
                            requireActivity(),
                            ShowGroupActivity::class.java
                        )
                    )
                }
            }

            view.findViewById<LinearLayout>(R.id.users_in_event_layout).addView(userOneLayoutInEvent)
        })
//        TODO_ "Фильтр по никнейму"
    }
}