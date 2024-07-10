package com.gy.ecotrace.ui.more

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.events.CreateEventActivity
import com.gy.ecotrace.ui.more.events.ShowAllEventsActivity
import com.gy.ecotrace.ui.more.friends.UsersSearchFriends
import com.gy.ecotrace.ui.more.profile.ProfileActivity

//import com.gy.ecotrace.ui.more.friends.UsersSearchFriends

class MoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Globals.getInstance().setString("CurrentlyWatching", requireActivity().getSharedPreferences("localValues", MODE_PRIVATE).getString("loggedId", "0")!!)
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Globals.getInstance().getString("CurrentlyLogged") == "0") {
            view.findViewById<TextView>(R.id.openProfileDescriptionText).text =
                "У вас еще нет аккаунта, создайте его, нажав сюда!"
        }
        view.findViewById<LinearLayout>(R.id.profileOpenLayout).setOnClickListener {
            val openProfile = Intent(requireContext(), ProfileActivity::class.java)
            if (Globals.getInstance().getString("CurrentlyLogged") == "0") {
                openProfile.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(openProfile)
        }
        view.findViewById<LinearLayout>(R.id.ecologicalCalculatorOpenLayout).setOnClickListener {
            Toast.makeText(context, "Экологический калькулятор", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.friendsOpenLayout).setOnClickListener {
            startActivity(
                Intent(
                    requireActivity(),
                    UsersSearchFriends::class.java)
            )
        }
        view.findViewById<LinearLayout>(R.id.allEventsOpenLayout).setOnClickListener {
            startActivity(
                Intent(
                    requireActivity(),
                    ShowAllEventsActivity::class.java)
            )
        }
        view.findViewById<LinearLayout>(R.id.allGroupsOpenLayout).setOnClickListener {
            Toast.makeText(context, "Друзья", Toast.LENGTH_SHORT).show()
//            startActivity(
//                Intent(
//                    requireActivity(),
//                    ShowAllGroupsActivity::class.java)
//            )
        }
        view.findViewById<LinearLayout>(R.id.settingsOpenLayout).setOnClickListener {
            Toast.makeText(context, "Настройки", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.howToUseOpenLayout).setOnClickListener {
            Toast.makeText(context, "Как пользоваться", Toast.LENGTH_SHORT).show()
        }
    }
}