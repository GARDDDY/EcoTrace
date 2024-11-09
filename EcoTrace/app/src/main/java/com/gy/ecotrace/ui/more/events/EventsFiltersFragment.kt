package com.gy.ecotrace.ui.more.events

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.EventBounds
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale

class EventsFiltersFragment : Fragment() {

    companion object {
        const val TAG = "EventsFiltersFragment"
        const val RESULT_KEY = "filters_result"
        const val ARG_FILTER = "filter"
        const val ARG_FROM = "from"
        const val ARG_TO = "to"
        const val ARG_MIN_MEMBERS = "min_members"
        const val ARG_MAX_MEMBERS = "max_members"
        const val ARG_HAS_MAP = "has_map"
        const val ARG_LAT = "lat"
        const val ARG_LNG = "lng"
        const val ARG_RADIUS_KM = "radius_km"

        fun newInstance(filter: String?, from: String?, to: String?): EventsFiltersFragment =
            EventsFiltersFragment().apply {
                arguments = bundleOf(ARG_FILTER to filter, ARG_FROM to from, ARG_TO to to)
            }
    }

    private val viewModel: ShowAllEventsViewModel by activityViewModels()

    private val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    private var selectedFilter: String? = null
    private var selectedFrom: String? = null
    private var selectedTo: String? = null
    private var loadedBounds: EventBounds? = null

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (!granted) Toast.makeText(requireContext(), "Нет доступа к геопозиции — фильтр по близости не будет применён", Toast.LENGTH_LONG).show() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedFilter = arguments?.getString(ARG_FILTER)
        selectedFrom = arguments?.getString(ARG_FROM)
        selectedTo = arguments?.getString(ARG_TO)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_event__search_events_apply_filters, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar7)
        val fromEdit = view.findViewById<EditText>(R.id.editTextDate)
        val toEdit = view.findViewById<EditText>(R.id.editTextDate2)
        val chipGroup = view.findViewById<ChipGroup>(R.id.tagsChipGroup)
        val lowMembersEdit = view.findViewById<EditText>(R.id.lowMembersCount)
        val highMembersEdit = view.findViewById<EditText>(R.id.highMemebersCount)
        val hasMapChip = view.findViewById<Chip>(R.id.hasMap)
        val radiusEdit = view.findViewById<EditText>(R.id.radiusKm)
        val applyBtn = view.findViewById<Button>(R.id.applyFiltersButton)

        toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        fromEdit.inputType = InputType.TYPE_NULL
        toEdit.inputType = InputType.TYPE_NULL

        selectedFrom?.let {
            fromEdit.setText(displayFormat.format(Instant.parse(it).toEpochMilli()))
        }
        selectedTo?.let {
            toEdit.setText(displayFormat.format(Instant.parse(it).toEpochMilli()))
        }

        fromEdit.setOnClickListener { pickDate(fromEdit, isFrom = true) { iso -> selectedFrom = iso } }
        toEdit.setOnClickListener   { pickDate(toEdit, isFrom = false)   { iso -> selectedTo   = iso } }

        lowMembersEdit.setText(viewModel.minMembers?.toString() ?: "")
        highMembersEdit.setText(viewModel.maxMembers?.toString() ?: "")
        hasMapChip.isChecked = viewModel.hasMap == true
        radiusEdit.setText(viewModel.radiusKm?.toString() ?: "")

        fun clampMembers(edit: EditText) {
            val bounds = loadedBounds ?: return
            val value = edit.text.toString().trim().toIntOrNull() ?: return
            val clamped = value.coerceIn(bounds.minMembers, bounds.maxMembers)
            if (clamped != value) edit.setText(clamped.toString())
        }
        lowMembersEdit.setOnFocusChangeListener { v, hasFocus -> if (!hasFocus) clampMembers(v as EditText) }
        highMembersEdit.setOnFocusChangeListener { v, hasFocus -> if (!hasFocus) clampMembers(v as EditText) }

        viewModel.loadBounds()
        viewModel.bounds.observe(viewLifecycleOwner) { bounds ->
            loadedBounds = bounds
            fromEdit.hint = "С ${bounds.minStartsAt?.let { displayFormat.format(Instant.parse(it).toEpochMilli()) } ?: "—"}"
            toEdit.hint = "По ${bounds.maxStartsAt?.let { displayFormat.format(Instant.parse(it).toEpochMilli()) } ?: "—"}"
            lowMembersEdit.hint = "От ${bounds.minMembers}"
            highMembersEdit.hint = "До ${bounds.maxMembers}"
            clampMembers(lowMembersEdit)
            clampMembers(highMembersEdit)
        }

        val tags   = Globals.getInstance().getEventsFilters()
        val colors = Globals.getInstance().getFiltersColors()
        for (i in tags.indices) {
            val chip = Chip(requireContext()).apply {
                text = tags[i].first
                isCheckable = true
                chipStrokeWidth = 2f
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor(colors[i].first))
                setTextColor(Color.parseColor(colors[i].second))
                chipBackgroundColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(
                        Color.parseColor(colors[i].first),
                        Color.TRANSPARENT
                    )
                )
                isChecked = (tags[i].first == selectedFilter)
            }
            chip.setOnCheckedChangeListener { _, checked ->
                selectedFilter = if (checked) tags[i].first else null
            }
            chipGroup.addView(chip)
        }

        radiusEdit.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        applyBtn.setOnClickListener {
            val radiusKm = radiusEdit.text.toString().trim().toDoubleOrNull()
            val location = if (radiusKm != null) lastKnownLocation() else null
            if (radiusKm != null && location == null) {
                Toast.makeText(requireContext(), "Не удалось определить местоположение — фильтр по радиусу пропущен", Toast.LENGTH_LONG).show()
            }

            setFragmentResult(RESULT_KEY, bundleOf(
                ARG_FILTER to selectedFilter,
                ARG_FROM   to selectedFrom,
                ARG_TO     to selectedTo,
                ARG_MIN_MEMBERS to lowMembersEdit.text.toString().trim().toIntOrNull(),
                ARG_MAX_MEMBERS to highMembersEdit.text.toString().trim().toIntOrNull(),
                ARG_HAS_MAP to (if (hasMapChip.isChecked) true else null),
                ARG_LAT to location?.latitude,
                ARG_LNG to location?.longitude,
                ARG_RADIUS_KM to location?.let { radiusKm }
            ))
            parentFragmentManager.popBackStack()
        }
    }

    private fun lastKnownLocation(): android.location.Location? {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: SecurityException) { null }
    }

    private fun pickDate(target: EditText, isFrom: Boolean, onPicked: (String) -> Unit) {
        val c = Calendar.getInstance()
        val dialog = DatePickerDialog(requireContext(), { _, y, m, d ->
            val cal = Calendar.getInstance().apply {
                set(y, m, d, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            target.setText(displayFormat.format(cal.timeInMillis))
            onPicked(Instant.ofEpochMilli(cal.timeInMillis).toString())
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

        val bounds = loadedBounds
        val minMillis = bounds?.minStartsAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        val maxMillis = bounds?.maxStartsAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        val otherMillis = (if (isFrom) selectedTo else selectedFrom)
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        val effectiveMax = if (isFrom) listOfNotNull(maxMillis, otherMillis).minOrNull() else maxMillis
        val effectiveMin = if (!isFrom) listOfNotNull(minMillis, otherMillis).maxOrNull() else minMillis
        effectiveMin?.let { dialog.datePicker.minDate = it }
        effectiveMax?.let { dialog.datePicker.maxDate = it }

        dialog.show()
    }
}
