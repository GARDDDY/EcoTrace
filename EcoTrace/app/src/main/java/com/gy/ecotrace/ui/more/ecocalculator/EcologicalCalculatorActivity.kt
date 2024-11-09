package com.gy.ecotrace.ui.more.ecocalculator

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.facebook.shimmer.ShimmerFrameLayout
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EcologicalCalculatorActivity : AppCompatActivity() {

    private val viewModel: CalcDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ecological_calculator)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val progressCard = findViewById<View>(R.id.cardProgress)
        progressCard.findViewById<ImageView>(R.id.calcIcon).setImageResource(R.drawable.baseline_insights_24)
        progressCard.findViewById<TextView>(R.id.calcName).text = getString(R.string.myProgress)
        progressCard.findViewById<TextView>(R.id.calcStatus).text = getString(R.string.progressAndAdvices)
        progressCard.setOnClickListener {
            startActivity(Intent(this, EcoCalcResultsActivity::class.java))
        }

        val loading: ShimmerFrameLayout = findViewById(R.id.dashboardLoading)
        val cardsContainer: LinearLayout = findViewById(R.id.calcCardsContainer)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loading.collect { isLoading ->
                        loading.visibility = if (isLoading) View.VISIBLE else View.GONE
                        cardsContainer.visibility = if (isLoading) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.cards.collect { cards ->
                        cardsContainer.removeAllViews()
                        cards.forEach { card -> addCard(card, cardsContainer) }
                    }
                }
            }
        }
        viewModel.load()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun addCard(card: CalcCardUi, container: LinearLayout) {
        val row = LayoutInflater.from(this).inflate(R.layout.layout_calc_dashboard_card, container, false)
        row.findViewById<ImageView>(R.id.calcIcon).setImageResource(CALC_ICONS[card.calculatorId])
        row.findViewById<TextView>(R.id.calcName).text = getString(CALC_NAMES[card.calculatorId])

        val status = row.findViewById<TextView>(R.id.calcStatus)
        val check = row.findViewById<ImageView>(R.id.calcCheck)
        val state = card.state
        when {
            state == null -> status.text = getString(R.string.calcLoadFailed)
            state.first -> status.text = getString(R.string.calcNotDoneYet)
            state.updating -> {
                status.text = getString(R.string.calcDoneToday)
                check.visibility = View.VISIBLE
            }
            state.daysSince != null -> status.text =
                getString(R.string.calcNotDoneSince, daysAgoText(this, state.daysSince))
            else -> status.text = getString(R.string.calcNotDoneYet)
        }

        row.setOnClickListener {
            val calc = Intent(this, DailyEcoCalcActivity::class.java)
            calc.putExtra("calcType", card.calculatorId)
            startActivity(calc)
        }
        container.addView(row)

        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.dirt2_white))
        container.addView(divider)
    }
}
