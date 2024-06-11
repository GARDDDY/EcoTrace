package com.gy.ecotrace.ui.more

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
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
        val profileBtn = view.findViewById<Button>(R.id.my_profile_button)
        val calcBtn = view.findViewById<Button>(R.id.eco_calc_btn)
        val usersBtn = view.findViewById<Button>(R.id.see_users_button_more_menu)
//        val groupsBtn = view.findViewById<Button>(R.id.see_groups_button_more_menu)

        profileBtn.setOnClickListener {
            val openProfile = Intent(requireContext(), ProfileActivity::class.java)
            if (Globals.getInstance().getString("CurrentlyLogged") == "0") {
                openProfile.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(openProfile)
        }
//        calcBtn.setOnClickListener {
//            val openCalculator = Intent(requireContext(), CalculatorActivity::class.java)
//            startActivity(openCalculator)
//        }
        usersBtn.setOnClickListener {
            val openUsers = Intent(requireContext(), UsersSearchFriends::class.java)
            startActivity(openUsers)
        }
//        groupsBtn.setOnClickListener {
//            val openGroups = Intent(requireContext(), GroupsSearchAndCreate::class.java)
//            startActivity(openGroups)
//        }
    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        viewModel = ViewModelProvider(this).get(MoreViewModel::class.java)
//        // TODO: Use the ViewModel
//    }

}