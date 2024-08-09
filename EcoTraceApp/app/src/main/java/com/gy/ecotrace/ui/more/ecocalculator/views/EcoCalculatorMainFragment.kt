package com.gy.ecotrace.ui.more.ecocalculator.views

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.ecocalculator.DailyEcoCalcActivity

class EcoCalculatorMainFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activitylayout_ecocalculator_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dailyCalcButtons = arrayOf(R.id.foodEcoCalc, R.id.waterEcoCalc, R.id.trashEcoCalc,
            R.id.energyEcoCalc, R.id.transportationEcoCalc)
        val nonDailyCalcButtons = arrayOf(R.id.housingEcoCalc, R.id.lifestyleEcoCalc, R.id.moodEcoCalc)

        for (daily in dailyCalcButtons.indices) {
            view.findViewById<Button>(dailyCalcButtons[daily]).setOnClickListener {
                val calc = Intent(requireActivity(), DailyEcoCalcActivity::class.java)
                calc.putExtra("calcType", daily)
                startActivity(calc)
            }
        }
    }
}