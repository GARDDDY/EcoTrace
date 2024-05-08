package com.gy.ecotrace.ui.activities.profile

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.gy.ecotrace.FirebaseMethods
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SignUpFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private suspend fun createNewUser(username: String, fullname: String, gender: Int, country: String, email: String, password: String): String {
        if (FirebaseMethods().isUser(username, email)) return "0"
        val indexes: DatabaseReference = FirebaseDatabase.getInstance().getReference("indexes")
        val database : DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
        val unique = database.push().key!!
        val dataInfo = FirebaseMethods.UserInfo(username, fullname, gender, country)
        val dataPrivate = FirebaseMethods.UserPrivate(email.replace(".", "!"), FirebaseMethods().hash256(password))
        val dataRules = FirebaseMethods.UserRules()
        indexes.child(email.replace(".", "!")).setValue(unique)
        indexes.child(username).setValue(unique)
        var result = database.child(unique).setValue(dataInfo)
        result.addOnFailureListener{ err ->
            Log.d("NEW USER", "dataInfo $err")
        }
        result = database.child(unique).child("private").setValue(dataPrivate)
        result.addOnFailureListener{ err ->
            Log.d("NEW USER", "dataPrivate $err")
        }
        result = database.child(unique).child("rules").setValue(dataRules)
        result.addOnFailureListener{ err ->
            Log.d("NEW USER", "dataRules $err")
        }

        return unique
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val replacer: Button = view.findViewById(R.id.login_register_button_transfer)

        replacer.setOnClickListener {
            val replacerHub = activity as SignInUpHub
            replacerHub.replaceFragment(SignInFragment())
        }

        val registerBtn: Button = view.findViewById(R.id.register_signup_menu_btn)
        val emailEntry: EditText = view.findViewById(R.id.login_signup_menu_entry)
        val passwordEntry: EditText = view.findViewById(R.id.password_signup_menu_entry)
        val usernameEntry: EditText = view.findViewById(R.id.username_signup_menu_entry)
        val fullnameEntry: EditText = view.findViewById(R.id.fullname_signup_menu_entry)
        val genderSwitcher: Switch = view.findViewById(R.id.gender_singup_menu_choose)
        registerBtn.setOnClickListener {
            GlobalScope.launch {
                val newId = createNewUser(usernameEntry.text.toString(), fullnameEntry.text.toString(), if (genderSwitcher.isActivated) 1 else 0, "", emailEntry.text.toString(), passwordEntry.text.toString())
                if (newId == "0") {

                } else {
                    requireActivity().getSharedPreferences("localValues", Context.MODE_PRIVATE).edit().putString("loggedId", newId).apply()
                    Globals.getInstance().setString("CurrentlyLogged", newId)
                    Globals.getInstance().setString("CurrentlyWatching", newId)
                    requireActivity().finish()
                }
            }
        }

    }
}