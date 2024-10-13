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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
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
import org.w3c.dom.Text

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


        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerWorldCountry)
        viewPager.adapter = MyPagerAdapter()

        viewPager.setPadding(0, 0, 0, 0)
        viewPager.clipToPadding = false
        viewPager.clipChildren = false
        viewPager.offscreenPageLimit = 2

        viewPager.setPageTransformer { page, position ->
            page.alpha = 0.5f + (1 - Math.abs(position))
            val translationX = -position * page.width / 2
            page.translationX = translationX
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        ratingsViewModel.getRating()
                    }
                    1 -> {
                        ratingsViewModel.getRating(true)
                    }
                }
            }
        })

        val usersLayout: LinearLayout = view.findViewById(R.id.usersLayout)
        var currentInRating = false
//        ratingsViewModel.getRating()
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
//                    userLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dirt2_white))
                    currentInRating = true

                    val currentLayout: FrameLayout = view.findViewById(R.id.currentUserInRating)

                    val layout = layoutInflater.inflate(R.layout.layout_user_rating, null)

                    layout.findViewById<TextView>(R.id.userPlace).text = "$lastPlace"//user.toString()
                    layout.findViewById<TextView>(R.id.username).text = users[user].username
                    layout.findViewById<TextView>(R.id.userExperience).text = users[user].experience.toString()

                    Glide.with(this@RatingsFragment)
                        .load(DatabaseMethods.ApplicationDatabaseMethods().getImageLink("users", users[user].userId))
                        .placeholder(R.drawable.baseline_person_24)
                        .circleCrop()
                        .into(layout.findViewById(R.id.userImage))

                    currentLayout.addView(layout)
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
        })
    }

    class MyPagerAdapter : RecyclerView.Adapter<MyPagerAdapter.ViewHolder>() {

        private val items = listOf("В мире", "В Вашей стране")

        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_rating_area, parent, false) as TextView
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = items[position]
        }

        override fun getItemCount(): Int = items.size
    }

}