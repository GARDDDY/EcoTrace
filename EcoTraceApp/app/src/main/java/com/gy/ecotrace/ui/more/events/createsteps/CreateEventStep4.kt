package com.gy.ecotrace.ui.more.events.createsteps

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CircleMapObject
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolygonMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.Line
import com.yandex.runtime.image.ImageProvider
import kotlin.random.Random

class CreateEventStep4: Fragment() {

    private val sharedViewModel: CreateEventViewModel by activityViewModels()

    private lateinit var mapView: MapView
    private lateinit var marksCollection: MapObjectCollection

    private var mapEditType = 0 //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activitylayout_create_event_step4, container, false)
        mapView = view.findViewById(R.id.createMapPoints)
        return view
    }
    private lateinit var mainLayout: LinearLayout


    private var movingObject: MapObject? = null
//
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


    private lateinit var fillColor: TextView
    private lateinit var strokeColor: TextView
    private fun applyMenu(layout: View, window: PopupWindow, mapObject: MapObject) {

        val objClass = sharedViewModel.getCoord(mapObject)
        val objType = if (mapObject is PlacemarkMapObject) 0 else 1

        val currentFill = objClass.fillColor
        val currentStroke = objClass.strokeColor

        if (objType == 0) {
            layout.findViewById<LinearLayout>(R.id.layoutradius).visibility = View.GONE
            layout.findViewById<LinearLayout>(R.id.layoutcolor).visibility = View.GONE
        }

        val radius = layout.findViewById<SeekBar>(R.id.setObjectRadius)
        val radiusText = layout.findViewById<TextView>(R.id.circleRadiusValue)
        radius.progress = (objClass.circleRadius ?: 0f).toInt()
        radiusText.text = radius.progress.toString()
        radius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                radiusText.text = p1.toString()
                (mapObject as CircleMapObject).geometry = Circle(
                    objClass.objectCenter,
                    p1.toFloat()
                )
                objClass.circleRadius = p1.toFloat()
            }
        })

        val allTimes = sharedViewModel.eventTimes.value ?: hashMapOf()
        val timeSpinner = layout.findViewById<Spinner>(R.id.connectWithTime)
        if (allTimes.isNotEmpty()) {
            val items = mutableListOf("Ничего не выбрано")
            items.addAll(allTimes.values)

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timeSpinner.adapter = adapter

            val pos = items.indexOf(allTimes.get(objClass.objectRelation) ?: items[0])
            Log.d("pos", pos.toString())
            timeSpinner.setSelection(pos)
            if (pos == 0) {
                objClass.objectRelation = null
            }

            timeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    if (selectedItem == "Ничего не выбрано") {
                        objClass.objectRelation = null
                    } else {
                        objClass.objectRelation = allTimes.entries.find { it.value == selectedItem }?.key
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else timeSpinner.visibility = View.GONE


        fillColor = layout.findViewById(R.id.chooseFillColor)
        fillColor.setBackgroundColor(Color.parseColor(currentFill))
        fillColor.setOnClickListener {
            val newColor = String.format("#%08X", Random.nextLong(0xFFFFFFFF + 1))
            fillColor.setBackgroundColor(Color.parseColor(newColor))
            (mapObject as CircleMapObject).fillColor = Color.parseColor(newColor)
            objClass.fillColor = newColor
        }
        strokeColor = layout.findViewById(R.id.chooseStrokeColor)
        strokeColor.setBackgroundColor(Color.parseColor(currentStroke))
        strokeColor.setOnClickListener {
            val newColor = String.format("#%08X", Random.nextLong(0xFFFFFFFF + 1))
            strokeColor.setBackgroundColor(Color.parseColor(newColor))
            (mapObject as CircleMapObject).strokeColor = Color.parseColor(newColor)
            objClass.strokeColor = newColor
        }

        layout.findViewById<ImageButton>(R.id.closeMenu).setOnClickListener {
            window.dismiss()
        }
        layout.findViewById<Button>(R.id.deleteObject).setOnClickListener {
            marksCollection.remove(mapObject)
            sharedViewModel.removeCoord(mapObject)
            window.dismiss()
        }
        val move = layout.findViewById<Button>(R.id.setMoveObject)
        if (movingObject == mapObject) move.text = "Перестать перемещать"
        move.setOnClickListener {
            movingObject = if (movingObject != mapObject) {
                move.text = "Перестать перемещать"
                mapObject
            } else {
                move.text = "Переместить"
                null
            }

        }

        val objName: EditText = layout.findViewById(R.id.mapObjectName)
        objName.setText(objClass.objectName)
        objName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                objClass.objectName = s.toString()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }
//
//
    private val circleTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is CircleMapObject) {
            val manageMenu = layoutInflater.inflate(R.layout.layout_manage_mapobject_menu, null)
            val window = PopupWindow(manageMenu, mainLayout.width, mainLayout.height, true)
            applyMenu(manageMenu, window, mapObject)
            mainLayout.post {
                window.showAtLocation(mainLayout, Gravity.TOP, 0, 150)
            }
        }
        true
    }

    private val dotTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is PlacemarkMapObject) {
            val manageMenu = layoutInflater.inflate(R.layout.layout_manage_mapobject_menu, null)
            val window = PopupWindow(manageMenu, mainLayout.width, mainLayout.height, true)
            applyMenu(manageMenu, window, mapObject)
            mainLayout.post {
                window.showAtLocation(mainLayout, Gravity.TOP, 0, 150)
            }
        }
        true
    }
//
//
    private fun addObj(objType: Int, point: Point, objClass: DatabaseMethods.DataClasses.MapObject) {
        when (objType) {
            0 -> {
                val placeMark = marksCollection.addPlacemark(point)
                placeMark.setIcon(ImageProvider.fromResource(context,
                     com.yandex.maps.mobile.R.drawable.search_layer_pin_selected_default))
                placeMark.setIconStyle(IconStyle().apply {
                    anchor = PointF(0.5f, 1f)
                })
                placeMark.addTapListener(dotTapListener)
                objClass.objectType = 2
                objClass.objectCenter = point

                sharedViewModel.addCoord(placeMark, objClass)

            }
            1 -> {
                val addedCircle =
                    marksCollection.addCircle(Circle(point, objClass.circleRadius ?: 100f), Color.BLUE, 2f, 0) // stroke, strokeWidth, fill
                addedCircle.addTapListener(circleTapListener)
                objClass.objectType = 0
                objClass.objectCenter = point
                objClass.circleRadius = objClass.circleRadius ?: 100f

                sharedViewModel.addCoord(addedCircle, objClass)
            }
        }
    }

    private val mapListener = object : InputListener {
        override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {

        }
        override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
            val objClass = DatabaseMethods.DataClasses.MapObject()
            if (movingObject != null) {
                if (movingObject is PlacemarkMapObject) {
                    (movingObject as PlacemarkMapObject).geometry = point
                    sharedViewModel.getCoord(movingObject as PlacemarkMapObject).objectCenter = point
                    movingObject = null
                }
                if (movingObject is CircleMapObject) {
                    (movingObject as CircleMapObject).geometry = Circle(
                        point,
                        (movingObject as CircleMapObject).geometry.radius
                    )
                    sharedViewModel.getCoord(movingObject as CircleMapObject).objectCenter = point
                    movingObject = null
                }
                return
            }

            addObj(mapEditType, point, objClass)
        }
    }

    private fun moveSelector(layout: RelativeLayout, selector: View) {
        val target = layout.getChildAt(mapEditType)

        val anim = ObjectAnimator.ofFloat(selector, "translationY", selector.translationY, target.top - selector.top.toFloat())
        anim.duration = 500
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        marksCollection = mapView.map.mapObjects.addCollection()
        mapView.map.addInputListener(mapListener)

        sharedViewModel.getCoords()
        sharedViewModel.toAddeventCoords.observe(viewLifecycleOwner, Observer {
            for (o in it) {
                addObj(o.objectType, o.objectCenter, o)
            }
        })

        mainLayout = view.findViewById(R.id.main)

        val selector: View = view.findViewById(R.id.selector)
        val ediTypeLayout: RelativeLayout = view.findViewById(R.id.editTypes)

        view.findViewById<ImageButton>(R.id.addPlacemark).setOnClickListener {
            mapEditType = 0
            moveSelector(ediTypeLayout, selector)
        }
        view.findViewById<ImageButton>(R.id.addCircle).setOnClickListener {
            mapEditType = 1
            moveSelector(ediTypeLayout, selector)
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
}