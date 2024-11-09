package com.gy.ecotrace.ui.more.ecocalculator

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class DailyEcoCalcActivity : AppCompatActivity() {

    private val viewModel: DailyCalcViewModel by viewModels()
    private var holder: EcoCalcViewHolder? = null
    private var confirmedUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daily_eco_calc)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val calcType = intent.getIntExtra("calcType", -1)
        if (calcType !in 0..4) {
            Toast.makeText(applicationContext, getString(R.string.dataWasNotGiven), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val calcNameView = findViewById<TextView>(R.id.calcName)
        calcNameView.text = getString(CALC_NAMES[calcType])

        val dataQuestions = loadQuestions(calcType)
        viewModel.init(calcType, dataQuestions.size)

        val questionContainer: FrameLayout = findViewById(R.id.questionContainer)
        val back = findViewById<ImageButton>(R.id.gotoBack)
        val forward = findViewById<ImageButton>(R.id.gotoForward)
        val saveAndExit: MaterialButton = findViewById(R.id.saveAndExit)

        fun updateNavButtons() {
            back.visibility = if (viewModel.currentPage == 0) View.GONE else View.VISIBLE
            forward.visibility = if (viewModel.currentPage == dataQuestions.size - 1) View.GONE else View.VISIBLE
        }

        viewModel.state.observe(this) { state ->
            if (holder != null) return@observe
            if (state == null) {
                Toast.makeText(this, getString(R.string.dataWasNotGiven), Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            confirmedUpdate = !state.updating
            if (state.updating) {
                calcNameView.text = "${getString(CALC_NAMES[calcType])}${getString(R.string.dataAddition)}"
                Toast.makeText(this, getString(R.string.youAreUpdatingYourDataOnlyNewChanges), Toast.LENGTH_LONG).show()
            }

            val averagesByQuestion = state.averages.associate { it.question to Pair(it.value.toInt(), it.count) }
            val view = LayoutInflater.from(this)
                .inflate(R.layout.activitylayout_ecological_calculator_layout, questionContainer, false)
            questionContainer.addView(view)

            holder = EcoCalcViewHolder(view, this, averagesByQuestion, viewModel)
            holder!!.bind(viewModel.currentPage, dataQuestions)
            updateNavButtons()
        }

        forward.setOnClickListener {
            viewModel.currentPage += 1
            holder?.bind(viewModel.currentPage, dataQuestions)
            updateNavButtons()
        }
        back.setOnClickListener {
            viewModel.currentPage -= 1
            holder?.bind(viewModel.currentPage, dataQuestions)
            updateNavButtons()
        }

        saveAndExit.setOnClickListener {
            val isUpd = viewModel.state.value?.updating == true
            if (!isUpd && !viewModel.isEnoughAnswers()) {
                Toast.makeText(this, getString(R.string.youHaveNotAnsweredAllQuestion), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isUpd && !confirmedUpdate) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.dataUpdate))
                builder.setMessage(getString(R.string.makeSureYouHaveUpdatedYouDataByAddingOnlyNewChanges))
                builder.setPositiveButton(R.string.confirm) { dialog, _ ->
                    confirmedUpdate = true
                    dialog.dismiss()
                    save()
                }
                builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                builder.create().show()
                return@setOnClickListener
            }
            save()
        }

        viewModel.end.observe(this) { enough ->
            saveAndExit.icon = if (enough) ContextCompat.getDrawable(this, R.drawable.baseline_check_24) else null
        }
    }

    private fun save() {
        viewModel.submit { message ->
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadQuestions(calcType: Int): HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion> {
        val fileName = "ecological-calculators/${calcType}1.json"
        val inputStream = applicationContext.assets.open(fileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()

        val dataJson = Gson().fromJson(String(buffer, Charsets.UTF_8), JsonObject::class.java)
        return Gson().fromJson(
            dataJson.getAsJsonObject("tests").toString(),
            object : TypeToken<HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>>() {}.type
        )
    }

    class EcoCalcViewHolder(
        private val view: View,
        private val context: Context,
        private val averagesByQuestion: Map<Int, Pair<Int, Int>>,
        private val viewModel: DailyCalcViewModel
    ) {
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
            questions: HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>
        ) {
            val item = questions["${pos + 1}"]!!
            val thisClass = viewModel.getAnswer(pos)

            val minValueValue = item.sliders.minValue
            val maxValueValue = item.sliders.maxValue
            val baseline = viewModel.todayBaselineFor(pos)
            val alreadyAnswered = viewModel.hasAnswer(pos)
            val state = viewModel.state.value
            val daysSince = state?.daysSince ?: 0

            testQuestion.text = when {
                state?.first == true -> item.qNoData
                baseline == null && daysSince > 1 -> item.qNoDataInTime
                    .replace("%d", daysSince.toString())
                    .replace("%s", dayWord(context, daysSince))
                else -> item.qData
            }

            minValue.text = minValueValue.toString()
            minType.text = item.sliders.valueType
            maxValue.text = maxValueValue.toString()
            maxType.text = item.sliders.valueType
            currentType.text = item.sliders.valueType
            specifyCurrentValue.text = item.sliders.valueType

            specifier.visibility = if (item.specify != null) View.VISIBLE else View.GONE
            if (item.specify != null) {
                specifier.setOnClickListener {
                    showSpecifyPopup(item, thisClass, Pair(minValueValue, maxValueValue))
                }
            }

            seekBar.min = minValueValue
            seekBar.max = maxValueValue

            if (!alreadyAnswered) {
                val averageSeed = averagesByQuestion[pos]
                when {
                    baseline != null -> {
                        thisClass.value = baseline.value.roundToInt()
                        if (!baseline.specify.isNullOrEmpty()) {
                            thisClass.specify.clear()
                            baseline.specify.forEach { (key, v) -> thisClass.specify[key] = v.roundToInt() }
                            thisClass.useSpecify = true
                        }
                        viewModel.addAnswer(thisClass)
                    }
                    (averageSeed?.second ?: 0) != 0 -> {
                        thisClass.value = averageSeed?.first ?: 0
                        viewModel.addAnswer(thisClass)
                    }
                    else -> thisClass.value = 0
                }
            }
            thisClass.question = pos

            seekBar.progress = thisClass.value
            currentValue.setText(seekBar.progress.toString())
            applySpecifyVisualState(thisClass)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    currentValue.setText(p1.toString())
                    item.dataValue = p1
                    thisClass.useSpecify = false
                    thisClass.value = p1
                    applySpecifyVisualState(thisClass)
                    viewModel.addAnswer(thisClass)
                }
            })

            currentValue.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    seekBar.progress = p0.toString().toIntOrNull() ?: 0
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })
        }

        private fun applySpecifyVisualState(thisClass: DatabaseMethods.DataClasses.EcoCalcSaveData) {
            val useSpecify = thisClass.useSpecify
            warningUseSpecify.visibility = if (useSpecify) View.VISIBLE else View.GONE
            warningUseGeneral.visibility = if (useSpecify) View.VISIBLE else View.GONE
            specifyCurrent.visibility = if (useSpecify) View.VISIBLE else View.GONE
            specifyCurrentValue.visibility = if (useSpecify) View.VISIBLE else View.GONE
            if (useSpecify) {
                specifyCurrent.text = thisClass.specify.values.sum().toString()
            }
            currentValue.paintFlags = if (useSpecify) Paint.STRIKE_THRU_TEXT_FLAG else 0
            currentType.paintFlags = if (useSpecify) Paint.STRIKE_THRU_TEXT_FLAG else 0
        }

        private fun showSpecifyPopup(
            specify: DatabaseMethods.DataClasses.EcoCalcQuestion,
            thisClass: DatabaseMethods.DataClasses.EcoCalcSaveData,
            minMax: Pair<Int, Int>
        ) {
            val valueType = specify.sliders.valueType
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_calc_specify, null)
            val totalView = dialogView.findViewById<TextView>(R.id.specifyDialogTotal)
            val itemsContainer = dialogView.findViewById<LinearLayout>(R.id.specifyItemsContainer)
            val doneButton = dialogView.findViewById<MaterialButton>(R.id.specifyDoneButton)

            fun updateTotal() {
                totalView.text = "${context.getString(R.string.total)}: ${thisClass.specify.values.sum()} $valueType"
            }

            (specify.specify ?: hashMapOf()).forEach { spec ->
                val row = LayoutInflater.from(context).inflate(R.layout.layout_specify_item_row, itemsContainer, false)
                val valueView = row.findViewById<TextView>(R.id.specifyItemValue)
                val rowSeekBar = row.findViewById<SeekBar>(R.id.specifyItemSeekBar)

                row.findViewById<TextView>(R.id.specifyItemName).text = spec.value.name
                rowSeekBar.min = minMax.first
                rowSeekBar.max = minMax.second
                rowSeekBar.progress = thisClass.specify[spec.key] ?: 0
                valueView.text = "${rowSeekBar.progress} $valueType"

                rowSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        valueView.text = "$p1 $valueType"
                        thisClass.specify[spec.key] = p1
                        thisClass.useSpecify = true
                        updateTotal()
                        applySpecifyVisualState(thisClass)
                        viewModel.addAnswer(thisClass)
                    }
                })

                itemsContainer.addView(row)
            }
            updateTotal()

            val dialog = BottomSheetDialog(context)
            dialog.setContentView(dialogView)
            doneButton.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }
}
