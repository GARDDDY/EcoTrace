package com.gy.ecotrace.ui.ratings

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.dto.RatingItem
import com.gy.ecotrace.sections.rating.RatingViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RatingsFragment : Fragment() {

    private val ratingViewModel: RatingViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private val medalDrawables = mapOf(
        1 to R.drawable.round_looks_one_24,
        2 to R.drawable.round_looks_two_24,
        3 to R.drawable.baseline_looks_3_24
    )

    private fun avatarUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" }

    private fun flagEmoji(countryCode: String?): String? {
        if (countryCode.isNullOrBlank()) return null
        return try {
            val flag1 = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val flag2 = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            String(Character.toChars(flag1)) + String(Character.toChars(flag2))
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_ratings, container, false)

    private fun bindRow(row: View, place: Int, item: RatingItem, isSelf: Boolean) {
        val placeView = row.findViewById<TextView>(R.id.userPlace)
        val medalView = row.findViewById<ImageView>(R.id.userMedal)
        val medalRes = medalDrawables[place]
        if (medalRes != null) {
            medalView.setImageResource(medalRes)
            medalView.visibility = View.VISIBLE
            placeView.visibility = View.INVISIBLE
        } else {
            placeView.text = place.toString()
            placeView.visibility = View.VISIBLE
            medalView.visibility = View.INVISIBLE
        }

        row.findViewById<TextView>(R.id.username).text = item.username
        row.findViewById<TextView>(R.id.userExperience).text = item.experience.toString()
        row.findViewById<TextView>(R.id.userCountryFlag).apply {
            val flag = flagEmoji(item.countryCode)
            visibility = if (flag != null) View.VISIBLE else View.GONE
            text = flag
        }
        Glide.with(this)
            .load(avatarUrl(item.avatarKey))
            .placeholder(R.drawable.baseline_person_24)
            .circleCrop()
            .into(row.findViewById(R.id.userImage))

        row.setBackgroundColor(
            ContextCompat.getColor(requireContext(), if (isSelf) R.color.dirt2_white else R.color.transparent)
        )
        row.setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatching", item.id.toString())
            startActivity(Intent(requireActivity(), ProfileActivity::class.java))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUserId = tokenStore.userId

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerWorldCountry)
        viewPager.adapter = MyPagerAdapter()
        viewPager.setPadding(0, 0, 0, 0)
        viewPager.clipToPadding = false
        viewPager.clipChildren = false
        viewPager.offscreenPageLimit = 2
        viewPager.setPageTransformer { page, position ->
            page.alpha = 0.5f + (1 - Math.abs(position))
            page.translationX = -position * page.width / 2
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) ratingViewModel.loadWorld() else ratingViewModel.loadMyCountry()
            }
        })
        if (currentUserId <= 0) viewPager.visibility = View.GONE

        val usersLayout: LinearLayout = view.findViewById(R.id.usersLayout)
        val currentUserFrame: FrameLayout = view.findViewById(R.id.currentUserInRating)
        val scrollView: ScrollView = view.findViewById(R.id.scrollView2)

        var selfRowView: View? = null
        fun updateSelfPinnedVisibility() {
            val row = selfRowView
            currentUserFrame.visibility =
                if (row != null && !row.getLocalVisibleRect(Rect())) View.VISIBLE else View.GONE
        }

        scrollView.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val sv = v as ScrollView
            if (!ratingViewModel.isUpdating && ratingViewModel.hasMore) {
                if (scrollY >= sv.getChildAt(0).height - sv.height) {
                    ratingViewModel.loadMore()
                }
            }
            updateSelfPinnedVisibility()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    ratingViewModel.isLoading.collect { loading ->
                        view.findViewById<ShimmerFrameLayout>(R.id.ratingLoading).visibility =
                            if (loading) View.VISIBLE else View.GONE
                        scrollView.visibility = if (loading) View.GONE else View.VISIBLE
                        if (loading) currentUserFrame.visibility = View.GONE else updateSelfPinnedVisibility()
                    }
                }
                launch {
                    ratingViewModel.users.collect { users ->
                        usersLayout.removeAllViews()
                        currentUserFrame.removeAllViews()
                        selfRowView = null

                        var place = 0
                        for (i in users.indices) {
                            if (i == 0 || users[i - 1].experience != users[i].experience) place++
                            val user = users[i]
                            val isSelf = user.id == currentUserId

                            val row = layoutInflater.inflate(R.layout.layout_user_rating, usersLayout, false)
                            bindRow(row, place, user, isSelf)
                            usersLayout.addView(row)

                            if (isSelf) {
                                selfRowView = row
                                val pinned = layoutInflater.inflate(R.layout.layout_user_rating, currentUserFrame, false)
                                bindRow(pinned, place, user, true)
                                currentUserFrame.addView(pinned)
                            }
                        }

                        scrollView.post { updateSelfPinnedVisibility() }
                    }
                }
            }
        }
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
