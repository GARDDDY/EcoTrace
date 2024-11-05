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

        val calcNameToItsType = arrayOf(
            getString(R.string.food),
            getString(R.string.water),
            getString(R.string.trash),
            getString(R.string.energy),
            getString(R.string.transport))

        viewModel = EcoResultsViewModel(Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods()))

        val mainLayout: LinearLayout = view.findViewById(R.id.mainLayout)

        viewModel.getNumCalculatorImages {
            it?.let {
                if (it.length != 0) {
                    view.findViewById<TextView>(R.id.noResultsWarning).visibility = View.GONE
                }

                it.split(",").forEach {
                    val calcType = it.split("_")[0].toInt()
                    val calcTypeType = it.split("_")[1].toInt()

                    val layout = layoutInflater.inflate(R.layout.layout_eco_calc_res, null)
                    layout.findViewById<TextView>(R.id.calcName).text = calcNameToItsType[calcType]
                    mainLayout.addView(layout)

                    viewModel.getCalcImage(calcType, calcTypeType-1) { bitmap ->
                        val img = ImageView(context)
                        img.visibility = View.VISIBLE
                        img.adjustViewBounds = true
                        img.setImageBitmap(bitmap)
                        viewModel.getCalcAdvices(calcType, calcTypeType) { advs ->
                            val foodAdvices = layout.findViewById<LinearLayout>(R.id.foodAdvices)

                            advs.forEach {
                                val adv = layoutInflater.inflate(R.layout.layout_eco_advice, null) as TextView
                                adv.text = it
                                foodAdvices.addView(adv)
                            }

                        }
                        layout.findViewById<LinearLayout>(R.id.foodImages).addView(img)
                    }
                }
            }
        }

    }
}