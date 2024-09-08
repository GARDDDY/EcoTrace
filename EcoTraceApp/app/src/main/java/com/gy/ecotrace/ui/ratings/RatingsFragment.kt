package com.gy.ecotrace.ui.ratings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.databinding.FragmentRatingsBinding
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.CreateGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity

class RatingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ratings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: "0"

        val repository = Repository(
            DatabaseMethods.UserDatabaseMethods(),
            DatabaseMethods.ApplicationDatabaseMethods()
        )
        val factory = RatingsViewModelFactory(repository)
        val ratingsViewModel = ViewModelProvider(this, factory)[RatingsViewModel::class.java]

        val usersLayout: LinearLayout = view.findViewById(R.id.usersLayout)
        var currentInRating = false
        ratingsViewModel.getRating(currentUser)
        ratingsViewModel.users.observe(viewLifecycleOwner, Observer { users ->
            view.findViewById<ShimmerFrameLayout>(R.id.ratingLoading).visibility = View.GONE
            Log.d("rating", users.toString())
            usersLayout.removeAllViews()
            var currentCount = 1
            var currentFirst = 0
            var lastPlace = 0
            for (user in users.indices) {

                if (user == 0 ||
                    users[user-1].experience != users[user].experience)
                {
                    currentCount = users.count { it.experience == users[user].experience }
                    Log.d("lp", lastPlace.toString())
                    lastPlace++
                }

                val userLayout = layoutInflater.inflate(R.layout.layout_user_rating, null)

                if (users[user].userId == currentUser) {
                    userLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dirt2_white))
                    currentInRating = true
                }

                Glide.with(this@RatingsFragment)
                    .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", users[user].userId))
                    .placeholder(R.drawable.baseline_person_24)
                    .circleCrop()
                    .into(userLayout.findViewById(R.id.userImage))


                userLayout.findViewById<TextView>(R.id.username).text = users[user].username
                userLayout.findViewById<TextView>(R.id.userExperience).text = users[user].experience.toString()
                userLayout.findViewById<TextView>(R.id.userPlace).text = "$lastPlace"

                userLayout.setOnClickListener {
                    Globals.getInstance().setString("CurrentlyWatching", users[user].userId)
                    startActivity(Intent(requireActivity(), ProfileActivity::class.java))
                }

                usersLayout.addView(userLayout)
            }

            if (!currentInRating) {
                val currentLayout: FrameLayout = view.findViewById(R.id.currentUserInRating)
                val userLayout = layoutInflater.inflate(R.layout.layout_user_rating, null)

                currentLayout.addView(userLayout)
            }
        })
    }
}