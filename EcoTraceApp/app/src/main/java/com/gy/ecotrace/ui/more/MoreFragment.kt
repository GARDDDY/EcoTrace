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
import com.google.firebase.auth.FirebaseAuth
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.ecocalculator.EcologicalCalculatorActivity
import com.gy.ecotrace.ui.more.events.CreateEventActivity
import com.gy.ecotrace.ui.more.events.ShowAllEventsActivity
import com.gy.ecotrace.ui.more.friends.UsersSearchFriends
import com.gy.ecotrace.ui.more.groups.ShowAllGroupsActivity
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import com.gy.ecotrace.ui.more.profile.SignInUpHub

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
        if ((FirebaseAuth.getInstance().currentUser?.uid ?: "0") == "0") {
            view.findViewById<TextView>(R.id.openProfileDescriptionText).text =
                "У вас еще нет аккаунта, создайте его, нажав сюда!"
        }
        view.findViewById<LinearLayout>(R.id.profileOpenLayout).setOnClickListener {
            val currentUser = Globals.getInstance().getString("CurrentlyWatching")
            val currentLogged = FirebaseAuth.getInstance().currentUser?.uid ?: "0"
            if (currentUser == "0") {
                if (currentLogged == "0") {
                    startActivity(Intent
                        (
                            requireActivity(),
                            SignInUpHub::class.java
                        )
                    )
                    return@setOnClickListener
                } else {
                    Globals.getInstance().setString("CurrentlyWatching", currentLogged)
                }
            }

            startActivity(Intent
                (
                requireActivity(),
                ProfileActivity::class.java
                )
            )
        }
        view.findViewById<LinearLayout>(R.id.ecologicalCalculatorOpenLayout).setOnClickListener {
            startActivity(
                Intent(
                    requireActivity(),
                    EcologicalCalculatorActivity::class.java)
            )
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
            startActivity(
                Intent(
                    requireActivity(),
                    ShowAllGroupsActivity::class.java)
            )
        }
        view.findViewById<LinearLayout>(R.id.settingsOpenLayout).setOnClickListener {
            Toast.makeText(context, "Настройки", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.howToUseOpenLayout).setOnClickListener {
            Toast.makeText(context, "Как пользоваться", Toast.LENGTH_SHORT).show()
        }
    }
}