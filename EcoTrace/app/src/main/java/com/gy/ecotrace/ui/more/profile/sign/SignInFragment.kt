package com.gy.ecotrace.ui.more.profile.sign

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gy.ecotrace.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class SignInFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    private val sharedViewModel: SignHubViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val replacerHub = activity as SignInUpHub

        val loginEntry: EditText = view.findViewById(R.id.loginData)
        val passwordEntry: EditText = view.findViewById(R.id.passwordData)
        val loginBtn: Button = view.findViewById(R.id.loginButton)
        val forgotPass: TextView = view.findViewById(R.id.forgotPassword) // Alert code entry

        view.findViewById<TextView>(R.id.gotoRegister).setOnClickListener {
            replacerHub.replaceFragment(SignUpFragment())
        }


        forgotPass.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireActivity())

            builder.setTitle("Сброс пароля")
            builder.setMessage("Введите адрес электронной почты, привязанной к вашему аккаунту")

            val viewLayout = layoutInflater.inflate(R.layout.layout_forgot_password, null)
            val emailEntry = viewLayout.findViewById<EditText>(R.id.emailAddress)
            val emailApply = viewLayout.findViewById<ImageButton>(R.id.applyEmail)
            emailEntry.addTextChangedListener(object : TextWatcher{
                override fun afterTextChanged(p0: Editable?) {
                    if ((p0?.length ?: 0) > 6) {
                        emailApply.visibility = View.VISIBLE
                    } else {
                        emailApply.visibility = View.GONE
                    }
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            emailApply.setOnClickListener {
                sharedViewModel.sendForgotCode(emailEntry.text.toString()) {
                    if (it) {
                        viewLayout.findViewById<LinearLayout>(R.id.emailEnter).visibility =
                            View.GONE

                        viewLayout.findViewById<TextView>(R.id.codeSentTo).text =
                            emailEntry.text.toString()
                        builder.setMessage("Введите полученный код, высланный на указанный адрес электронной почты")
                        viewLayout.findViewById<LinearLayout>(R.id.codeEnter).visibility =
                            View.VISIBLE
                    } else {
                        Toast.makeText(requireActivity(), "Не удалось отправить код!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val codeEntry = viewLayout.findViewById<EditText>(R.id.emailCode)
            codeEntry.addTextChangedListener(object : TextWatcher{
                override fun afterTextChanged(p0: Editable?) {
                    if ((p0?.length ?: 0) == 6) {
                        codeEntry.clearFocus()
                        sharedViewModel.checkCode(emailEntry.text.toString(), p0.toString().toInt()) {
                            if (it) {
                                viewLayout.findViewById<LinearLayout>(R.id.codeEnter).visibility =
                                    View.GONE
                                builder.setMessage("Придумайте новый пароль для входа в свой аккаунт")
                                viewLayout.findViewById<LinearLayout>(R.id.passwordChange).visibility =
                                    View.VISIBLE
                            } else {
                                Toast.makeText(requireActivity(), "Вы ввели неверный код!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            val newPassword = viewLayout.findViewById<EditText>(R.id.newPassword)
            newPassword.addTextChangedListener(object : TextWatcher{
                override fun afterTextChanged(p0: Editable?) {
                    // ??
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })
            builder.setView(viewLayout)
            val dialog = builder.create()
            val applyPassword = viewLayout.findViewById<ImageButton>(R.id.applyPassword)
            applyPassword.setOnClickListener {
                sharedViewModel.applyPassword(emailEntry.text.toString(), codeEntry.text.toString().toInt(), newPassword.text.toString()) {
                    if (it) {
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireActivity(), "Вы ввели неверный код!", Toast.LENGTH_SHORT).show()
                    }
                }
            }




            dialog.show()
        }

        loginBtn.setOnClickListener {
            replacerHub.signIn(loginEntry.text.toString(), passwordEntry.text.toString())
        }
    }
}
