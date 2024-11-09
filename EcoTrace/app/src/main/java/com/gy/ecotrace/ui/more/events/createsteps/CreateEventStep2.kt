package com.gy.ecotrace.ui.more.events.createsteps

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CircleMapObject
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlin.random.Random

class CreateEventStep2 : Fragment() {

    private val sharedViewModel: CreateEventViewModel by activityViewModels()

    private lateinit var mapView: MapView
    private lateinit var marksCollection: MapObjectCollection
    private var mapEditType = 0

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) moveToCurrentLocation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_layout_event__create_step_2, container, false)
        mapView = view.findViewById(R.id.createMapPoints)
        return view
    }

    private lateinit var mainLayout: LinearLayout

    private var movingObject: MapObject? = null

    private fun disableButtons(layout: View, objType: Int) {
        if (objType == 2) layout.findViewById<LinearLayout>(R.id.layoutradius).visibility = View.GONE
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
        radius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                radiusText.text = p1.toString()
                (mapObject as CircleMapObject).geometry = Circle(objClass.objectCenter, p1.toFloat())
                objClass.circleRadius = p1.toFloat()
            }
        })

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

        layout.findViewById<ImageButton>(R.id.closeMenu).setOnClickListener { window.dismiss() }

        layout.findViewById<Button>(R.id.deleteObject).setOnClickListener {
            marksCollection.remove(mapObject)
            sharedViewModel.removeCoord(mapObject)
            sharedViewModel.saveDraft()
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
            window.dismiss()
            Toast.makeText(context, "Выберите новую точку, зажав ее на карте", Toast.LENGTH_LONG).show()
        }

        val objName: EditText = layout.findViewById(R.id.mapObjectName)
        objName.setText(objClass.objectName)
        objName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { objClass.objectName = s.toString() }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private val circleTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is CircleMapObject) {
            val manageMenu = layoutInflater.inflate(R.layout.layout_event__menu_manage_map_object, null)
            val window = PopupWindow(manageMenu, mainLayout.width, mainLayout.height, true)
            applyMenu(manageMenu, window, mapObject)
            mainLayout.post { window.showAtLocation(mainLayout, Gravity.TOP, 0, 150) }
        }
        true
    }

    private val dotTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is PlacemarkMapObject) {
            val manageMenu = layoutInflater.inflate(R.layout.layout_event__menu_manage_map_object, null)
            val window = PopupWindow(manageMenu, mainLayout.width, mainLayout.height, true)
            applyMenu(manageMenu, window, mapObject)
            mainLayout.post { window.showAtLocation(mainLayout, Gravity.TOP, 0, 150) }
        }
        true
    }

    private fun addObj(objType: Int, point: Point, objClass: DatabaseMethods.DataClasses.MapObject) {
        when (objType) {
            0 -> {
                val placeMark = marksCollection.addPlacemark(point)
                placeMark.setIcon(ImageProvider.fromResource(context, com.yandex.maps.mobile.R.drawable.search_layer_pin_selected_default))
                placeMark.setIconStyle(IconStyle().apply { anchor = PointF(0.5f, 1f) })
                placeMark.addTapListener(dotTapListener)
                objClass.objectType = 2
                objClass.objectCenter = point
                sharedViewModel.addCoord(placeMark, objClass)
            }
            1 -> {
                val addedCircle = marksCollection.addCircle(
                    Circle(point, objClass.circleRadius ?: 100f),
                    runCatching { Color.parseColor(objClass.strokeColor) }.getOrDefault(Color.BLUE),
                    2f,
                    runCatching { Color.parseColor(objClass.fillColor) }.getOrDefault(Color.argb(60, 0, 0, 255))
                )
                addedCircle.addTapListener(circleTapListener)
                objClass.objectType = 0
                objClass.objectCenter = point
                objClass.circleRadius = objClass.circleRadius ?: 100f
                sharedViewModel.addCoord(addedCircle, objClass)
            }
        }
        sharedViewModel.saveDraft()
    }

    private val mapListener = object : InputListener {
        override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {}
        override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
            if (movingObject != null) {
                when (val mo = movingObject) {
                    is PlacemarkMapObject -> {
                        mo.geometry = point
                        sharedViewModel.getCoord(mo).objectCenter = point
                    }
                    is CircleMapObject -> {
                        mo.geometry = Circle(point, mo.geometry.radius)
                        sharedViewModel.getCoord(mo).objectCenter = point
                    }
                }
                movingObject = null
                return
            }
            addObj(mapEditType, point, DatabaseMethods.DataClasses.MapObject())
        }
    }

    private fun moveSelector(target: View, selector: View) {
        val anim = ObjectAnimator.ofFloat(
            selector, "translationX",
            selector.translationX, target.left - selector.left.toFloat()
        )
        anim.duration = 300
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.start()
    }

    private fun moveToCurrentLocation() {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: SecurityException) { null }

        location?.let {
            mapView.map.move(
                CameraPosition(Point(it.latitude, it.longitude), 13.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 0.5f), null
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        marksCollection = mapView.map.mapObjects.addCollection()
        mapView.map.addInputListener(mapListener)

        sharedViewModel.getCoords()
        sharedViewModel.toAddeventCoords.observe(viewLifecycleOwner, Observer { list ->
            for (o in list) addObj(o.objectType, o.objectCenter, o)
        })

        sharedViewModel.restoreDraftCoords.observe(viewLifecycleOwner) { coords ->
            if (coords.isNullOrEmpty()) return@observe
            coords.forEach { coord ->
                val obj = DatabaseMethods.DataClasses.MapObject()
                obj.circleRadius = coord.radius
                obj.fillColor    = coord.fill
                obj.strokeColor  = coord.stroke
                obj.objectName   = coord.name ?: ""
                addObj(if (coord.isCircle) 1 else 0, Point(coord.lat, coord.lng), obj)
            }
            sharedViewModel.clearRestoreDraftCoords()
        }

        mainLayout = view.findViewById(R.id.main)

        val selector: View = view.findViewById(R.id.selector)

        view.findViewById<ImageButton>(R.id.addPlacemark).setOnClickListener {
            mapEditType = 0
            moveSelector(it, selector)
        }
        view.findViewById<ImageButton>(R.id.addCircle).setOnClickListener {
            mapEditType = 1
            moveSelector(it, selector)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            moveToCurrentLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
