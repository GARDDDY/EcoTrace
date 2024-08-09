//package com.gy.ecotrace.ui.more.profile
//
//import android.app.AlertDialog
//import android.content.Context.MODE_PRIVATE
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.PorterDuff
//import android.graphics.Typeface
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.HorizontalScrollView
//import android.widget.ImageButton
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.ProgressBar
//import android.widget.TextView
//import android.widget.Toolbar
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.toArgb
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
//import com.bumptech.glide.Glide
//import com.google.gson.Gson
//import com.gy.ecotrace.Globals
//import com.gy.ecotrace.db.DatabaseMethods
//import com.gy.ecotrace.R
//import com.gy.ecotrace.db.Repository
//import com.gy.ecotrace.ui.more.events.ShowEventActivity
//import com.gy.ecotrace.ui.more.groups.ShowGroupActivity
//
//class ProfileViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return ProfileViewModel(repository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
//
//class ProfileFragment : Fragment() {
//    private lateinit var userViewModel: ProfileViewModel
//
//    private var currentUser = Globals.getInstance().getString("CurrentlyWatching")
//    private var loggedUser = Globals.getInstance().getString("CurrentlyLogged")
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
//
//        val factory = ProfileViewModelFactory(repository)
//        userViewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        return inflater.inflate(R.layout.activity_profile, container, false)
//    }
//
//    private fun getCountryEmoji(country: String): Bitmap {
//        val flag1 = Character.codePointAt(country, 0) - 0x41 + 0x1F1E6
//        val flag2 = Character.codePointAt(country, 1) - 0x41 + 0x1F1E6
//        val flag = String(Character.toChars(flag1))+String(Character.toChars(flag2))
//
//        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
//        paint.textSize = 100f
//        paint.color = Color.Black.toArgb()
//        paint.textAlign = android.graphics.Paint.Align.LEFT
//        paint.typeface = Typeface.DEFAULT
//
//        val baseline = -paint.ascent()
//        val width = (paint.measureText(flag) + 0.5f).toInt()
//        val height = (baseline + paint.descent() + 0.5f).toInt()
//
//        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(image)
//        canvas.drawText(flag, 0f, baseline, paint)
//        return image
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        val toolbar: Toolbar = view.findViewById(R.id.toolbar_profile)
//        val backIcon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_arrow_back_24)
//        backIcon?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.ok_green), PorterDuff.Mode.SRC_ATOP)
//        toolbar.setNavigationIcon(backIcon)
//        toolbar.setNavigationOnClickListener {
//            requireActivity().onBackPressed()
//        }
//
//        if (currentUser == loggedUser) {
//            view.findViewById<LinearLayout>(R.id.add_user_as_friend).visibility = View.GONE
//            view.findViewById<LinearLayout>(R.id.invite_user_to_group).visibility = View.GONE
//
//            toolbar.inflateMenu(R.menu.popup_menu_profile)
//            toolbar.setOnMenuItemClickListener {
//                when (it.itemId) {
//                    R.id.action_settings -> {
//                        val myIntent = Intent(requireActivity(), ChangeProfile::class.java)
//                        this@ProfileFragment.startActivity(myIntent)
//                        true
//                    }
//                    R.id.action_logout -> {
//                        val builder = AlertDialog.Builder(requireContext())
//                        builder.setTitle("Выход из аккаунта")
//
//                        builder.setMessage("Вы действительно хотите выйти из своего аккаунта?")
//                        builder.setPositiveButton("Подтвердить") { dialog, which ->
//                            requireActivity().getSharedPreferences("localValues", MODE_PRIVATE).edit().putString("loggedId", "0").apply()
//                            Globals.getInstance().setString("CurrentlyWatching", "0")
//                            requireActivity().finish()
//                        }
//                        builder.setNegativeButton("Отмена") { dialog, which ->
//                            dialog.dismiss()
//                        }
//                        val dialog = builder.create()
//                        dialog.show()
//                        true
//                    }
//                    else -> {
//                        super.onOptionsItemSelected(it)
//                    }
//                }
//            }
//        } else {
//            view.findViewById<LinearLayout>(R.id.add_user_as_friend).setOnClickListener{
//                userViewModel.friends.observe(viewLifecycleOwner, Observer {
//                    val anyCommunications = it.indexOfFirst { fship ->  fship.userId == loggedUser }
//
//                    if (anyCommunications != -1) {
//                        val builder = AlertDialog.Builder(requireContext())
//                        builder.setTitle("Удаление из друзей")
//
//                        builder.setMessage("Вы действительно хотите удалить этого пользователя из списка своих друзей?")
//                        builder.setPositiveButton("Подтвердить") { dialog, which ->
//                            userViewModel.removeFriend(currentUser, loggedUser)
//                            requireActivity().recreate()
//                        }
//                        builder.setNegativeButton("Отмена") { dialog, which ->
//                            dialog.dismiss()
//                        }
//                        val dialog = builder.create()
//                        dialog.show()
//                    } else {
//                        val builder = AlertDialog.Builder(requireContext())
//                        builder.setTitle("Добавление в друзья")
//
//                        builder.setMessage("Вы действительно хотите добавить этого пользователя в список своих друзей?")
//                        builder.setPositiveButton("Подтвердить") { dialog, which ->
//                            userViewModel.addFriend(loggedUser, currentUser)
//                            requireActivity().recreate()
//                        }
//                        builder.setNegativeButton("Отмена") { dialog, which ->
//                            dialog.dismiss()
//                        }
//                        val dialog = builder.create()
//                        dialog.show()
//                    }
//                })
//            }
//            view.findViewById<LinearLayout>(R.id.invite_user_to_group).visibility = View.GONE
//        }
//
//        Glide.with(this@ProfileFragment)
//            .load(Globals().getImgUrl("users", currentUser))
//            .placeholder(R.drawable.baseline_person_24)
//            .into(view.findViewById(R.id.profile_image_profile_menu))
//
//
//
//        userViewModel.getUser(currentUser)
//        userViewModel.user.observe(viewLifecycleOwner, Observer { user ->
//            user?.let {
//                view.findViewById<TextView>(R.id.username_profile_menu_text).text = user.username
//                view.findViewById<TextView>(R.id.user_about_me_text).text = user.aboutMe
//                view.findViewById<TextView>(R.id.user_country_text).text = user.country.name
//                view.findViewById<ImageView>(R.id.imageView)
//                    .setImageBitmap(getCountryEmoji(user.country.code))
//
//
//            }
//        })
//        val eventRoles = DatabaseMethods.DataClasses.EventRoles
//        userViewModel.observeEvents(currentUser)
//        userViewModel.events.observe(viewLifecycleOwner, Observer { events ->
//            events?.let{
//            if (events.size > 0) {
//                view.findViewById<TextView>(R.id.text_warning_no_activity).visibility = View.GONE
//                view.findViewById<HorizontalScrollView>(R.id.events_scroll_view_profile).visibility =
//                    View.VISIBLE
//                val activitiesLayout: LinearLayout = view.findViewById(R.id.activities_layout)
//                activitiesLayout.removeAllViews()
//                for (act in events) {
//                    val activityOneLayout = layoutInflater.inflate(R.layout.layout_user_event, null)
//                    activityOneLayout.findViewById<TextView>(R.id.event_name).text =
//                        act.eventInfo.eventName
//                    val eventStatusString: String = when (act.eventInfo.eventStatus) {
//                        0 -> "Еще не началось!"
//                        1 -> "Уже проходит!"
//                        2 -> "Закончилось"
//                        else -> "error-unreal-status"
//                    }
//                    activityOneLayout.findViewById<TextView>(R.id.event_status).text =
//                        eventStatusString
//                    activityOneLayout.findViewById<TextView>(R.id.event_count_members).text =
//                        "${act.eventInfo.eventCountMembers} участников"
//                    activityOneLayout.findViewById<TextView>(R.id.event_user_role).text =
//                        "Роль: "//${eventRoles[act.eventInfo.eventUsersToTheirRoles!!.get(currentUser)!!]}"
//
//                    Glide.with(this@ProfileFragment)
//                        .load(Globals().getImgUrl("events", "${act.eventInfo.eventId}"))
//                        .placeholder(R.drawable.round_family_restroom_24)
//                        .into(activityOneLayout.findViewById(R.id.event_show_more))
//
//                    activityOneLayout.setOnClickListener {
//                        Globals.getInstance()
//                            .setString("CurrentlyWatchingEvent", act.eventInfo.eventId)
//                        val eventIntent = Intent(requireActivity(), ShowEventActivity::class.java)
//                        eventIntent.putExtra("eInfo", Gson().toJson(act))
//                        this@ProfileFragment.startActivity(eventIntent)
//                    }
//
//                    activitiesLayout.addView(activityOneLayout)
//                }
//            }
//        }
//
//        })
//        userViewModel.getFriends(currentUser)
//        userViewModel.friends.observe(viewLifecycleOwner, Observer { friends ->
//            if (friends.size > 0) {
//            val friendsLayout: LinearLayout = view.findViewById(R.id.friends_layout)
//            for (fr in friends) {
//                if (fr.userId != currentUser) {
//                    if (fr.sender && !fr.friend && currentUser == loggedUser || fr.friend) {
//                        val friendOneLayout =
//                            layoutInflater.inflate(R.layout.friend_linear_layout, null)
//                        friendOneLayout.findViewById<TextView>(R.id.username_friend_layout).text =
//                            fr.username
//
//                        friendOneLayout.setOnClickListener {
//                            val myIntent = Intent(requireActivity(), ProfileActivity::class.java)
//                            myIntent.putExtra("previousId", currentUser)
//                            Globals.getInstance().setString("CurrentlyWatching", fr.userId)
//                            this@ProfileFragment.startActivity(myIntent)
//                        }
//
//                        Glide.with(this@ProfileFragment)
//                            .load(Globals().getImgUrl("users", "${fr.userId}"))
//                            .placeholder(R.drawable.baseline_person_24)
//                            .into(friendOneLayout.findViewById(R.id.user_img_friend_layout))
//
//                        friendsLayout.addView(friendOneLayout)
//                    }
//                    if (fr.userId == loggedUser) {
//                        view.findViewById<TextView>(R.id.textView24).text = if(fr.friend) "Ваш друг" else "Отправлен"
//                        view.findViewById<ImageButton>(R.id.imageButton2)
//                            .setImageResource(R.drawable.baseline_person_remove_24)
//                    }
//                }
//            }
//        }})
//        userViewModel.observeGroups(currentUser)
//        userViewModel.groups.observe(viewLifecycleOwner, Observer { groups ->
//            groups?.let {
//                if (groups.size > 0) {
//                    val groupsLayout: LinearLayout = view.findViewById(R.id.groups_layout)
//                    groupsLayout.removeAllViews()
//                    for (gr in groups) {
//                        val groupOneLayout = layoutInflater.inflate(R.layout.layout_user_group, null)
//                        groupOneLayout.findViewById<TextView>(R.id.group_name).text = gr.groupInfo.groupName
//                        groupOneLayout.findViewById<TextView>(R.id.group_memebrs_count).text = gr.groupInfo.groupCountMembers.toString()
//                        Glide.with(this@ProfileFragment)
//                            .load(Globals().getImgUrl("groups", gr.groupInfo.groupId))
//                            .into(groupOneLayout.findViewById(R.id.group_image))
//
//                        groupOneLayout.setOnClickListener {
//                            Globals.getInstance().setString("CurrentlyWatchingGroup", gr.groupInfo.groupId)
//                            val eventIntent = Intent(requireActivity(), ShowGroupActivity::class.java)
//                            eventIntent.putExtra("gInfo", Gson().toJson(gr))
//                            this@ProfileFragment.startActivity(eventIntent)
//                        }
//
//                        groupsLayout.addView(groupOneLayout)
//                    }
//            }
//        }
//        })
//
//        userViewModel.additionalData.observe(viewLifecycleOwner, Observer {
//            if (it) {
//                view.findViewById<SwipeRefreshLayout>(R.id.refresh_profile).visibility = View.VISIBLE
//                view.findViewById<ProgressBar>(R.id.progressBar_loading).visibility = View.GONE
//            }
//        })
//    }
//}