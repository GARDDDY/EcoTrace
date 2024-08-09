package com.gy.ecotrace.ui.more.groups.createsteps

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.CreateGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel

class CreateGroupStep1 : Fragment() {

    private lateinit var sharedViewModel: CreateGroupViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedViewModel = ViewModelProvider(requireActivity(), CreateGroupViewModelFactory(
            Repository(
                DatabaseMethods.UserDatabaseMethods(),
                DatabaseMethods.ApplicationDatabaseMethods()
            )
        )
        )[CreateGroupViewModel::class.java]
        return inflater.inflate(R.layout.activitylayout_create_group_step1, container, false)
    }

    private fun isGoodName(name: String): Boolean {
        for (symbol in arrayOf( '.', '#', '$', '[', ']')) {
            if (name.contains(symbol)) return false
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupName: EditText = view.findViewById(R.id.groupName)

        groupName.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if (p0.toString().length <= 1) {
                    return
                }
                if (!isGoodName(p0.toString())) {
                    Toast.makeText(
                        context,
                        "В названии группы не могут содержаться символы  ., #, \$, [, ]",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                sharedViewModel.isGroupNameAvailable(p0.toString()) {
                    if (it) {
                        sharedViewModel.applyGroupData(p0.toString())
                    } else {
                        Toast.makeText(
                            context,
                            "Такое название группы уже занято!",
                            Toast.LENGTH_LONG
                        ).show()
                        groupName.setText(sharedViewModel.groupData.value?.groupName ?: "")
                    }
                }
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        val groupAbout: EditText = view.findViewById(R.id.groupAbout)
        groupAbout.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                sharedViewModel.applyGroupData(groupAbout = p0.toString())
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }


}