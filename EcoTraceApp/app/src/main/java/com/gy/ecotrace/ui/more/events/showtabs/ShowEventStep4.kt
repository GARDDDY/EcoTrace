package com.gy.ecotrace.ui.more.events.showtabs

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.ShowGroupActivity
import com.gy.ecotrace.ui.more.profile.ProfileActivity

class ShowEventStep4 : Fragment() {

    private val sharedViewModel: ShowEventViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private val delay: Long = 300
    private var runnable: Runnable? = null

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

        val eventRoles: Array<String> = Globals.getInstance().getEventRoles()
        val allMembers = view.findViewById<LinearLayout>(R.id.mainMembersLayout)
        sharedViewModel.getEventMembers()
        sharedViewModel.members.observe(viewLifecycleOwner, Observer{
            Log.d("users", it.toString())
            it?.let {
                view.findViewById<ScrollView>(R.id.loadingLayout).visibility = View.GONE
                allMembers.removeAllViews()

                if (it.size == 0) {
                    // no one
                }

                for (user in it) {
                    val userOneLayoutInEvent =
                        layoutInflater.inflate(R.layout.layout_user_in_event, null)
                    userOneLayoutInEvent.findViewById<TextView>(R.id.username_user_in_event_layout).text =
                        user.username
                    userOneLayoutInEvent.findViewById<TextView>(R.id.user_role_in_event_user_in_event_layout).text =
                        eventRoles[user.role]

                    userOneLayoutInEvent.findViewById<TextView>(R.id.user_rank_user_in_event_layout).text =
                        "none"//Globals().rankToString(user.experience)
                    userOneLayoutInEvent.findViewById<TextView>(R.id.user_experience_user_in_event_layout).text =
                        user.experience.toString()

                    val manage = userOneLayoutInEvent.findViewById<ImageButton>(R.id.manageUser)
                    if (user.role == 0 ||
                        (sharedViewModel.event.value?.eventInfo?.eventCreatorId ?: "-") != FirebaseAuth.getInstance().currentUser?.uid){
                        manage.visibility = View.GONE
                    }

                    manage.setOnClickListener {
                        val popupView = layoutInflater.inflate(R.layout.layout_menu_user_manage_in_event, null)
                        val popupWindow = PopupWindow(
                            popupView,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            true
                        )
                        popupView.findViewById<TextView>(R.id.username).text = user.username

                        val toMember = popupView.findViewById<TextView>(R.id.tomember)
                        val toHelper = popupView.findViewById<TextView>(R.id.tohelper)

                        if (user.role == 2) toMember.visibility = View.GONE
                        if (user.role == 1) toHelper.visibility = View.GONE

                        toMember.setOnClickListener {
                            sharedViewModel.setUserRole(user.userId, 2)
                        }
                        toHelper.setOnClickListener {
                            sharedViewModel.setUserRole(user.userId, 1)
                        }

                        val location = IntArray(2)
                        manage.getLocationOnScreen(location)
                        popupWindow.showAtLocation(manage, Gravity.NO_GRAVITY,
                            location[0], location[1] + manage.height)
                    }

                    Glide.with(requireActivity())
                        .load(Globals().getImgUrl("users", user.userId))
                        .circleCrop()
                        .placeholder(R.drawable.baseline_person_24)
                        .into(userOneLayoutInEvent.findViewById(R.id.user_img_user_in_event_layout))


                    userOneLayoutInEvent.findViewById<LinearLayout>(R.id.profile_open)
                        .setOnClickListener {
                            Globals.getInstance().setString("CurrentlyWatching", user.userId)
                            this.startActivity(
                                Intent(
                                    requireActivity(),
                                    ProfileActivity::class.java
                                )
                            )
                        }
                    allMembers.addView(userOneLayoutInEvent)
                }
            }
        })

        view.findViewById<EditText>(R.id.nameSearcher).addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                view.findViewById<ScrollView>(R.id.loadingLayout).visibility = View.VISIBLE
                allMembers.visibility = View.GONE
                runnable?.let { handler.removeCallbacks(it) }
                runnable = Runnable {
                    sharedViewModel.getEventMembers(p0.toString())
                }
                handler.postDelayed(runnable!!, delay)
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }
}