package com.gy.ecotrace.ui.more.events.createsteps

import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.ui.more.events.CreateEventActivity
import com.gy.ecotrace.ui.more.events.CreateEventActivity.Companion
import com.gy.ecotrace.ui.more.events.CreateEventActivity.Companion.eventmoreClass
import com.gy.ecotrace.ui.more.events.CreateEventActivity.Companion.viewPager
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.map.CircleMapObject
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectDragListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolygonMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider
import kotlin.random.Random

class CreateEventStep3: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activitylayout_create_event_step3, container, false)
        mapView = view.findViewById(R.id.createMapPoints)
        return view
    }
    private lateinit var mainLayout: LinearLayout
    private lateinit var mapView: MapView
    private lateinit var marksCollection: MapObjectCollection
    private var editMapState = true

    private var movingObject: Any? = null

    private var addRadius = false
    private var addDot = true

    private var addRect = false


    private val mapObjects = mutableMapOf<Any, DatabaseMethods.DataClasses.MapObject>()

    private fun disableButtons(layout: View, objType: Int) {
        // 0 - Circle  1 - Area  2 - Dot
        when (objType){
            0 -> {
            }
            1 -> {

            }
            2 -> {
                layout.findViewById<LinearLayout>(R.id.layoutradius).visibility = View.GONE
            }
        }

    }

    private fun addGoalsAndTimes(layout: View, obj: MapObject) {

        val dropdownGoals: Spinner = layout.findViewById(R.id.connectWithGoal)
        if (eventmoreClass.eventGoals != null) {
            val values = ArrayList<String>()
            values.add("Ничего не выбрано")
            for (i in eventmoreClass.eventGoals!!.values) {
                values.add("${values.size}. $i")
            }

            val adapter = ArrayAdapter(requireContext(), R.layout.widget_custom_spinner_item, values)
            adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
            dropdownGoals.adapter = adapter
        } else {
            dropdownGoals.visibility = View.GONE
        }

        val dropdownTimes: Spinner = layout.findViewById(R.id.connectWithTime)
        if (eventmoreClass.eventTimes != null) {
            val values = ArrayList<String>()
            values.add("Ничего не выбрано")
            for (i in eventmoreClass.eventTimes!!.values) {
                values.add("${values.size}. $i")
            }

            val adapter = ArrayAdapter(requireContext(), R.layout.widget_custom_spinner_item, values)
            adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
            dropdownTimes.adapter = adapter
        } else {
            dropdownTimes.visibility = View.GONE
        }
        dropdownGoals.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                mapObjects[obj]!!.objectRelation.isWithGoal = true
                mapObjects[obj]!!.objectRelation.relationValue = p2-1
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        dropdownTimes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                mapObjects[obj]!!.objectRelation.isWithGoal = false
                mapObjects[obj]!!.objectRelation.relationValue = p2-1
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

    }


    private lateinit var fillColor: TextView
    private lateinit var strokeColor: TextView
    private fun defaultFunctions(layout: View, obj: MapObject, window: PopupWindow) {
        val objClass = mapObjects[obj]!!
        val currentFill = objClass.fillColor
        val currentStroke = objClass.strokeColor
        fillColor = layout.findViewById(R.id.chooseFillColor)
        fillColor.setBackgroundColor(Color.parseColor(currentFill))
        fillColor.setOnClickListener {
            val newColor = String.format("#%08X", Random.nextLong(0xFFFFFFFF + 1))
            fillColor.setBackgroundColor(Color.parseColor(newColor))
            if (obj is CircleMapObject) {
                obj.fillColor = Color.parseColor(newColor)
            }
            if (obj is PolygonMapObject) {
                obj.fillColor = Color.parseColor(newColor)
            }
            objClass.fillColor = newColor
        }
        strokeColor = layout.findViewById(R.id.chooseStrokeColor)
        strokeColor.setBackgroundColor(Color.parseColor(currentStroke))
        strokeColor.setOnClickListener {
            val newColor = String.format("#%08X", Random.nextLong(0xFFFFFFFF + 1))
            strokeColor.setBackgroundColor(Color.parseColor(newColor))
            if (obj is CircleMapObject) {
                obj.strokeColor = Color.parseColor(newColor)
            }
            if (obj is PolygonMapObject) {
                obj.strokeColor = Color.parseColor(newColor)
            }
            objClass.strokeColor = newColor
        }

        layout.findViewById<ImageButton>(R.id.closeMenu).setOnClickListener {
            window.dismiss()
        }
        layout.findViewById<Button>(R.id.deleteObject).setOnClickListener {
            marksCollection.remove(obj)
            window.dismiss()
        }
        layout.findViewById<Button>(R.id.setMoveObject).setOnClickListener {
            movingObject = if (movingObject != obj) obj else null
        }

        val objName: EditText = layout.findViewById(R.id.mapObjectName)
        objName.setText(objClass.objectName)
        objName.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                objClass.objectName = s.toString()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        addGoalsAndTimes(layout, obj)
    }


    private val circleTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is CircleMapObject) {
            val manageMenu = layoutInflater.inflate(R.layout.layout_manage_mapobject_menu, null)
            disableButtons(manageMenu, 0)

            val objClass = mapObjects[mapObject]!!
            val radiusBar: SeekBar = manageMenu.findViewById(R.id.setObjectRadius)
            val radiusValue = manageMenu.findViewById<TextView>(R.id.circleRadiusValue)
            radiusBar.progress = objClass.circleRadius!!.toInt()
            radiusBar.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    val newCircle = Circle(
                        objClass.objectCenter,
                        p1.toFloat()
                    )
                    mapObject.geometry = newCircle
                    radiusValue.text = "$p1"
                    objClass.circleRadius = p1.toFloat()
                }
            })
            radiusValue.text = "${objClass.circleRadius!!.toInt()}"

            val window = PopupWindow(manageMenu, mainLayout.width, mainLayout.height, true)
            mainLayout.post {
                window.showAtLocation(mainLayout, Gravity.TOP, 0, 150)
            }
            defaultFunctions(manageMenu, mapObject, window)
        }
        true
    }

    private val polygonTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is PolygonMapObject) {
            val manageMenu = layoutInflater.inflate(R.layout.layout_manage_mapobject_menu, null)
            disableButtons(manageMenu, 1)
            val objClass = mapObjects[mapObject]!!

            val window = PopupWindow(manageMenu, mainLayout.width, mainLayout.height, true)
            mainLayout.post {
                window.showAtLocation(mainLayout, Gravity.TOP, 0, 150)
            }
            defaultFunctions(manageMenu, mapObject, window)
        }
        true
    }
    private val dotTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is PlacemarkMapObject) {
            val manageMenu = layoutInflater.inflate(R.layout.layout_manage_mapobject_menu, null)
            disableButtons(manageMenu, 2)
            val objClass = mapObjects[mapObject]!!

            val window = PopupWindow(manageMenu, mainLayout.width, mainLayout.height, true)
            mainLayout.post {
                window.showAtLocation(mainLayout, Gravity.TOP, 0, 150)
            }
            defaultFunctions(manageMenu, mapObject, window)
        }
        true
    }


    private val mapListener = object : InputListener {
        override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
            if (!editMapState) {
                Log.d("testtouch", "${point.latitude} ${point.longitude}")
//
            }
        }

        override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
            if (!editMapState) {
                val objClass = DatabaseMethods.DataClasses.MapObject()
                if (addDot) {
                    val placeMark = marksCollection.addPlacemark(point)
                    placeMark.setIcon(ImageProvider.fromResource(context,
                        com.yandex.maps.mobile.R.drawable.search_layer_pin_selected_default))
                    placeMark.setIconStyle(IconStyle().apply {
                        anchor = PointF(0.5f, 1f)
                    })
                    placeMark.addTapListener(dotTapListener)
                    objClass.objectType = 2
                    objClass.objectCenter = point

                    mapObjects[placeMark] = objClass
                }
                else
                    if (addRect) {

                    }
                    else
                        if (addRadius) {
                            val addedCircle =
                                marksCollection.addCircle(Circle(point, 100f), Color.BLUE, 2f, 0) // stroke, strokeWidth, fill
                            addedCircle.addTapListener(circleTapListener)
                            objClass.objectType = 0
                            objClass.objectCenter = point
                            objClass.circleRadius = 100f

                            mapObjects[addedCircle] = objClass
                        }
            }

        }
    }

    private fun mapRestrictions(state: Boolean) {
        mapView.map.isScrollGesturesEnabled = state
        mapView.map.isZoomGesturesEnabled = state
        mapView.map.isRotateGesturesEnabled = state
        mapView.map.isTiltGesturesEnabled = state
        mapView.map.isRotateGesturesEnabled = state
        mapView.map.isScrollGesturesEnabled = state
        mapView.map.isZoomGesturesEnabled = state
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainLayout = view.findViewById(R.id.main)
        marksCollection = mapView.map.mapObjects.addCollection()
        val editMap: Button = view.findViewById(R.id.editMap)
        mapRestrictions(false)
        mapView.map.addInputListener(mapListener)
        editMap.setOnClickListener {
            editMapState = !editMapState
            viewPager.isUserInputEnabled = editMapState
            mapRestrictions(!editMapState)
            editMap.text = when (editMapState) {
                false -> "Завершить редактирование"
                else -> "Редактировать карту"
            }
        }

        view.findViewById<Button>(R.id.addCircle).setOnClickListener {
            addRadius = true
            addRect = false
            addDot = false
        }
        view.findViewById<Button>(R.id.addRect).setOnClickListener {
            TODO("зона по точка Type 1|2")
            addRadius = false
            addRect = true
            addDot = false
            Toast.makeText(
                context,
                "Сначала будет установлена верхняя левая точка, затем - нижняя правая!",
                Toast.LENGTH_LONG).show()
        }
        view.findViewById<Button>(R.id.addPlacemark).setOnClickListener {
            addRadius = false
            addRect = false
            addDot = true
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.map.removeInputListener(mapListener)
    }

    fun mapObj(): MutableMap<Any, DatabaseMethods.DataClasses.MapObject> {
        return mapObjects
    }
}