package com.gy.ecotrace.ui.more.ecocalculator.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModel
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.ecocalculator.EcoResultsViewModel

class EcoCalculatorResultsFragment : Fragment() {
    private lateinit var viewModel: EcoResultsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_eco_calculator_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calcNameToItsType = arrayOf("Пища", "Вода", "Отходы", "Энергия", "Транспорт")

        viewModel = EcoResultsViewModel(Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods()))

        val mainLayout: LinearLayout = view.findViewById(R.id.mainLayout)

        for (i in 0..4) {
            viewModel.getNumCalculatorImages(i) { count ->
                val layout = layoutInflater.inflate(R.layout.layout_eco_calc_res, null)
                layout.findViewById<TextView>(R.id.calcName).text = calcNameToItsType[i]
                mainLayout.addView(layout)
                for (j in 0 until count) {
                    viewModel.getCalcImage(i, j) { bitmap ->
                        val img = ImageView(context)
                        img.visibility = View.VISIBLE
                        img.adjustViewBounds = true
                        img.setImageBitmap(bitmap)
                        layout.findViewById<LinearLayout>(R.id.foodImages).addView(img)
                    }
                }
            }
        }

    }
}