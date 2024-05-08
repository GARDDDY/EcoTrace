package com.gy.ecotrace.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.activities.CalculatorActivity
import com.gy.ecotrace.ui.activities.profile.ProfileActivity
import com.gy.ecotrace.ui.more.activities.GroupsSearchAndCreate
import com.gy.ecotrace.ui.more.activities.UsersSearchFriends

class MoreFragment : Fragment() {

    companion object {
        fun newInstance() = MoreFragment()
    }

    private lateinit var moreViewModel: MoreViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val profileBtn = view.findViewById<Button>(R.id.my_profile_button)
        val calcBtn = view.findViewById<Button>(R.id.eco_calc_btn)
        val usersBtn = view.findViewById<Button>(R.id.see_users_button_more_menu)
        val groupsBtn = view.findViewById<Button>(R.id.see_groups_button_more_menu)

        profileBtn.setOnClickListener {
            val openProfile = Intent(requireContext(), ProfileActivity::class.java)
            openProfile.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(openProfile)
        }
        calcBtn.setOnClickListener {
            val openCalculator = Intent(requireContext(), CalculatorActivity::class.java)
            startActivity(openCalculator)
        }
        usersBtn.setOnClickListener {
            val openUsers = Intent(requireContext(), UsersSearchFriends::class.java)
            startActivity(openUsers)
        }
        groupsBtn.setOnClickListener {
            val openGroups = Intent(requireContext(), GroupsSearchAndCreate::class.java)
            startActivity(openGroups)
        }
    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        viewModel = ViewModelProvider(this).get(MoreViewModel::class.java)
//        // TODO: Use the ViewModel
//    }

}