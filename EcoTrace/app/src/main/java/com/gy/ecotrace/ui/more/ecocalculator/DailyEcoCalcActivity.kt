package com.gy.ecotrace.ui.more.ecocalculator

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import java.util.Stack

class DailyEcoCalcActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daily_eco_calc)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        val calcType = intent.getIntExtra("calcType", -1)
        val calcNameToItsType = arrayOf(
            getString(R.string.food),
            getString(R.string.water),
            getString(R.string.trash),
            getString(R.string.energy),
            getString(R.string.transport)
        )
        if (calcType == -1) {
            Toast.makeText(applicationContext, getString(R.string.dataWasNotGiven), Toast.LENGTH_SHORT)
                .show()
            finish()
        }

        val averages = intent.getStringExtra("calcAverages") ?: ""
        val averagesDataMap = Gson().fromJson(averages, JsonObject::class.java)
        val isUpd = averagesDataMap.get("upd").toString().toBoolean()
        val isFirst = averagesDataMap.get("first").toString().toBoolean()
        val noInDays = averagesDataMap.get("days").toString().toInt()
        val averagesData = (Gson().fromJson(averagesDataMap?.get("data"), JsonObject::class.java)
            ?: JsonObject()).asMap().map { it.value.toString() }.toMutableList()

        findViewById<TextView>(R.id.calcName).text =
            "${calcNameToItsType[calcType]}${if (isUpd) getString(R.string.dataAddition) else ""}"
        if (isUpd) {
            Toast.makeText(
                this@DailyEcoCalcActivity,
                getString(R.string.youAreUpdatingYourDataOnlyNewChanges),
                Toast.LENGTH_LONG
            ).show()
        }
        val assetManager = applicationContext.assets
        val fileName = "ecological-calculators/${calcType}1.json"

        val inputStream = assetManager.open(fileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()

        val dataJson = Gson().fromJson(
            String(buffer, Charsets.UTF_8),
            JsonObject::class.java
        )
        val dataQuestions: HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion> =
            Gson().fromJson(dataJson.getAsJsonObject("tests").toString(), object :
                TypeToken<HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>>() {}.type)
//        val dataFormulas: HashMap<String, DatabaseMethods.DataClasses.Formula> = Gson().fromJson(dataJson.getAsJsonObject("formulas").toString(), object :
//            TypeToken<HashMap<String, DatabaseMethods.DataClasses.Formula>>() {}.type)


        val repository = Repository(
            DatabaseMethods.UserDatabaseMethods(),
            DatabaseMethods.ApplicationDatabaseMethods()
        )
        val factory = EcoCalcViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[EcoCalcViewModel::class.java]
        viewModel.init(dataQuestions.size)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = false
        viewPager.adapter = object : RecyclerView.Adapter<EcoCalcViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EcoCalcViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.activitylayout_ecological_calculator_layout, parent, false)
                return EcoCalcViewHolder(
                    view,
                    applicationContext,
                    findViewById(R.id.main),
                    averagesData,
                    viewModel,
                    isFirst,
                    noInDays
                )
            }

            override fun onBindViewHolder(holder: EcoCalcViewHolder, position: Int) {
                holder.bind(position, dataQuestions, viewModel)
            }

            override fun getItemCount(): Int = dataQuestions.size
        }


        val back = findViewById<ImageButton>(R.id.gotoBack)
        val forward = findViewById<ImageButton>(R.id.gotoForward)
        forward.setOnClickListener {
            viewModel.currentPage += 1
            if (viewModel.currentPage == dataQuestions.size - 1) {
                forward.visibility = View.GONE
            }
            viewPager.currentItem = viewModel.currentPage
            back.visibility = View.VISIBLE
        }
        back.setOnClickListener {
            viewModel.currentPage -= 1
            if (viewModel.currentPage == 0) {
                back.visibility = View.GONE
            }
            viewPager.currentItem = viewModel.currentPage
            forward.visibility = View.VISIBLE
        }

        var updating = !isUpd
        val saveAndExit: Button = findViewById(R.id.saveAndExit)
        saveAndExit.setOnClickListener {
            if (!isUpd && !viewModel.isEnoughAnswers()) {
                Toast.makeText(this, getString(R.string.youHaveNotAnsweredAllQuestion), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isUpd && !updating) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.dataUpdate))
                builder.setMessage(getString(R.string.makeSureYouHaveUpdatedYouDataByAddingOnlyNewChanges))
                builder.setPositiveButton(R.string.confirm) { dialog, _ ->
                    updating = true
                    dialog.dismiss()
                }
                builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
            }
            if (updating) {
                viewModel.saveData(calcType) {
                    runOnUiThread {
                        Toast.makeText(this@DailyEcoCalcActivity, it, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
        viewModel.end.observe(this, Observer {
            Log.d("end", "$it")
            if (it) {
                saveAndExit.foregroundTintList =
                    ContextCompat.getColorStateList(applicationContext, R.color.ok_green)
            }
        })

    }

    class EcoCalcViewHolder(
        private val view: View,
        private val context: Context,
        private val mainLayout: ConstraintLayout,
        private val averages: MutableList<String>,
        private val viewModel: EcoCalcViewModel,
        private val isFirst: Boolean,
        private val noInDays: Int
    ) : RecyclerView.ViewHolder(view) {
        private val testQuestion: TextView = view.findViewById(R.id.testQuestion)
        private val specifier: TextView = view.findViewById(R.id.specifyData)
        private val seekBar: SeekBar = view.findViewById(R.id.seekBar)

        val currentValue: EditText = view.findViewById(R.id.currentValue)
        val currentType: TextView = view.findViewById(R.id.valueTypeToCurrent)
        val minValue: TextView = view.findViewById(R.id.minValue)
        val minType: TextView = view.findViewById(R.id.valueTypeToMin)
        val maxValue: TextView = view.findViewById(R.id.maxValue)
        val maxType: TextView = view.findViewById(R.id.valueTypeToMax)

        val warningUseSpecify: TextView = view.findViewById(R.id.hintUsedSpecify)
        val warningUseGeneral: TextView = view.findViewById(R.id.hintToUseGeneral)

        val specifyCurrent: TextView = view.findViewById(R.id.currentValueSpecify)
        val specifyCurrentValue: TextView = view.findViewById(R.id.valueTypeToCurrentSpecify)

        fun bind(
            pos: Int,
            questions: HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>,
            viewModel: EcoCalcViewModel
        ) {
            val item = questions["${pos + 1}"]!!
            val thisClass = viewModel.getAnswer(pos)

            val minValueValue = item.sliders.minValue
            val maxValueValue = item.sliders.maxValue

            testQuestion.text =
                if (isFirst) item.qNoData else if (noInDays > 1) item.qNoDataInTime.replace(
                    "%s %d",
                    getDays(noInDays)
                ) else item.qData

            // sliders texts
            minValue.text = minValueValue.toString()
            minType.text = item.sliders.valueType
            maxValue.text = maxValueValue.toString()
            maxType.text = item.sliders.valueType
            currentType.text = item.sliders.valueType

            if (item.specify != null) {
                specifier.visibility = View.VISIBLE
                specifier.setOnClickListener {
                    showSpecifyPopup(item, thisClass, Pair(minValueValue, maxValueValue), view)
                }
            }

            seekBar.min = minValueValue
            seekBar.max = maxValueValue

            val gotValues = if (!isFirst) Regex("(\\d+);(\\d+)").find(averages[pos]) else null

            seekBar.progress = gotValues?.groupValues?.get(1)?.toInt() ?: 0
            currentValue.setText(seekBar.progress.toString())
            thisClass.value = seekBar.progress
            thisClass.question = pos

            if ((gotValues?.groupValues?.get(2)?.toInt() ?: 0) != 0) {
                viewModel.addAnswer(thisClass)
            }



            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    currentValue.setText(p1.toString())
                    item.dataValue = p1
                    item.useSpecify = false
                    warningUseSpecify.visibility = View.GONE
                    warningUseGeneral.visibility = View.GONE

                    specifyCurrent.visibility = View.GONE
                    specifyCurrentValue.visibility = View.GONE

                    currentValue.paintFlags = 0
                    currentType.paintFlags = 0

                    thisClass.useSpecify = false
                    thisClass.value = p1

                    viewModel.addAnswer(thisClass)
                }
            })

            currentValue.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    seekBar.progress = p0.toString().toInt()
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })
        }

        private fun showSpecifyPopup(
            specify: DatabaseMethods.DataClasses.EcoCalcQuestion,
            thisClass: DatabaseMethods.DataClasses.EcoCalcSaveData,
            minMax: Pair<Int, Int>,
            holder: View
        ) {
            val mainFrame = FrameLayout(context).apply {
                setBackgroundColor(Color.parseColor("#75000000"))
            }
            val popupView = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            for (spec in specify.specify ?: hashMapOf()) {
                val specLayout = LayoutInflater.from(context)
                    .inflate(R.layout.layout_specify_ecocalc, null)
                specLayout.findViewById<TextView>(R.id.specifyType).text = spec.value.name
                specLayout.findViewById<TextView>(R.id.valueTypeToMin).text =
                    specify.sliders.valueType
                val minValue = specLayout.findViewById<TextView>(R.id.minValue)
                minValue.text = minMax.first.toString()
                specLayout.findViewById<TextView>(R.id.valueTypeToMax).text =
                    specify.sliders.valueType
                val maxValue = specLayout.findViewById<TextView>(R.id.maxValue)
                maxValue.text = minMax.second.toString()
                specLayout.findViewById<TextView>(R.id.valueTypeToCurrent).text =
                    specify.sliders.valueType
                val seekBar: SeekBar = specLayout.findViewById(R.id.seekBar)
                val currValue = specLayout.findViewById<EditText>(R.id.currentValue)
                seekBar.min = minMax.first
                seekBar.max = minMax.second
                seekBar.progress = thisClass.specify[spec.key] ?: 0
                currValue.setText(seekBar.progress.toString())

                minValue.setOnClickListener {
                    seekBar.progress = seekBar.min
                    spec.value.value = seekBar.min
                }
                maxValue.setOnClickListener {
                    seekBar.progress = seekBar.max
                    spec.value.value = seekBar.max
                }

                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        currValue.setText(p1.toString())
                        thisClass.specify[spec.key] = p1
                        warningUseSpecify.visibility = View.VISIBLE
                        warningUseGeneral.visibility = View.VISIBLE

                        specifyCurrent.visibility = View.VISIBLE
                        specifyCurrentValue.visibility = View.VISIBLE
                        specifyCurrent.text = thisClass.specify.values.sumOf { it }.toString()
                        specifyCurrentValue.text = specify.sliders.valueType

                        currentValue.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        currentType.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG


                        thisClass.useSpecify = true

                        viewModel.addAnswer(thisClass)
                    }
                })

                currValue.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                        seekBar.progress = p0.toString().toInt()
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                })

                popupView.addView(specLayout)
            }


            mainFrame.addView(
                popupView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            val popupWindow = PopupWindow(
                mainFrame,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                true
            )
            popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0)
        }

        private fun getDays(days: Int): String {
            return when {
                days % 10 == 1 && days % 100 != 11 -> context.getString(R.string.dayiped)
                days % 10 in 2..4 && (days % 100 < 10 || days % 100 >= 20) -> context.getString(R.string.dayrped)
                else -> context.getString(R.string.dayrpgetmn)
            }
        }

    }
}