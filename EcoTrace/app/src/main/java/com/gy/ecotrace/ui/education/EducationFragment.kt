package com.gy.ecotrace.ui.education

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gy.ecotrace.R
import com.gy.ecotrace.databinding.FragmentEducationBinding
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.yandex.mapkit.search.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class EducationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_education, container, false)
    }

    private data class MainData(
        val name: String,
        val imageUrl: String
    )

    private data class EduJson(
        val main: MainData
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allEducations: LinearLayout = view.findViewById(R.id.allEducations)

        val assetManager = requireContext().assets
        assetManager.list("education")?.forEach { file ->
            try {
                val inputStream = assetManager.open("education/$file")
                val reader = InputStreamReader(inputStream)
                val gson = Gson()
                val jsonRoot = gson.fromJson(reader, EduJson::class.java)

                val mainData = jsonRoot.main

                reader.close()

                val eduLayout = layoutInflater.inflate(R.layout.layout_eduaction, null)
                eduLayout.findViewById<TextView>(R.id.educationName).text = mainData.name
                Glide.with(this)
                    .load(mainData.imageUrl)
                    .placeholder(R.drawable.baseline_image_24)
                    .into(eduLayout.findViewById(R.id.educationImage))

                eduLayout.setOnClickListener {
                    val edu = Intent(requireActivity(), ShowEducationActivity::class.java)
                    edu.putExtra("testName", mainData.name)
                    edu.putExtra("edu", file)
                    startActivity(edu)
                }

                getEduStatus(Regex("\\d+").find(file)!!.value.toInt()) {
                    requireActivity().runOnUiThread {
                        val status = eduLayout.findViewById<ImageView>(R.id.eduStatus)
                        val loading = eduLayout.findViewById<ShimmerFrameLayout>(R.id.eduLoading)
                        Log.d("edu stayusi", "$it")

                        loading.visibility = View.GONE
                        status.visibility = View.VISIBLE
                        it?.let {
                            when (it) {
                                false -> status.imageTintList = ContextCompat.getColorStateList(
                                    requireContext(),
                                    R.color.red_no
                                )

                                true -> status.imageTintList = ContextCompat.getColorStateList(
                                    requireContext(),
                                    R.color.ok_green
                                )
                            }
                        }
                    }
                }

                allEducations.addView(eduLayout)
            } catch (e: Exception) {
                Log.e("file", "Error reading or parsing file: $file", e)
            }
        }

    }


    private fun getEduStatus(edu: Int, callback: (Boolean?) -> Unit) {
        GlobalScope.launch {
            val data = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods()).getEduStatus(edu)

            withContext(Dispatchers.IO) {
                callback(data)
            }
        }
    }

}