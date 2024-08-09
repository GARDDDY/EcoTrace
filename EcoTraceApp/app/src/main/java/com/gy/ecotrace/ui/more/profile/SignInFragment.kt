package com.gy.ecotrace.ui.more.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.text.TextWatcher
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
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class SignInFragment : Fragment() {
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
                                val storedPassword = userSnapshot.child("private/password").value.toString()
                                if (storedPassword != null){ //== DatabaseMethods().hash256(password)) {
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
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val replacerHub = activity as SignInUpHub

        val loginEntry: EditText = view.findViewById(R.id.loginData)
        val passwordEntry: EditText = view.findViewById(R.id.passwordData)
        val loginBtn: Button = view.findViewById(R.id.loginButton)
        val forgotPass: TextView = view.findViewById(R.id.forgotPassword) // Alert code entry

        view.findViewById<ImageButton>(R.id.showPasswordButton).setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                passwordEntry.transformationMethod = null
            } else if (motionEvent.action == MotionEvent.ACTION_UP ||
                motionEvent.action == MotionEvent.ACTION_CANCEL)
            {
                passwordEntry.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            true
        }

        view.findViewById<TextView>(R.id.gotoRegister).setOnClickListener {
            replacerHub.replaceFragment(SignUpFragment())
        }


        forgotPass.setOnClickListener {

        }

        loginBtn.setOnClickListener {
            replacerHub.signIn(loginEntry.text.toString(), passwordEntry.text.toString())
        }
    }
}
