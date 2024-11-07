package com.gy.ecotrace.ui.education

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.RelativeLayout.getDefaultSize
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.ui.more.ecocalculator.DailyEcoCalcActivity.EcoCalcViewHolder
import com.yandex.mapkit.search.Line

class ShowEducationActivity : AppCompatActivity() {

    private lateinit var viewModel: EducationViewModel

    private fun startControlWork(layout: LinearLayout, data: HashMap<String, DatabaseMethods.DataClasses.EduTask>) {
        for ((num, work) in data) {
            Log.d("task", work.question)
            val workLayout = layoutInflater.inflate(R.layout.layout_education_task, null)
            workLayout.findViewById<TextView>(R.id.taskTitle).text = work.question

            if (work.qType == 0) {
                val options: RadioGroup = workLayout.findViewById(R.id.taskOptionsAns)
                options.visibility = View.VISIBLE

                for (child in 0..<options.childCount) {
                    val option = (options.getChildAt(child) as RadioButton)
                    option.text = work.answers?.get(child) ?: "NONE"

                    option.setOnCheckedChangeListener { compoundButton, b ->
                        Log.d("set ans", "$num -> $child")
                        viewModel.control(num, child)
                    }
                }
            } else if (work.qType == 1) {
                val answer: EditText = workLayout.findViewById(R.id.taskTextAns)
                answer.visibility = View.VISIBLE

                answer.addTextChangedListener(object : TextWatcher{
                    override fun afterTextChanged(p0: Editable?) {
                        viewModel.control(num, p0.toString())
                    }
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                })
            }

            layout.addView(workLayout)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_education)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.endTheTest))

            builder.setMessage(getString(R.string.youWillLoseAllTheProgress))
            builder.setPositiveButton(getString(R.string.confirm)) { dialog, which ->
                onBackPressed()
            }
            builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }

        toolbar.title = intent.getStringExtra("testName")

        val eduType = intent.getStringExtra("edu")
        val assetManager = applicationContext.assets

        val inputStream = assetManager.open("education/$eduType")
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()

        val dataJson = Gson().fromJson(
            String(buffer, Charsets.UTF_8),
            JsonObject::class.java
        )
        val dataQuestions: HashMap<String, DatabaseMethods.DataClasses.EduFacts> =
            Gson().fromJson(dataJson.getAsJsonObject("data").toString(), object :
                TypeToken<HashMap<String, DatabaseMethods.DataClasses.EduFacts>>() {}.type)

        val dataWork: HashMap<String, DatabaseMethods.DataClasses.EduTask> =
            Gson().fromJson(dataJson.getAsJsonObject("control").toString(), object :
                TypeToken<HashMap<String, DatabaseMethods.DataClasses.EduTask>>() {}.type)


        val progress: ProgressBar = findViewById(R.id.progressBar)
        viewModel = EducationViewModel()
        viewModel.count(dataQuestions.size)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val controlLayout: LinearLayout = findViewById(R.id.controlWorkLayout)
        val checkButton: Button = findViewById(R.id.doCheckBtn)
        viewModel.facts.observe(this, Observer {
            if (it) {
                viewPager.visibility = View.GONE
                progress.visibility = View.GONE
                (controlLayout.parent as ScrollView).visibility = View.VISIBLE

                startControlWork(controlLayout, dataWork)
                checkButton.visibility = View.VISIBLE
                checkButton.setOnClickListener {
                    viewModel.controlEnded(dataWork)
                    viewModel.end(eduType!!) {
                        val message = when(it) {
                            true -> "Вам начислены баллы! Вы ответили верно на ${viewModel.corrects()}/${dataWork.size}"
                            else -> "Вы ответили верно на ${viewModel.corrects()}/${dataWork.size}"
                        }
                        Toast.makeText(this@ShowEducationActivity, message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        })

        viewPager.adapter = object : RecyclerView.Adapter<Edu>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Edu {
                val view = if (viewType == 0) {
                    LayoutInflater.from(parent.context).inflate(R.layout.activitylayout_edu_fact, parent, false)
                } else {
                    LayoutInflater.from(parent.context).inflate(R.layout.goto_control, parent, false)
                }
                return Edu(view, applicationContext, viewModel)
            }

            override fun onBindViewHolder(holder: Edu, position: Int) {
                holder.bind(position, dataQuestions)
            }

            override fun getItemCount(): Int = dataQuestions.size + 1

            override fun getItemViewType(position: Int): Int {
                return if (position + 1 == itemCount) 1 else 0
            }

        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                progress.progress = (position / (dataQuestions.size).toFloat() * 100).toInt()
            }
        })

    }

    class Edu(private val view: View, private val context: Context, private val viewModel: EducationViewModel): RecyclerView.ViewHolder(view) {
        fun bind(pos: Int, data: HashMap<String, DatabaseMethods.DataClasses.EduFacts>) {
            val thisPos = pos+1
            val thisPosData = data["${thisPos}"]
            if (itemViewType == 0) {
                val factMainTitle: TextView = view.findViewById(R.id.mainTitleFact)
                val factText: TextView = view.findViewById(R.id.factText)

                factMainTitle.text = thisPosData?.main ?: "NONE"
                factText.text = thisPosData?.fact ?: "NONE"


                val openQuestion = view.findViewById<Button>(R.id.openQuestions)
                val questionLayout = view.findViewById<RelativeLayout>(R.id.questionLayout)
                openQuestion.setOnClickListener {
                    openQuestion.isActivated = !openQuestion.isActivated

                    when (openQuestion.isActivated) {
                        true -> {
                            questionLayout.visibility = View.VISIBLE
                            openQuestion.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_keyboard_arrow_up_24, 0)
                            openQuestion.text = context.getString(R.string.closeQuestion)
                        }
                        false -> {
                            questionLayout.visibility = View.GONE
                            openQuestion.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_keyboard_arrow_down_24, 0)
                            openQuestion.text = context.getString(R.string.openQuestion)
                        }
                    }
                }

                val questionTitle: TextView = view.findViewById(R.id.questionTitle)
                val options: LinearLayout = view.findViewById(R.id.optionsLayout)

                val status: TextView = view.findViewById(R.id.answerStatus)

                questionTitle.text = thisPosData?.question ?: "NONE"
                for (child in 0..<options.childCount) {
                    val option = (options.getChildAt(child) as Button)
                    option.text = thisPosData?.answers?.get(child) ?: "Nothing"

                    option.setOnClickListener {
                        if (viewModel.isUsed(thisPos)) {
                            return@setOnClickListener
                        }

                        thisPosData?.answer?.let { index ->
                            val childView = options.getChildAt(index)
                            val tintColor =
                                ContextCompat.getColorStateList(context, R.color.ok_green)
                            childView?.backgroundTintList = tintColor
                        }

                        if (child != thisPosData?.answer) {
                            val tintColor =
                                ContextCompat.getColorStateList(context, R.color.red_no)
                            option.backgroundTintList = tintColor
                            status.text = context.getString(R.string.unfortunatelyItIsWrongAnswer)
                        }

                        viewModel.used(thisPos)

                        status.visibility = View.VISIBLE
                    }
                }
            } else {
                ((view as RelativeLayout).getChildAt(0) as Button).setOnClickListener {
                    if (!viewModel.isAllCompleted()) {
                        Toast.makeText(context, context.getString(R.string.youHaveNotTakenAllTheTests), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    Log.d("task", "goto")
                    viewModel.goto()
                }
            }
        }

    }
}