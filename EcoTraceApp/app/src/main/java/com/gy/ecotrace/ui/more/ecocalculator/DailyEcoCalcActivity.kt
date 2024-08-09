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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.ViewPager2
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.util.Stack
import kotlin.math.exp
import kotlin.math.min

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

        if (calcType == -1) {
            Toast.makeText(applicationContext, "Данные не были переданы!", Toast.LENGTH_SHORT).show()
            finish()
        }

        val calcNameToItsType = arrayOf("Пища", "Вода", "Отходы", "Энергия", "Транспорт")

        findViewById<TextView>(R.id.calcName).text = calcNameToItsType[calcType]

        val assetManager = applicationContext.assets
        val fileName = "ecological-calculators/${calcType}1.json"

            val inputStream = assetManager.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            val dataJson = Gson().fromJson(
                String(buffer, Charsets.UTF_8),
                JsonObject::class.java)
            val dataQuestions: HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion> = Gson().fromJson(dataJson.getAsJsonObject("tests").toString(), object :
                TypeToken<HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>>() {}.type)
            val dataFormulas: HashMap<String, DatabaseMethods.DataClasses.Formula> = Gson().fromJson(dataJson.getAsJsonObject("formulas").toString(), object :
                TypeToken<HashMap<String, DatabaseMethods.DataClasses.Formula>>() {}.type)

            val viewPager: ViewPager2 = findViewById(R.id.viewPager)
            viewPager.adapter = EcoCalcAdapter(dataQuestions, dataFormulas, applicationContext, findViewById(R.id.main))

        val saveAndExit: Button = findViewById(R.id.saveAndExit)
        saveAndExit.setOnClickListener {
            DatabaseMethods.UserDatabaseMethods().saveEcoCalc(dataQuestions, dataFormulas)
        }

    }

    class EcoCalcAdapter(private val items: HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>,
                         private val formulas: HashMap<String, DatabaseMethods.DataClasses.Formula>,
                         private val appContext: Context, private val main: ConstraintLayout)
        : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        class NormalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val testQuestion: TextView = view.findViewById(R.id.testQuestion)
            val specifier: TextView = view.findViewById(R.id.specifyData)
            val seekBar: SeekBar = view.findViewById(R.id.seekBar)

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
        }

        class LastPageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val mainL: LinearLayout = view.findViewById(R.id.mainLayout)
        }

        private companion object {
            const val VIEW_TYPE_NORMAL = 0
            const val VIEW_TYPE_LAST = 1
        }

        override fun getItemViewType(position: Int): Int {
            Log.d("page", "$position ${itemCount-1}")
            return if (position >= itemCount - 1) VIEW_TYPE_LAST else VIEW_TYPE_NORMAL
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_LAST) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.activity_daily_eco_calc_results, parent, false)
                LastPageViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.activitylayout_ecological_calculator_layout, parent, false)
                NormalViewHolder(view)
            }
        }

        private fun showSpecifyPopup(specify: DatabaseMethods.DataClasses.EcoCalcQuestion, minMax: Pair<Int, Int>, holder: ViewHolder) {
            val mainFrame = FrameLayout(appContext).apply {
                setBackgroundColor(Color.parseColor("#75000000"))
            }
            val popupView = LinearLayout(appContext).apply {
                orientation = LinearLayout.VERTICAL
            }

            for (spec in specify.specify ?: hashMapOf()) {
                val specLayout = LayoutInflater.from(appContext)
                    .inflate(R.layout.layout_specify_ecocalc, null)
                specLayout.findViewById<TextView>(R.id.specifyType).text = spec.value.name
                specLayout.findViewById<TextView>(R.id.valueTypeToMin).text = specify.sliders.valueType
                val minValue = specLayout.findViewById<TextView>(R.id.minValue)
                minValue.text = minMax.first.toString()
                specLayout.findViewById<TextView>(R.id.valueTypeToMax).text = specify.sliders.valueType
                val maxValue = specLayout.findViewById<TextView>(R.id.maxValue)
                maxValue.text = minMax.second.toString()
                specLayout.findViewById<TextView>(R.id.valueTypeToCurrent).text = specify.sliders.valueType
                val seekBar: SeekBar = specLayout.findViewById(R.id.seekBar)
                val currValue = specLayout.findViewById<TextView>(R.id.currentValue)
                seekBar.min = minMax.first
                seekBar.max = minMax.second
                seekBar.progress = spec.value.value ?: 0
                currValue.text = seekBar.progress.toString()

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
                        spec.value.value = p1
                        specify.useSpecify = true
                        (holder as NormalViewHolder).warningUseSpecify.visibility = View.VISIBLE
                        holder.warningUseGeneral.visibility = View.VISIBLE

                        holder.specifyCurrent.visibility = View.VISIBLE
                        holder.specifyCurrent.text = specify.specify!!.values.sumOf { it.value ?: 0 }.toString()
                        holder.specifyCurrentValue.visibility = View.VISIBLE
                        holder.specifyCurrentValue.text = specify.sliders.valueType
                        holder.currentValue.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        holder.currentType.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        currValue.text = p1.toString()
                    }
                })

                popupView.addView(specLayout)
            }


            mainFrame.addView(popupView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))
            val popupWindow = PopupWindow(mainFrame, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, true)
            popupWindow.showAtLocation(main, Gravity.CENTER, 0, 0)
        }

        private fun workWithData(data: HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>, formula: String, useSpecify: Boolean): String? {
            var endFormula = formula

            for (match in Regex("data\\[(\\d+)](?:\\.(\\w+))?(?:\\[(\\d+)])?").findAll(formula)) {
                val questionIndex = match.groupValues[1]
                val attribute = match.groupValues[2]
                val specifyIndex = match.groupValues[3]

                Log.d("test", "$match $attribute  $specifyIndex")

                val questionData = data[questionIndex]
                if (useSpecify && !questionData!!.useSpecify) {
                    return null
                }
                val replaceValue = when {
                    specifyIndex.isNotEmpty() -> questionData?.specify?.get(specifyIndex)?.value.toString()
                    attribute == "sum" -> {
                        if (questionData?.dataValue != null) {
                            if (questionData.specify != null) {
                                questionData.specify?.values?.sumOf { it.value ?: 0 }.toString()
                            } else questionData.dataValue.toString()
                        } else "null"
                    }
                    attribute == "value" -> questionData?.dataValue.toString()
                    attribute == "specify" -> {
                        questionData!!.specify!![specifyIndex]!!.value.toString()
                    }
                    else -> "NONE"
                }

                endFormula = endFormula.replace(match.value, replaceValue)
            }

            return endFormula
        }

        private fun calculate(expression: String): Float {
            fun calcPolish(tokens: List<String>): Float {
                val stack = Stack<Float>()
                for (token in tokens) {
                    when {
                        token.toFloatOrNull() != null -> stack.push(token.toFloat())
                        token == "+" -> stack.push(stack.pop() + stack.pop())
                        token == "-" -> stack.push(-stack.pop() + stack.pop())
                        token == "*" -> stack.push(stack.pop() * stack.pop())
                        token == "/" -> stack.push(1 / stack.pop() * stack.pop())
                        else -> throw IllegalArgumentException("Unknown operator: $token")
                    }
                }
                if (stack.size != 1) {
                    throw IllegalStateException("Invalid RPN expression")
                }
                return stack.pop()
            }
            fun toPolish(infix: String): List<String> {
                val output = mutableListOf<String>()
                val operators = Stack<String>()

                val precedence = mapOf(
                    "+" to 1,
                    "-" to 1,
                    "*" to 2,
                    "/" to 2
                )

                val tokens = infix.replace("(", " ( ").replace(")", " ) ").split(" ").filter { it.isNotEmpty() }

                for (token in tokens) {
                    when {
                        token.toFloatOrNull() != null -> output.add(token)
                        token == "(" -> operators.push(token)
                        token == ")" -> {
                            while (operators.isNotEmpty() && operators.peek() != "(") {
                                output.add(operators.pop())
                            }
                            if (operators.isEmpty() || operators.peek() != "(") {
                                throw IllegalStateException("Mismatched parentheses")
                            }
                            operators.pop()
                        }
                        else -> {
                            while (operators.isNotEmpty() &&
                                (precedence[operators.peek()] ?: 0) >= (precedence[token] ?: 0)
                            ) {
                                output.add(operators.pop())
                            }
                            operators.push(token)
                        }
                    }
                }

                while (operators.isNotEmpty()) {
                    val op = operators.pop()
                    if (op == "(" || op == ")") {
                        throw IllegalStateException("Mismatched parentheses")
                    }
                    output.add(op)
                }
                println(output)
                return output
            }

            Log.d("npm got expression", expression)
            return calcPolish(toPolish(expression))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == VIEW_TYPE_LAST) {
                val lastHolder = holder as LastPageViewHolder
                lastHolder.mainL.removeAllViews()
//
                for (form in formulas.toSortedMap()) {

                    val value = workWithData(items, form.value.formula, form.value.useSpecify)
                    if (value != null && Regex("[A-Za-z]+").find(value) == null) {

                        val formulaLayout = LayoutInflater.from(appContext)
                            .inflate(R.layout.layout_eco_calc_formula_result, null)

                        formulaLayout.findViewById<TextView>(R.id.formulaDescription).text = form.value.description
                        formulaLayout.findViewById<TextView>(R.id.formulaValues).text = value
                        val formulaValue = calculate(value)
                        formulaLayout.findViewById<TextView>(R.id.formulaValue).text = formulaValue.toString()
                        form.value.value = formulaValue
                        formulaLayout.findViewById<TextView>(R.id.valueType).text = form.value.valueType

                        lastHolder.mainL.addView(formulaLayout)
                    }
                }

            } else {
                val normalHolder = holder as NormalViewHolder
                val item = items["${position + 1}"]!!

                normalHolder.testQuestion.text = item.qData
                val minValue = item.sliders.minValue
                val maxValue = item.sliders.maxValue
                normalHolder.minValue.text = minValue.toString()
                normalHolder.minType.text = item.sliders.valueType
                normalHolder.maxValue.text = maxValue.toString()
                normalHolder.maxType.text = item.sliders.valueType
                normalHolder.currentType.text = item.sliders.valueType

                if (item.specify != null) {
                    normalHolder.specifier.visibility = View.VISIBLE
                    normalHolder.specifier.setOnClickListener {
                        showSpecifyPopup(item, Pair(minValue, maxValue), holder)
                    }
                }

                normalHolder.seekBar.min = minValue
                normalHolder.seekBar.max = maxValue
                normalHolder.seekBar.progress = item.dataValue ?: 0//(maxValue / 2)
                normalHolder.currentValue.setText(normalHolder.seekBar.progress.toString())

                normalHolder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        normalHolder.currentValue.setText(p1.toString())
                        item.dataValue = p1
                        item.useSpecify = false
                        (holder).warningUseSpecify.visibility = View.GONE
                        (holder).warningUseGeneral.visibility = View.GONE

                        holder.specifyCurrent.visibility = View.GONE
                        holder.specifyCurrentValue.visibility = View.GONE
                        holder.currentValue.paintFlags = 0
                        holder.currentType.paintFlags = 0
                        item.specify?.values?.forEach{
                            it.value = p1 / item.specify?.size!!
                        }
                    }
                })

                normalHolder.currentValue.addTextChangedListener(object : TextWatcher{
                    override fun afterTextChanged(p0: Editable?) {
                        normalHolder.seekBar.progress = p0.toString().toInt()
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                })
            }
        }


        override fun getItemCount(): Int {
            return items.size + 1
        }

    }
}