package com.gy.ecotrace.ui.activities.profile

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.gy.ecotrace.FirebaseMethods
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SignInFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    suspend fun loginIntoAccount(login: String, password: String): String {
        return suspendCoroutine {
            val indexes: DatabaseReference = FirebaseDatabase.getInstance().getReference("indexes").child(login)
            indexes.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val id = snapshot.getValue().toString()
                    if (id.isNotEmpty()) {
                        val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(id)
                        database.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val storedPassword = userSnapshot.child("password").value.toString()
                                if (storedPassword == FirebaseMethods().hash256(password)) {
                                    it.resume(id)
                                } else {
                                    it.resume("0")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                it.resume("0")
                            }
                        })
                    } else {
                        it.resume("0")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    it.resume("0")
                }
            })
        }
    }

    private fun saveLocalInfo(userId: String) {

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val replacer: Button = view.findViewById(R.id.login_register_button_transfer)

        replacer.setOnClickListener {
            val replacerHub = activity as SignInUpHub
            replacerHub.replaceFragment(SignUpFragment())
        }

        val loginEntry: EditText = view.findViewById(R.id.email_login_login_menu_entry)
        val passwordEntry: EditText = view.findViewById(R.id.password_login_menu_entry)
        val showPassword: ImageButton = view.findViewById(R.id.show_password_login_menu_btn)
        val loginBtn: Button = view.findViewById(R.id.login_login_menu_btn)

        val forgotPass: Button = view.findViewById(R.id.forgot_login_menu_btn) // Alert code entry

        showPassword.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                passwordEntry.transformationMethod = null
            } else if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL) {
                passwordEntry.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            true
        }


        forgotPass.setOnClickListener {

        }

        loginBtn.setOnClickListener {
            GlobalScope.launch {
                val status = loginIntoAccount(loginEntry.text.toString(), passwordEntry.text.toString())
                Log.e("Account", status)
                when (status){
                    "0" -> Toast.makeText(this@SignInFragment.requireContext(), "Не удалось войти в аккаунт!", Toast.LENGTH_SHORT).show()
                    else -> {
                        Log.e("Account", status)
                        requireActivity().getSharedPreferences("localValues", Context.MODE_PRIVATE).edit().putString("loggedId", status).apply()
                        Globals.getInstance().setString("CurrentlyLogged", status)
                        Globals.getInstance().setString("CurrentlyWatching", status)
                        requireActivity().finish()
                    }
                }
            }
        }
    }
}