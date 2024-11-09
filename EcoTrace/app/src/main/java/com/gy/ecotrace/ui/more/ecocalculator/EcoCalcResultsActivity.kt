package com.gy.ecotrace.ui.more.ecocalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.facebook.shimmer.ShimmerFrameLayout
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.profile.renderUserGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EcoCalcResultsActivity : AppCompatActivity() {

    private val viewModel: CalcResultsViewModel by viewModels()
    private val chipIds = listOf(R.id.chipFood, R.id.chipWater, R.id.chipTrash, R.id.chipEnergy, R.id.chipTransport)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_eco_calc_results)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        setupGraph()
        setupSpecifyBlocks()

        viewModel.loadGraph()
        viewModel.loadSpecifyBlocks()
    }

    private fun setupGraph() {
        val chart: LineChart = findViewById(R.id.carbonGraph)
        val loading: ShimmerFrameLayout = findViewById(R.id.graphLoading)
        val chartCard: View = findViewById(R.id.chartCard)
        val noGraphWarning: TextView = findViewById(R.id.noGraphWarning)

        val periodToggle = findViewById<MaterialButtonToggleGroup>(R.id.graphPeriodToggle)
        val typeChips = findViewById<ChipGroup>(R.id.graphTypesChipGroup)

        periodToggle.check(if (viewModel.lastGraphTime == 1) R.id.period30Button else R.id.period7Button)
        viewModel.lastGraphTypes?.forEach { excludedIndex ->
            typeChips.findViewById<Chip>(chipIds[excludedIndex]).isChecked = false
        }
        if (viewModel.lastGraphTypes == null) {
            viewModel.lastGraphTypes = excludedTypes(typeChips)
        }

        periodToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val time = if (checkedId == R.id.period30Button) 1 else 0
            if (viewModel.lastGraphTime == time) return@addOnButtonCheckedListener
            viewModel.loadGraph(time = time, excludeTypes = viewModel.lastGraphTypes)
        }
        typeChips.setOnCheckedStateChangeListener { group, _ ->
            val excluded = excludedTypes(group as ChipGroup)
            viewModel.loadGraph(time = viewModel.lastGraphTime, excludeTypes = excluded)
        }

        viewModel.graphLoading.observe(this) { isLoading ->
            loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.graph.observe(this) { graph ->
            val hasData = graph != null && graph.datasets.isNotEmpty()
            noGraphWarning.visibility = if (hasData) View.GONE else View.VISIBLE
            chartCard.visibility = if (hasData) View.VISIBLE else View.GONE
            if (hasData) renderUserGraph(chart, graph!!)
        }
    }

    private fun setupSpecifyBlocks() {
        val loading: ShimmerFrameLayout = findViewById(R.id.advicesLoading)
        val noAdvicesWarning: TextView = findViewById(R.id.noAdvicesWarning)
        val container: LinearLayout = findViewById(R.id.specifyBlocksContainer)

        viewModel.specifyLoading.observe(this) { isLoading ->
            loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.specifyBlocks.observe(this) { blocks ->
            container.removeAllViews()
            noAdvicesWarning.visibility = if (blocks.isEmpty()) View.VISIBLE else View.GONE
            blocks.forEach { block ->
                val view = LayoutInflater.from(this)
                    .inflate(R.layout.layout_calc_specify_block, container, false)
                view.findViewById<TextView>(R.id.blockTitle).text = block.title
                renderSpecifyChart(view.findViewById(R.id.blockChart), block.chart)

                val advicesLayout = view.findViewById<LinearLayout>(R.id.blockAdvices)
                block.advices.forEach { advice ->
                    val adv = LayoutInflater.from(this)
                        .inflate(R.layout.layout_eco_advice, advicesLayout, false) as TextView
                    adv.text = advice
                    advicesLayout.addView(adv)
                }

                container.addView(view)
            }
        }
    }

    private fun excludedTypes(chipGroup: ChipGroup): MutableList<Int> = chipIds
        .mapIndexedNotNull { index, id -> index.takeIf { !chipGroup.findViewById<Chip>(id).isChecked } }
        .toMutableList()
}
