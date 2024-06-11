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
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.HorizontalScrollView
//import android.widget.ImageButton
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.ProgressBar
//import android.widget.TextView
//import androidx.appcompat.widget.Toolbar
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.toArgb
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.RecyclerView
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
//import com.bumptech.glide.Glide
//import com.gy.ecotrace.DatabaseMethods
//import com.gy.ecotrace.Globals
//import com.gy.ecotrace.R
//import com.imagekit.android.ImageKit
//import com.imagekit.android.entity.TransformationPosition
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.File
//
//
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"
//
//class ProfileFragment1 : Fragment() {
//    private var param1: String? = null
//    private var param2: String? = null
//    private val globals = Globals.getInstance()
//    private var currentUser = globals.getString("CurrentlyWatching")
//    private var loggedUser = globals.getString("CurrentlyLogged")
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
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
//
//
//    private suspend fun setProfileInfo(f: DatabaseMethods, view: View){
//        // Пользователь
//        f.appCont(requireContext().applicationContext)
////        val image: Bitmap? =
////            withContext(Dispatchers.IO) { f.getUserProfileImage(currentUser) }
////        val imgElement: ImageView =
////            view.findViewById(R.id.profile_image_profile_menu)
////        if (image == null) {
////            imgElement.setImageResource(R.drawable.baseline_person_24)
////        } else {
////            imgElement.setImageBitmap(image)
////        }
//        Glide.with(this@ProfileFragment1)
//            .load(ImageKit.getInstance()
//                .url(
//                    path = "users/$currentUser.png",
//                    transformationPosition = TransformationPosition.QUERY
//                )
//            )
//            .placeholder(R.drawable.baseline_person_24)
//            .error(R.drawable.baseline_person_24)
//            .into(view.findViewById(R.id.profile_image_profile_menu))
//
//        val userInfo: DatabaseMethods.UserInfo? = withContext(Dispatchers.IO) { f.getUserInfo(currentUser) }
//        userInfo!!
//        view.findViewById<TextView>(R.id.username_profile_menu_text).text = userInfo.username
//        view.findViewById<TextView>(R.id.user_about_me_text).text = userInfo.aboutMe
//        view.findViewById<TextView>(R.id.user_country_text).text = userInfo.country.name
//        Log.d("countyrcode", "1")
//        view.findViewById<ImageView>(R.id.imageView).setImageBitmap(getCountryEmoji(userInfo.country.code))
//        Log.d("countyrcode", "2")
//
//
//        // Мероприятия
//        val activities: Array<DatabaseMethods.UserActivity> = withContext(Dispatchers.IO) { f.getUserEvents(currentUser) }
//        if (activities.size > 0) {
//            view.findViewById<TextView>(R.id.text_warning_no_events_joined_yet).visibility = View.GONE
//            view.findViewById<HorizontalScrollView>(R.id.events_scroll_view_profile).visibility = View.VISIBLE
//            val activitiesLayout: LinearLayout = view.findViewById(R.id.activities_layout)
//            for (act in activities) {
//                val activityOneLayout = layoutInflater.inflate(R.layout.layout_user_event, null)
//                activityOneLayout.findViewById<TextView>(R.id.event_name).text = act.event_name
//                val eventStatusString: String = when (act.event_status) {
//                    0 -> "Еще не началось!"
//                    1 -> "Уже проходит!"
//                    2 -> "Закончилось"
//                    else -> "error-unreal-status"
//                }
//                activityOneLayout.findViewById<TextView>(R.id.event_status).text = eventStatusString
//                activityOneLayout.findViewById<TextView>(R.id.event_count_members).text = "${act.event_count_members} участников"
//                activityOneLayout.findViewById<TextView>(R.id.event_user_role).text = "Роль: ${act.roles!!.get("role_${act.event_user_role}")}"
//
//                Glide.with(this@ProfileFragment1)
//                    .load(Globals().getImgUrl("events", act.eventId))
//                    .into(activityOneLayout.findViewById(R.id.event_show_more))
//
//                activitiesLayout.addView(activityOneLayout)
//            }
//        }
//        Log.e("Got act", activities.size.toString())
//
//
//        // Друзья
//        val friends: MutableList<String> = withContext(Dispatchers.IO) { f.getUserFriends(currentUser) }
//        Log.d("Friends", friends.size.toString())
//        if (friends.size > 0) {
//            val friendsLayout: LinearLayout = view.findViewById(R.id.friends_layout)
//            for (fr in friends) {
//                if (fr != currentUser) {
//                    val friendOneLayout = layoutInflater.inflate(R.layout.friend_linear_layout, null)
//                    val friendImg: Bitmap? = withContext(Dispatchers.IO) { f.getUserProfileImage(fr) }
//                    val imgElement: ImageView = friendOneLayout.findViewById(R.id.user_img_friend_layout)
//                    if (friendImg == null) {
//                        imgElement.setImageResource(R.drawable.baseline_person_24)
//                    } else {
//                        imgElement.setImageBitmap(friendImg)
//                    }
//                    val friendInfo: DatabaseMethods.UserInfo? = withContext(Dispatchers.IO) { f.getUserInfo(fr) }
//                    friendOneLayout.findViewById<TextView>(R.id.username_friend_layout).text = friendInfo!!.username
//
//                    friendOneLayout.setOnClickListener{
//                        val myIntent = Intent(requireActivity(), ProfileActivity::class.java)
//                        myIntent.putExtra("previousId", currentUser)
//                        globals.setString("CurrentlyWatching", fr)
//                        this@ProfileFragment1.startActivity(myIntent)
//                    }
//
//                    friendsLayout.addView(friendOneLayout)
//                }
//                if (fr == loggedUser) {
//                    view.findViewById<TextView>(R.id.textView24).text = "Ваш друг"
//                    view.findViewById<ImageButton>(R.id.imageButton2).setImageResource(R.drawable.baseline_person_remove_24)
//                }
//            }
//        }
//
//        // Группы
//        val groups: MutableList<DatabaseMethods.UserGroups> = withContext(Dispatchers.IO) { f.getUserGroups(currentUser) }
//        Log.d("groups", groups.size.toString())
//        if (groups.size > 0) {
//            val groupsLayout: LinearLayout = view.findViewById(R.id.groups_layout)
//            for (gr in groups) {
//                val groupOneLayout = layoutInflater.inflate(R.layout.layout_user_group, null)
//                groupOneLayout.findViewById<TextView>(R.id.group_name).text = gr.name
//                groupOneLayout.findViewById<TextView>(R.id.group_memebrs_count).text = gr.members.toString()
//                Glide.with(this@ProfileFragment1)
//                    .load(Globals().getImgUrl("groups", gr.groupId))
//                    .into(groupOneLayout.findViewById(R.id.group_image))
//
//                groupsLayout.addView(groupOneLayout)
//            }
//        }
//
//    }
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return inflater.inflate(R.layout.fragment_profile, container, false)
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
//        }
//
//        toolbar.inflateMenu(R.menu.popup_menu_profile)
//        toolbar.setOnMenuItemClickListener {
//            when (it.itemId) {
//                R.id.action_settings -> {
//                    val myIntent = Intent(requireActivity(), ChangeProfile::class.java)
//                    this@ProfileFragment1.startActivity(myIntent)
//                    true
//                }
//                R.id.action_logout -> {
//                    val builder = AlertDialog.Builder(requireContext())
//                    builder.setTitle("Выход из аккаунта")
//
//                    builder.setMessage("Вы действительно хотите выйти из своего аккаунта?")
//                    builder.setPositiveButton("Подтвердить") { dialog, which ->
//                        requireActivity().getSharedPreferences("localValues", MODE_PRIVATE).edit().putString("loggedId", "0").apply()
//                        Globals.getInstance().setString("CurrentlyWatching", "0")
//                        requireActivity().finish()
//                    }
//                    builder.setNegativeButton("Отмена") { dialog, which ->
//                        dialog.dismiss()
//                    }
//                    val dialog = builder.create()
//                    dialog.show()
//                    true
//                }
//                else -> {
//                    super.onOptionsItemSelected(it)
//                }
//            }
//        }
//        val f = DatabaseMethods()
//        val profileRefresh = view.findViewById<SwipeRefreshLayout>(R.id.refresh_profile)
//
//        profileRefresh.setColorSchemeResources(R.color.ok_green)
//        profileRefresh.setProgressBackgroundColorSchemeResource(R.color.dirt_white)
//        profileRefresh.setOnRefreshListener {
//            File(requireActivity().applicationContext.filesDir, "${currentUser}_picture.png").delete()
//            File(requireActivity().applicationContext.filesDir, "${currentUser}_info.json").delete()
//
//            view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
//            view.findViewById<RecyclerView>(R.id.content_view).visibility =
//                View.GONE
//            view.findViewById<LinearLayout>(R.id.main_profile).visibility =
//                View.GONE
//            view.findViewById<LinearLayout>(R.id.activities_layout).removeAllViews()
//            view.findViewById<LinearLayout>(R.id.friends_layout).removeAllViews()
//            view.findViewById<LinearLayout>(R.id.groups_layout).removeAllViews()
//
//            lifecycleScope.launch {
//                setProfileInfo(f, view)
//            }
//
//
//            view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
//            view.findViewById<RecyclerView>(R.id.content_view).visibility =
//                View.VISIBLE
//            view.findViewById<LinearLayout>(R.id.main_profile).visibility =
//                View.VISIBLE
//
//            profileRefresh.isRefreshing = false
//        }
//
//
//        lifecycleScope.launch {
//            setProfileInfo(f, view)
//            view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
//            view.findViewById<RecyclerView>(R.id.content_view).visibility =
//                View.VISIBLE
//            view.findViewById<LinearLayout>(R.id.main_profile).visibility =
//                View.VISIBLE
//        }
//    }
//
//
//
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment ProfileFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            ProfileFragment1().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}