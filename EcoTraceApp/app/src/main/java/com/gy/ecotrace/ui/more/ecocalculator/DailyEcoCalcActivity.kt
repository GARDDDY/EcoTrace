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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
        val calcNameToItsType = arrayOf("Пища", "Вода", "Отходы", "Энергия", "Транспорт")
        if (calcType == -1) {
            Toast.makeText(applicationContext, "Данные не были переданы!", Toast.LENGTH_SHORT).show()
            finish()
        }

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
                val view = LayoutInflater.from(parent.context).inflate(R.layout.activitylayout_ecological_calculator_layout, parent, false)
                return EcoCalcViewHolder(view, applicationContext, findViewById(R.id.main))
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
            Log.d("new page", "${viewModel.currentPage}")
            viewPager.currentItem = viewModel.currentPage
            back.visibility = View.VISIBLE
        }
        back.setOnClickListener {
            viewModel.currentPage -= 1
            if (viewModel.currentPage == 0) {
                back.visibility = View.GONE
            }
            Log.d("new page", "${viewModel.currentPage}")
            viewPager.currentItem = viewModel.currentPage
            forward.visibility = View.VISIBLE
        }


        val saveAndExit: Button = findViewById(R.id.saveAndExit)
        saveAndExit.setOnClickListener {
            (viewModel.answers.value ?: mutableListOf()).let { data ->

                Log.d("data", data.size.toString())
                Log.d("data1", data.toString())
                if (data.size != dataQuestions.size) {
                    Toast.makeText(this, "Вы ответили не на все вопросы!", Toast.LENGTH_SHORT).show()

                    return@let
                }

                for (form in dataFormulas.toSortedMap()) {
                    val formulaForQuestion = Regex("\\d+").find(form.key)?.value?.toInt() ?: - 1

                    val value = workWithData(dataQuestions, form.value.formula, form.value.useSpecify)
                    if (value != null && Regex("[A-Za-z]+").find(value) == null) {

                        Log.d("formula", value)
                        val formulaValue = calculate(value)
                        Log.w("formula", formulaValue.toString())

                        if (formulaForQuestion != -1) {
                            data[formulaForQuestion-1].formulaValue = formulaValue.toDouble()
                        }



                    }
                }


                viewModel.saveData(calcType) {
                    if (it) {
                        finish()
                    } else {
                        Toast.makeText(this, "Не удалось сохранить данные! Повторите еще раз", Toast.LENGTH_LONG).show()
                    }

                    viewModel.answers.removeObservers(this)
                }
            }
        }

    }

    class EcoCalcViewHolder(private val view: View, private val context: Context, private val mainLayout: ConstraintLayout): RecyclerView.ViewHolder(view) {
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

        fun bind(pos: Int, questions: HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>, viewModel: EcoCalcViewModel) {
            val item = questions["${pos+1}"]!!
            testQuestion.text = item.qData ?: "error"

            val minValueValue = item.sliders.minValue
            val maxValueValue = item.sliders.maxValue
            minValue.text = minValueValue.toString()
            minType.text = item.sliders.valueType
            maxValue.text = maxValueValue.toString()
            maxType.text = item.sliders.valueType
            currentType.text = item.sliders.valueType

            if (item.specify != null) {
                specifier.visibility = View.VISIBLE
                specifier.setOnClickListener {
                    showSpecifyPopup(item, Pair(minValueValue, maxValueValue), view)
                }
            }

            seekBar.min = minValueValue
            seekBar.max = maxValueValue
            seekBar.progress = item.dataValue ?: 0//(maxValue / 2)
            currentValue.setText(seekBar.progress.toString())



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
                    item.specify?.values?.forEach{
                        it.value = p1 / item.specify?.size!!

                    }

                    val saveClass = DatabaseMethods.DataClasses.EcoCalcSaveData(
                        question = pos,
                        value = item.dataValue!!,
                        specify = item.specify?.mapValues { it.value.value!! }
                    )
                    viewModel.addAnswer(saveClass)
                }
            })

            currentValue.addTextChangedListener(object : TextWatcher{
                override fun afterTextChanged(p0: Editable?) {
                    seekBar.progress = p0.toString().toInt()
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })
        }

        private fun showSpecifyPopup(specify: DatabaseMethods.DataClasses.EcoCalcQuestion, minMax: Pair<Int, Int>, holder: View) {
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
                specLayout.findViewById<TextView>(R.id.valueTypeToMin).text = specify.sliders.valueType
                val minValue = specLayout.findViewById<TextView>(R.id.minValue)
                minValue.text = minMax.first.toString()
                specLayout.findViewById<TextView>(R.id.valueTypeToMax).text = specify.sliders.valueType
                val maxValue = specLayout.findViewById<TextView>(R.id.maxValue)
                maxValue.text = minMax.second.toString()
                specLayout.findViewById<TextView>(R.id.valueTypeToCurrent).text = specify.sliders.valueType
                val seekBar: SeekBar = specLayout.findViewById(R.id.seekBar)
                val currValue = specLayout.findViewById<EditText>(R.id.currentValue)
                seekBar.min = minMax.first
                seekBar.max = minMax.second
                seekBar.progress = spec.value.value ?: 0
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
                        spec.value.value = p1
                        specify.useSpecify = true
                        warningUseSpecify.visibility = View.VISIBLE
                        warningUseGeneral.visibility = View.VISIBLE

                        specifyCurrent.visibility = View.VISIBLE
                        specifyCurrent.text = specify.specify!!.values.sumOf { it.value ?: 0 }.toString()
                        specifyCurrentValue.visibility = View.VISIBLE
                        specifyCurrentValue.text = specify.sliders.valueType
                        currentValue.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        currentType.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        currValue.setText(p1.toString())
                    }
                })

                currValue.addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                        val value = if (p0.toString().isNotEmpty()) p0.toString().toInt() else 0
                        spec.value.value = value

                        specify.useSpecify = true
                        warningUseSpecify.visibility = View.VISIBLE
                        warningUseGeneral.visibility = View.VISIBLE

                        specifyCurrent.visibility = View.VISIBLE
                        specifyCurrent.text = specify.specify!!.values.sumOf { it.value ?: 0 }.toString()
                        specifyCurrentValue.visibility = View.VISIBLE
                        specifyCurrentValue.text = specify.sliders.valueType
                        currentValue.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        currentType.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        seekBar.progress = value
                        currValue.setSelection(currValue.text.length)
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                })

                popupView.addView(specLayout)
            }


            mainFrame.addView(popupView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))
            val popupWindow = PopupWindow(mainFrame, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, true)
            popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0)
        }
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
                throw IllegalStateException("Invalid rpn")
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
                            throw IllegalStateException("Bad parentheses")
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
                    throw IllegalStateException("Bad parentheses")
                }
                output.add(op)
            }
            return output
        }
        return calcPolish(toPolish(expression))
    }
}