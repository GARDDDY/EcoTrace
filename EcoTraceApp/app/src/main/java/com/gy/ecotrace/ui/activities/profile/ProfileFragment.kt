package com.gy.ecotrace.ui.activities.profile

import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.gy.ecotrace.FirebaseMethods
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private val globals = Globals.getInstance()
    private var currentUser = globals.getString("CurrentlyWatching")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private suspend fun setProfileInfo(f: FirebaseMethods, view: View){
        f.appCont(requireContext().applicationContext)
        val image: Bitmap? =
            withContext(Dispatchers.IO) { f.getUserProfileImage(currentUser) }
        val imgElement: ImageView =
            view.findViewById(R.id.profile_image_profile_menu)
        if (image == null) {
            imgElement.setImageResource(R.drawable.baseline_person_24)
        } else {
            imgElement.setImageBitmap(image)
        }

        val userInfo: FirebaseMethods.UserInfo? = withContext(Dispatchers.IO) { f.getUserInfo(currentUser) }
        userInfo!!
        view.findViewById<TextView>(R.id.username_profile_menu_text).text = userInfo.username
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val swipeLayout = view.findViewById<SwipeRefreshLayout>(R.id.refresh_profile)
        swipeLayout.setOnRefreshListener(object : OnRefreshListener{
            override fun onRefresh() {
                // удалить локальный файл текущего юзера
                TODO("Not yet implemented")
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar: Toolbar = view.findViewById(R.id.toolbar_profile)
        val backIcon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.ok_green), PorterDuff.Mode.SRC_ATOP)
        toolbar.setNavigationIcon(backIcon)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        toolbar.inflateMenu(R.menu.popup_menu_profile)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_settings -> {
                    val myIntent = Intent(requireActivity(), ChangeProfile::class.java)
                    this@ProfileFragment.startActivity(myIntent)
                    true
                }
                R.id.action_logout -> {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Выход из аккаунта")

                    builder.setMessage("Вы действительно хотите выйти из своего аккаунта?")
                    builder.setPositiveButton("Подтвердить") { dialog, which ->
                        requireActivity().getSharedPreferences("localValues", MODE_PRIVATE).edit().putString("loggedId", "0").apply()
                        Globals.getInstance().setString("CurrentlyWatching", "0")
                        requireActivity().recreate()
                    }
                    builder.setNegativeButton("Отмена") { dialog, which ->
                        dialog.dismiss()
                    }
                    val dialog = builder.create()
                    dialog.show()
                    true
                }
                else -> {
                    super.onOptionsItemSelected(it)
                }
            }
        }
        val f = FirebaseMethods()
        val profileRefresh = view.findViewById<SwipeRefreshLayout>(R.id.refresh_profile)
        profileRefresh.setOnRefreshListener {
            Log.d("refresh", "true")
            view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
            view.findViewById<RecyclerView>(R.id.content_view).visibility =
                View.GONE
            view.findViewById<LinearLayout>(R.id.main_profile).visibility =
                View.GONE
            lifecycleScope.launch {
                setProfileInfo(f, view)
            }
            view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            view.findViewById<RecyclerView>(R.id.content_view).visibility =
                View.VISIBLE
            view.findViewById<LinearLayout>(R.id.main_profile).visibility =
                View.VISIBLE

            profileRefresh.isRefreshing = false
        }
        lifecycleScope.launch {
            // проверить в локальных файлах
            Log.wtf("Profile ID", currentUser)
            setProfileInfo(f, view)

            view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            view.findViewById<RecyclerView>(R.id.content_view).visibility =
                View.VISIBLE
            view.findViewById<LinearLayout>(R.id.main_profile).visibility =
                View.VISIBLE
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}